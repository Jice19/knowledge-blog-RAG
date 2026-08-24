package com.jice19.blog.rag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 按标题切片（简化版：以 ## 二级标题为边界）。
 * 说明：生产可用 commonmark-java/flexmark 做 AST 解析，避免代码块里的 # 误判。
 */
@Service
public class ChunkService {

    public List<Chunk> chunk(String markdown) {
        List<Chunk> chunks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return chunks;
        }

        String[] lines = markdown.split("\n");
        StringBuilder current = new StringBuilder();
        String heading = "";

        for (String line : lines) {
            if (line.startsWith("## ")) {
                if (!current.toString().isBlank()) {
                    chunks.add(new Chunk(current.toString().trim(), heading.trim()));
                }
                current = new StringBuilder();
                heading = line.substring(3).trim();
                current.append(line).append("\n");
            } else {
                current.append(line).append("\n");
            }
        }
        if (!current.toString().isBlank()) {
            chunks.add(new Chunk(current.toString().trim(), heading.trim()));
        }

        // 没有 ## 标题时，整篇作为一块
        if (chunks.isEmpty() && !markdown.isBlank()) {
            chunks.add(new Chunk(markdown.trim(), ""));
        }
        return chunks;
    }
}
