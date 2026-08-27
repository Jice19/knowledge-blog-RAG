package com.jice19.blog.service;

import com.jice19.blog.dto.ArticleDTO;
import com.jice19.blog.dto.CategoryDTO;
import com.jice19.blog.entity.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 笔记导入：把超大 Markdown 笔记按 # 一级标题拆成多篇文章，自动推断分类并发布。
 * 发布复用 ArticleService.create（触发 RabbitMQ 异步向量化）。
 */
@Service
@RequiredArgsConstructor
public class NotesImportService {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    /** 文章边界：# 或 ##（三级及以下保留在正文，交给 ChunkService 切片） */
    private static final Pattern BOUNDARY = Pattern.compile("^(#{1,2})\\s+(.+)$");

    /** 标题关键词 → 分类（顺序匹配，命中即停） */
    private static final List<String[]> CATEGORY_RULES = List.of(
            new String[]{"agent", "Agent"},
            new String[]{"vue", "前端"}, new String[]{"vite", "前端"}, new String[]{"webpack", "前端"},
            new String[]{"npm", "前端"}, new String[]{"pnpm", "前端"}, new String[]{"yarn", "前端"},
            new String[]{"js", "前端"}, new String[]{"es6", "前端"}, new String[]{"es5", "前端"},
            new String[]{"git", "前端"}, new String[]{"promise", "前端"}, new String[]{"函数式", "前端"},
            new String[]{"模块化", "前端"}, new String[]{"工程化", "前端"}, new String[]{"响应式", "前端"},
            new String[]{"原型", "前端"}, new String[]{"防抖", "前端"}, new String[]{"节流", "前端"},
            new String[]{"柯里化", "前端"}, new String[]{"watch", "前端"}, new String[]{"computed", "前端"},
            new String[]{"计网", "网络"},
            new String[]{"前端", "前端"}, new String[]{"javascript", "前端"}, new String[]{"typescript", "前端"},
            new String[]{"react", "前端"}, new String[]{"css", "前端"}, new String[]{"html", "前端"},
            new String[]{"浏览器", "前端"},
            new String[]{"面试", "面试题"}, new String[]{"八股", "面试题"},
            new String[]{"手写", "手写题"}, new String[]{"算法", "手写题"},
            new String[]{"redis", "Redis"}, new String[]{"mysql", "MySQL"}, new String[]{"sql", "MySQL"},
            new String[]{"rabbitmq", "RabbitMQ"}, new String[]{"mq", "RabbitMQ"},
            new String[]{"spring", "Spring"}, new String[]{"mybatis", "Spring"}, new String[]{"java", "Java"},
            new String[]{"jvm", "Java"}, new String[]{"线程", "Java"}, new String[]{"并发", "Java"},
            new String[]{"集合", "Java"},
            new String[]{"后端", "后端"}, new String[]{"分布式", "后端"}, new String[]{"微服务", "后端"},
            new String[]{"设计模式", "后端"},
            new String[]{"网络", "网络"}, new String[]{"http", "网络"}, new String[]{"tcp", "网络"}
    );

    private static final Map<String, String> CATEGORY_SLUGS = Map.ofEntries(
            Map.entry("Java", "java"), Map.entry("Redis", "redis"), Map.entry("RabbitMQ", "mq"),
            Map.entry("Spring", "spring"), Map.entry("MySQL", "mysql"), Map.entry("网络", "network"),
            Map.entry("RAG", "rag"), Map.entry("前端", "frontend"), Map.entry("后端", "backend"),
            Map.entry("Agent", "agent"), Map.entry("面试题", "interview"),
            Map.entry("手写题", "handwritten"), Map.entry("个人笔记", "notes")
    );

    public record Note(String title, String summary, String content, String category) {
    }

