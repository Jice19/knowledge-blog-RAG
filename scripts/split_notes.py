#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把超大 Markdown 笔记按 # / ## 标题拆成多篇带 frontmatter 的文章，供 import_articles.py 导入。

用法：
  python3 scripts/split_notes.py /path/to/你的笔记.md [输出目录]

默认输出到 content/articles/，文件名 notes-序号-标题slug.md。
标题前的引导内容（无标题）会归入「其他笔记」。
"""

import os
import re
import sys
import unicodedata

# 标题关键词 → 分类名（顺序匹配，命中即停）
CATEGORY_RULES = [
    ("agent", "Agent"),
    ("vue", "前端"), ("vite", "前端"), ("webpack", "前端"),
    ("npm", "前端"), ("pnpm", "前端"), ("yarn", "前端"),
    ("js", "前端"), ("es6", "前端"), ("es5", "前端"),
    ("git", "前端"), ("promise", "前端"), ("函数式", "前端"),
    ("模块化", "前端"), ("工程化", "前端"), ("响应式", "前端"),
    ("原型", "前端"), ("防抖", "前端"), ("节流", "前端"),
    ("柯里化", "前端"), ("watch", "前端"), ("computed", "前端"),
    ("计网", "网络"),
    ("前端", "前端"), ("javascript", "前端"), ("typescript", "前端"),
    ("react", "前端"), ("css", "前端"), ("html", "前端"), ("浏览器", "前端"),
    ("面试", "面试题"), ("八股", "面试题"),
    ("手写", "手写题"), ("算法", "手写题"),
    ("redis", "Redis"), ("mysql", "MySQL"), ("sql", "MySQL"),
    ("rabbitmq", "RabbitMQ"), ("mq", "RabbitMQ"),
    ("spring", "Spring"), ("mybatis", "Spring"), ("java", "Java"),
    ("jvm", "Java"), ("线程", "Java"), ("并发", "Java"), ("集合", "Java"),
    ("后端", "后端"), ("分布式", "后端"), ("微服务", "后端"), ("设计模式", "后端"),
    ("http", "网络"), ("tcp", "网络"),
]


def infer_category(title):
    t = title.lower()
    for kw, cat in CATEGORY_RULES:
        if kw in t:
            return cat
    return "个人笔记"


def slugify(title):
    s = unicodedata.normalize("NFKD", title)
    s = "".join(c if c.isascii() and c.isalnum() else "-" for c in s)
    s = re.sub(r"-+", "-", s).strip("-")
    return s[:40] or "note"


def split_by_boundary(lines):
    """按 # / ## 拆成 [(标题, 正文行), ...]，标题前的引导内容归入「其他笔记」"""
    pattern = re.compile(r"^#{1,2}\s+(.+)$")
    sections, cur_title, cur_body = [], None, []
    for line in lines:
        m = pattern.match(line.strip())
        if m:
            if cur_title is not None:
                sections.append((cur_title, cur_body))
            elif "".join(cur_body).strip():
                sections.append(("其他笔记", cur_body))
            cur_title, cur_body = m.group(1).strip(), []
        else:
            cur_body.append(line)
    if cur_title is not None:
        sections.append((cur_title, cur_body))
    elif "".join(cur_body).strip():
        sections.append(("其他笔记", cur_body))
    return sections


def make_summary(body_lines, limit=120):
    in_fence = False
    for line in body_lines:
        t = line.strip()
        if t.startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence or not t or t.startswith("#"):
            continue
        return t[:limit]
    return ""


def main():
    if len(sys.argv) < 2:
        print("用法: python3 scripts/split_notes.py 你的笔记.md [输出目录]")
        return 1
    src, out_dir = sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else "content/articles"

    with open(src, encoding="utf-8") as f:
        lines = f.read().splitlines()

    sections = split_by_boundary(lines)
    if not sections:
        print("未发现标题，整篇作为一篇导入")
        sections = [("个人笔记", lines)]

    os.makedirs(out_dir, exist_ok=True)
    written = 0
    for i, (title, body) in enumerate(sections, 1):
        category = infer_category(title)
        summary = make_summary(body)
        content = "\n".join(body).strip()
        if not content:
            continue
        fname = f"notes-{i:03d}-{slugify(title)}.md"
        with open(os.path.join(out_dir, fname), "w", encoding="utf-8") as f:
            f.write(f"---\ntitle: {title}\nsummary: {summary}\ncategory: {category}\n---\n\n{content}\n")
        written += 1
        print(f"[{category}] {title}  ->  {fname}")

    print(f"\n完成：拆出 {written} 篇文章，输出到 {out_dir}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
