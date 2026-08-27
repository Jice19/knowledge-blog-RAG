#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量导入文章脚本：登录 → 建/查分类 → 逐篇发布 → 导出标题→id 映射。

用法：
  python3 scripts/import_articles.py

前置：后端已启动（mvn clean spring-boot:run），数据库已初始化（默认管理员 admin/admin123）。
文章来源：content/articles/*.md，格式为 frontmatter + Markdown 正文：
  ---
  title: 标题
  summary: 摘要
  category: 分类名
  ---
  ## 章节一
  ...
"""

import json
import os
import sys
import glob
import urllib.parse
import urllib.request

BACKEND = "http://localhost:8080"
ADMIN_USER = "admin"
ADMIN_PASS = "admin123"
ARTICLES_DIR = "content/articles"
OUT_MAP = "content/imported_articles.json"

# 分类名 → slug（脚本会查重，存在则复用；缺失则按此表或自动生成 slug 创建）
CATEGORY_SLUGS = {
    "Java": "java",
    "Redis": "redis",
    "RabbitMQ": "mq",
    "Spring": "spring",
    "MySQL": "mysql",
    "网络": "network",
    "RAG": "rag",
    "前端": "frontend",
    "后端": "backend",
    "Agent": "agent",
    "面试题": "interview",
    "手写题": "handwritten",
    "个人笔记": "notes",
}


def http(method, url, body=None, token=None, timeout=60):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read().decode("utf-8"))


def parse_frontmatter(path):
    """解析 frontmatter，返回 (title, summary, category, content)"""
    with open(path, encoding="utf-8") as f:
        raw = f.read()
    if not raw.startswith("---\n"):
        raise ValueError(f"{path} 缺少 frontmatter")
    rest = raw[len("---\n"):]
    meta, sep, content = rest.partition("\n---\n")
    if not sep:
        raise ValueError(f"{path} frontmatter 未闭合")
    fields = {}
    for line in meta.splitlines():
        if ":" in line:
            k, v = line.split(":", 1)
            fields[k.strip()] = v.strip()
    return (fields.get("title", ""), fields.get("summary", ""),
            fields.get("category", ""), content.strip())


def main():
    # 1. 登录拿 token
    login = http("POST", f"{BACKEND}/api/auth/login",
                 {"username": ADMIN_USER, "password": ADMIN_PASS})
    token = login["data"]["token"]
    print(f"登录成功：{login['data']['username']}")

    # 2. 解析全部文章，收集用到的分类
    files = sorted(glob.glob(os.path.join(ARTICLES_DIR, "*.md")))
    parsed = []
    needed = set()
    for path in files:
        title, summary, category, content = parse_frontmatter(path)
        parsed.append((title, summary, category, content))
        needed.add(category)

    # 3. 分类：查重 + 动态建缺失
    cats = http("GET", f"{BACKEND}/api/categories")["data"]
    name2id = {c["name"]: c["id"] for c in cats}
    for name in needed:
        if name not in name2id:
            slug = CATEGORY_SLUGS.get(name) or "cat-" + str(abs(hash(name)) % 100000)
            http("POST", f"{BACKEND}/api/admin/categories",
                 {"name": name, "slug": slug}, token)
            name2id[name] = None  # 稍后重新拉取 id
    if any(v is None for v in name2id.values()):
        cats = http("GET", f"{BACKEND}/api/categories")["data"]
        name2id = {c["name"]: c["id"] for c in cats}
    print(f"分类就绪：{list(name2id)}")

    # 4. 逐篇发布
    imported = []
    for title, summary, category, content in parsed:
        cid = name2id.get(category)
        if cid is None:
            print(f"跳过 {title}：未知分类 {category}")
            continue
        body = {"title": title, "summary": summary, "content": content,
                "categoryId": cid, "status": 1, "tagIds": []}
        try:
            http("POST", f"{BACKEND}/api/admin/articles", body, token)
            imported.append(title)
            print(f"已发布：{title}")
        except Exception as e:
            print(f"失败 {title}: {e}")

    # 4. 导出标题→id 映射（供评测集使用）
    page, size, title2id = 1, 50, {}
    while True:
        d = http("GET", f"{BACKEND}/api/articles?page={page}&size={size}")["data"]
        for a in d.get("records") or []:
            title2id[(a.get("title") or "").strip()] = a.get("id")
        if page * size >= d.get("total", 0):
            break
        page += 1
    os.makedirs(os.path.dirname(OUT_MAP), exist_ok=True)
    with open(OUT_MAP, "w", encoding="utf-8") as f:
        json.dump({t: i for t, i in title2id.items() if t in imported},
                  f, ensure_ascii=False, indent=2)
    print(f"\n完成：成功 {len(imported)} 篇，映射已写入 {OUT_MAP}")
    print("提示：文章已异步进入向量化队列，稍等片刻后即可运行评测。")


if __name__ == "__main__":
    sys.exit(main())