    /** 拆分 + 发布，返回 {created, skipped, titles} */
    public Map<String, Object> importMarkdown(String markdown) {
        List<Note> notes = split(markdown);

        Map<String, Long> name2id = new HashMap<>();
        for (Category c : categoryService.listAll()) {
            name2id.put(c.getName(), c.getId());
        }

        int created = 0, skipped = 0;
        List<String> titles = new ArrayList<>();
        for (Note n : notes) {
            if (n.content().isBlank()) {
                skipped++;
                continue;
            }
            Long cid = name2id.get(n.category());
            if (cid == null) {
                cid = createCategory(n.category());
                if (cid == null) {
                    skipped++;
                    continue;
                }
                name2id.put(n.category(), cid);
            }
            ArticleDTO dto = new ArticleDTO();
            dto.setTitle(n.title());
            dto.setSummary(n.summary());
            dto.setContent(n.content());
            dto.setCategoryId(cid);
            dto.setStatus(1);
            dto.setTagIds(List.of());
            articleService.create(dto);
            created++;
            titles.add(n.title());
        }
        return Map.of("created", created, "skipped", skipped, "titles", titles);
    }

    private List<Note> split(String markdown) {
        List<Note> result = splitBy(markdown, BOUNDARY);
        if (result.isEmpty()) {
            result.add(new Note("个人笔记", "", markdown.trim(), "个人笔记"));
        }
        return result;
    }

    /** 按 # / ## 拆分（标题行不进正文）；标题之前的引导内容归入「其他笔记」；空正文跳过 */
    private List<Note> splitBy(String markdown, Pattern heading) {
        List<Note> notes = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return notes;
        }
        String[] lines = markdown.split("\n");
        String curTitle = null;
        List<String> curBody = new ArrayList<>();
        for (String line : lines) {
            Matcher m = heading.matcher(line.trim());
            if (m.matches()) {
                if (curTitle != null) {
                    notes.add(buildNote(curTitle, curBody));
                } else if (!joinBlank(curBody)) {
                    notes.add(buildNote("其他笔记", curBody));
                }
                curTitle = m.group(2).trim();
                curBody = new ArrayList<>();
            } else {
                curBody.add(line);
            }
        }
        if (curTitle != null) {
            notes.add(buildNote(curTitle, curBody));
        } else if (!joinBlank(curBody)) {
            notes.add(buildNote("其他笔记", curBody));
        }
        return notes;
    }

    private boolean joinBlank(List<String> body) {
        return String.join("\n", body).trim().isBlank();
    }

    private Note buildNote(String title, List<String> body) {
        String content = String.join("\n", body).trim();
        String summary = extractSummary(body);
        String category = inferCategory(title);
        // 标题截断，避免超出数据库 VARCHAR(200)
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }
        return new Note(title, summary, content, category);
    }

    /** 摘要：首个非空、非标题、非代码块的正文行 */
    private String extractSummary(List<String> body) {
        boolean inFence = false;
        for (String line : body) {
            String t = line.trim();
            if (t.startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (inFence || t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            return t.length() > 200 ? t.substring(0, 200) : t;
        }
        return "";
    }

    private String inferCategory(String title) {
        String t = title.toLowerCase();
        String cat = "个人笔记";
        for (String[] rule : CATEGORY_RULES) {
            if (t.contains(rule[0])) {
                cat = rule[1];
                break;
            }
        }
        // 复用已有的种子分类，避免重复建「前端」/「后端」这类相近分类
        if ("前端".equals(cat)) {
            return "前端开发";
        }
        if ("后端".equals(cat)) {
            return "后端开发";
        }
        return cat;
    }

    private Long createCategory(String name) {
        // slug 防冲突：已被占用则加 -2、-3 后缀
        Set<String> slugs = categoryService.listAll().stream()
                .map(Category::getSlug).collect(Collectors.toSet());
        String base = CATEGORY_SLUGS.getOrDefault(name, "cat-" + Math.abs(name.hashCode()));
        String slug = base;
        for (int i = 2; slugs.contains(slug); i++) {
            slug = base + "-" + i;
        }
        CategoryDTO dto = new CategoryDTO();
        dto.setName(name);
        dto.setSlug(slug);
        dto.setSort(0);
        categoryService.create(dto);
        return categoryService.listAll().stream()
                .filter(c -> c.getName().equals(name))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
    }
}
