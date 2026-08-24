package com.jice19.blog.rag;

import com.jice19.blog.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 按标题切片（以 ## 二级标题为边界）。
 * 标题块超长时按句子二次切分 + 相邻片段 overlap 重叠，避免语义断裂与上下文丢失；
 * 子片继承原标题，引用溯源不受影响。
 * 说明：生产可用 commonmark-java/flexmark 做 AST 解析，避免代码块里的 # / 换行误判。
 */
@Service
@RequiredArgsConstructor
public class ChunkService {

    private final RagProperties props;

    /** 句子边界：中文/英文句末标点 + 换行（保留分隔符，避免"第一句。"被切掉句号） */
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？!?；;\\n]+");

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
                    chunks.addAll(splitOverflow(current.toString().trim(), heading.trim()));
                }
                current = new StringBuilder();
                heading = line.substring(3).trim();
                current.append(line).append("\n");
            } else {
                current.append(line).append("\n");
            }
        }
        if (!current.toString().isBlank()) {
            chunks.addAll(splitOverflow(current.toString().trim(), heading.trim()));
        }

        // 没有 ## 标题时，整篇作为一个标题块（同样走超长二次切分兜底）
        if (chunks.isEmpty() && !markdown.isBlank()) {
            chunks.addAll(splitOverflow(markdown.trim(), ""));
        }
        return chunks;
    }

    /**
     * 超长二次切分：不超过阈值直接成片；
     * 超过阈值按句子贪心打包，窗口间保留 overlap 字符重叠，heading 继承。
     */
    private List<Chunk> splitOverflow(String text, String heading) {
        int max = props.getChunkMaxChars();
        int overlap = props.getChunkOverlapChars();
        if (text.length() <= max) {
            return List.of(new Chunk(text, heading));
        }

        List<String> sentences = splitSentences(text);
        List<Chunk> result = new ArrayList<>();
        StringBuilder window = new StringBuilder();

        for (String sentence : sentences) {
            // 单句超长（无句末标点的长文本）：按字符硬切兜底，相邻片保留 overlap
            if (sentence.length() > max) {
                if (!window.isEmpty()) {
                    result.add(new Chunk(window.toString().trim(), heading));
                    window.setLength(0);
                }
                for (String piece : hardSplit(sentence, max, overlap)) {
                    result.add(new Chunk(piece, heading));
                }
                continue;
            }

            // 新窗口开头 = 上一片末尾 overlap 字符（重叠上下文；放不下则放弃前缀）
            if (window.isEmpty() && !result.isEmpty()) {
                String last = result.get(result.size() - 1).text();
                int from = Math.max(0, last.length() - overlap);
                String prefix = last.substring(from);
                if (prefix.length() + sentence.length() > max) {
                    window.append(sentence);
                    continue;
                }
                window.append(prefix);
            }

            if (window.length() + sentence.length() > max) {
                String prev = window.toString();
                result.add(new Chunk(prev.trim(), heading));
                int from = Math.max(0, prev.length() - overlap);
                window.setLength(0);
                window.append(prev, from, prev.length());
            }
            window.append(sentence);
        }
        if (!window.isEmpty()) {
            result.add(new Chunk(window.toString().trim(), heading));
        }
        return result;
    }

    /** 按句末标点/换行切成句子（分隔符保留在句尾） */
    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher m = SENTENCE_END.matcher(text);
        int last = 0;
        while (m.find()) {
            sentences.add(text.substring(last, m.end()));
            last = m.end();
        }
        if (last < text.length()) {
            sentences.add(text.substring(last));
        }
        return sentences;
    }

    /** 超长单句兜底：按字符硬切，相邻片段从上一片末尾 overlap 处开始 */
    private List<String> hardSplit(String text, int max, int overlap) {
        List<String> pieces = new ArrayList<>();
        int from = 0;
        while (from < text.length()) {
            int to = Math.min(text.length(), from + max);
            pieces.add(text.substring(from, to));
            if (to >= text.length()) {
                break;
            }
            from = to - overlap;
        }
        return pieces;
    }
}
