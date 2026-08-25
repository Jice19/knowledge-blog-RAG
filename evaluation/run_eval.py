#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
RAG 轻量离线评测：检索命中率(Hit@K) + 忠实度(Faithfulness) + 相关性(Relevance)

零第三方依赖，仅用 Python 标准库。

用法：
  # 1) 列出文章，辅助构造评测集
  python3 evaluation/run_eval.py --list-articles

  # 2) 跑评测（读 evaluation/golden_set.json）
  python3 evaluation/run_eval.py

前置：后端(mvn spring-boot:run) 与 Ollama 均已启动，且已完成向量入库。
"""

import json
import re
import sys
import urllib.parse
import urllib.request

BACKEND = "http://localhost:8080"
OLLAMA = "http://localhost:11434"
CHAT_MODEL = "qwen3:0.6b"
TOP_K = 3
GOLDEN_SET = "evaluation/golden_set.json"
REPORT = "evaluation/report.md"


def http_get_json(url, timeout=300):
    with urllib.request.urlopen(url, timeout=timeout) as r:
        return json.loads(r.read().decode("utf-8"))


def ask(question, top_k=TOP_K):
    """调用非流式问答接口，返回 (answer, [articleId...])"""
    url = f"{BACKEND}/api/rag/ask?q={urllib.parse.quote(question)}&topK={top_k}"
    data = http_get_json(url)["data"]
    refs = data.get("references") or []
    ids = [int(ref["articleId"]) for ref in refs]
    return data.get("answer") or "", ids


def search(question, top_k=TOP_K):
    """调用检索接口，返回召回 chunk 原文列表与文章 id 列表"""
    url = f"{BACKEND}/api/rag/search?q={urllib.parse.quote(question)}&topK={top_k}"
    hits = http_get_json(url)["data"]
    texts, ids = [], []
    for hit in hits:
        p = hit.get("payload") or {}
        if p.get("text"):
            texts.append(p["text"])
        if p.get("articleId") is not None:
            ids.append(int(p["articleId"]))
    return texts, ids


def load_title_map():
    """返回 {文章标题: 文章id}，优先读导入脚本导出的映射，否则回源列表接口"""
    import os
    map_file = "content/imported_articles.json"
    if os.path.exists(map_file):
        with open(map_file, encoding="utf-8") as f:
            m = json.load(f)
        if m:
            return m
    page, size, title2id = 1, 50, {}
    while True:
        d = http_get_json(f"{BACKEND}/api/articles?page={page}&size={size}")["data"]
        for a in d.get("records") or []:
            title2id[(a.get("title") or "").strip()] = a.get("id")
        if page * size >= d.get("total", 0):
            break
        page += 1
    return title2id


def ollama_chat(system, user):
    payload = {
        "model": CHAT_MODEL,
        "stream": False,
        "keep_alive": "30m",
        "options": {"temperature": 0},
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
    }
    req = urllib.request.Request(
        f"{OLLAMA}/api/chat",
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(req, timeout=300) as r:
        return json.loads(r.read().decode("utf-8"))["message"]["content"]


def judge_faithfulness(contexts, answer):
    """答案每句是否被资料支撑 → 支持=1 / 部分支持=0.5 / 不支持=0"""
    ctx = "\n\n".join(f"[{i + 1}] {t}" for i, t in enumerate(contexts))
    out = ollama_chat(
        "你是严谨的评测裁判，只按资料判断，不要补充知识。",
        f"资料：\n{ctx}\n\n模型答案：{answer}\n\n"
        "请判断：模型答案中的每一句是否都能被上面的资料支撑？只输出一个词：支持 / 部分支持 / 不支持。",
    )
    if "部分支持" in out:
        return 0.5, out
    if "不支持" in out:
        return 0.0, out
    if "支持" in out:
        return 1.0, out
    return 0.0, out


def judge_relevance(question, ref_answer, answer):
    """与标准答案对比打分 1~5"""
    out = ollama_chat(
        "你是严谨的评测裁判。",
        f"问题：{question}\n标准答案：{ref_answer}\n模型答案：{answer}\n"
        "请给模型答案打分 1~5（5=完全正确且完整，1=完全错误）。只输出一个数字。",
    )
    m = re.search(r"[1-5]", out)
    return int(m.group()) if m else 0, out


def list_articles():
    page, size = 1, 50
    while True:
        data = http_get_json(f"{BACKEND}/api/articles?page={page}&size={size}")["data"]
        for a in data.get("records") or []:
            title = (a.get("title") or "").strip()
            summary = (a.get("summary") or "").strip()[:60]
            print(f"id={a.get('id')}  |  {title}  |  {summary}")
        if page * size >= data.get("total", 0):
            break
        page += 1


def run_eval():
    with open(GOLDEN_SET, encoding="utf-8") as f:
        cases = json.load(f)
    title2id = load_title_map()

    rows = []
    for c in cases:
        q = c["question"]
        # 解析正确答案文章 id：优先显式 id，否则按标题查
        exp_id = c.get("expected_article_id")
        if exp_id is None and c.get("expected_article_title"):
            exp_id = title2id.get(c["expected_article_title"])
        if exp_id is None:
            print(f"[{c['id']}] 跳过：找不到正确文章 id（标题={c.get('expected_article_title')}）")
            continue
        try:
            answer, ref_ids = ask(q)
            contexts, _ = search(q)
            hit1 = int(exp_id in ref_ids[:1])
            hit3 = int(exp_id in ref_ids[:3])
            faith, faith_raw = judge_faithfulness(contexts, answer)
            relev, relev_raw = judge_relevance(q, c["reference_answer"], answer)
        except Exception as e:
            print(f"[{c['id']}] 失败：{e}")
            rows.append((c, None, None, None, None))
            continue

        rows.append((c, hit1, hit3, faith, relev))
        print(f"[{c['id']}] {q}\n    Hit@1={hit1} Hit@3={hit3} "
              f"忠实度={faith} 相关性={relev}\n    refs={ref_ids}")

    n = len([r for r in rows if r[1] is not None])
    if n == 0:
        print("无有效结果，请确认后端/Ollama 已启动且已入库。")
        return
    hit1 = sum(r[1] for r in rows if r[1] is not None) / n
    hit3 = sum(r[2] for r in rows if r[2] is not None) / n
    faith = sum(r[3] for r in rows if r[3] is not None) / n
    relev = sum(r[4] for r in rows if r[4] is not None) / n

    summary = (
        f"\n===== 汇总（{n} 条）=====\n"
        f"检索命中率 Hit@1 : {hit1:.2%}\n"
        f"检索命中率 Hit@3 : {hit3:.2%}\n"
        f"平均忠实度 Faithfulness : {faith:.2f}（1=完全忠实）\n"
        f"平均相关性 Relevance : {relev:.1f}（1~5）\n"
    )
    print(summary)

    with open(REPORT, "w", encoding="utf-8") as f:
        f.write("# RAG 评测报告\n\n")
        for c, h1, h3, fa, rv in rows:
            f.write(f"- {c['id']}. {c['question']}")
            if h1 is None:
                f.write("（失败）\n")
            else:
                f.write(f" | Hit@1={h1} Hit@3={h3} 忠实度={fa} 相关性={rv}\n")
        f.write("\n" + summary)
    print(f"报告已写入 {REPORT}")


if __name__ == "__main__":
    if "--list-articles" in sys.argv:
        list_articles()
    else:
        run_eval()
