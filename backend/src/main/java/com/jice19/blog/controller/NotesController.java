package com.jice19.blog.controller;

import com.jice19.blog.common.Result;
import com.jice19.blog.service.NotesImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 笔记上传导入：上传 Markdown，自动按 # 标题拆分成多篇文章并发布（异步向量化）
 */
@RestController
@RequestMapping("/api/admin/notes")
@RequiredArgsConstructor
public class NotesController {

    private final NotesImportService notesImportService;

    @PostMapping("/import")
    public Result<Map<String, Object>> importNotes(@RequestParam("file") MultipartFile file) throws IOException {
        String markdown = new String(file.getBytes(), StandardCharsets.UTF_8);
        return Result.success(notesImportService.importMarkdown(markdown));
    }
}
