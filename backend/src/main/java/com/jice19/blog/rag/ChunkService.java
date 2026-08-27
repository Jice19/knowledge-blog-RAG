package com.jice19.blog.rag;

import com.jice19.blog.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 切片：支持 # / ## / ### 多级标题，切片 heading 存完整标题路径（如 "Redis > 缓存 > 穿透"）。
 * 标题块超长时按句子二次切分 + overlap，子片继承标题路径；扫描标题时跳过代码块，避免代码里的 # 误判。
 */
@Service
@RequiredArgsConstructor
public class ChunkService {

    private final RagProperties props;

    /** 一/二/三级标题：level + 标题文本 */
    private static final Pattern HEADING = Pattern.compile("^(#{1,3})\\s+(.+)$");

    /** 句子边界：中文/英文句末标点 + 换行 */
    private static final Pattern SENTENCE_END = Pattern.compile("[。！？!?；;\\n]+");

    public List<Chunk> chunk(String markdown) {
        List<Chunk> chunks = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return chunks;
        }

        String[] lines = markdown.split("\n");
        StringBuilder current = new StringBuilder();
        String h1 = "", h2 = "", h3 = "";
        boolean inFence = false;
        boolean hasBody = false;

        for (String line : lines) {
            String trimmed = line.trim();

            // 代码块围栏切换（``` 开头）
            if (trimmed.startsWith("```")) {
                inFence = !inFence;
                current.append(line).append("\n");
                hasBody = true;
                continue;
            }

            if (!inFence) {
                Matcher m = HEADING.matcher(trimmed);
                if (m.matches()) {
                    // 上一标题块有正文才落盘；纯标题行不单独成片，只作为标题路径
                    if (hasBody && !current.toString().isBlank()) {
                        chunks.addAll(splitOverflow(current.toString().trim(), path(h1, h2, h3)));
                    }
                    current = new StringBuilder();
                    hasBody = false;

                    int level = m.group(1).length();
                    String title = m.group(2).trim();
                    if (level == 1) {
                        h1 = title; h2 = ""; h3 = "";
                    } else if (level == 2) {
                        h2 = title; h3 = "";
                    } else {
                        h3 = title;
                    }
                    current.append(line).append("\n");
                    continue;
                }
            }
            current.append(line).append("\n");
            if (!trimmed.isEmpty()) {
                hasBody = true;
            }
        }

        if (hasBody && !current.toString().isBlank()) {
            chunks.addAll(splitOverflow(current.toString().trim(), path(h1, h2, h3)));
        }

        // 完全没有标题时，整篇作为一个标题块（同样走超长二次切分兜底）
        if (chunks.isEmpty() && !markdown.isBlank()) {
            chunks.addAll(splitOverflow(markdown.trim(), ""));
        }
        return chunks;
    }

    /** 拼接非空标题层级为完整路径，如 "一级 > 二级 > 三级" */
    private String path(String h1, String h2, String h3) {
        List<String> parts = new ArrayList<>();
        if (!h1.isBlank()) {
            parts.add(h1);
        }
        if (!h2.isBlank()) {
            parts.add(h2);
        }
        if (!h3.isBlank()) {
            parts.add(h3);
        }
        return String.join(" > ", parts);
    }

    /** 超长二次切分：不超过阈值直接成片；超过按句子贪心打包 + overlap，heading 继承 */
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
