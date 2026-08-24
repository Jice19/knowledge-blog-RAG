package com.jice19.blog.controller;

import com.jice19.blog.common.PageResult;
import com.jice19.blog.common.Result;
import com.jice19.blog.dto.ArticleDTO;
import com.jice19.blog.service.ArticleService;
import com.jice19.blog.vo.ArticleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文章：公开列表/详情 + 管理端增删改查
 */
@RestController
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /** 公开：分页查询已发布文章（可按分类过滤） */
    @GetMapping("/api/articles")
    public Result<PageResult<ArticleVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(articleService.pagePublished(page, size, categoryId));
    }

    /** 公开：文章详情（浏览量自增） */
    @GetMapping("/api/articles/{id}")
    public Result<ArticleVO> detail(@PathVariable Long id) {
        return Result.success(articleService.getPublishedDetail(id));
    }

    /** 管理端：分页查询全部文章 */
    @GetMapping("/api/admin/articles")
    public Result<PageResult<ArticleVO>> adminPage(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.success(articleService.pageAll(page, size, status));
    }

    /** 管理端：按 id 查文章（任意状态） */
    @GetMapping("/api/admin/articles/{id}")
    public Result<ArticleVO> getById(@PathVariable Long id) {
        return Result.success(articleService.getById(id));
    }

    /** 管理端：新增文章 */
    @PostMapping("/api/admin/articles")
    public Result<Void> create(@RequestBody @Valid ArticleDTO dto) {
        articleService.create(dto);
        return Result.success();
    }

    /** 管理端：修改文章 */
    @PutMapping("/api/admin/articles/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid ArticleDTO dto) {
        articleService.update(id, dto);
        return Result.success();
    }

    /** 管理端：删除文章 */
    @DeleteMapping("/api/admin/articles/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }
}
