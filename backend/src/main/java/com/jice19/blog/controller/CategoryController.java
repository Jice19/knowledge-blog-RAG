package com.jice19.blog.controller;

import com.jice19.blog.common.Result;
import com.jice19.blog.dto.CategoryDTO;
import com.jice19.blog.entity.Category;
import com.jice19.blog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类：公开列表 + 管理端增删改
 */
@RestController
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 公开：分类列表 */
    @GetMapping("/api/categories")
    public Result<List<Category>> list() {
        return Result.success(categoryService.listAll());
    }

    /** 管理端：新增 */
    @PostMapping("/api/admin/categories")
    public Result<Void> create(@RequestBody @Valid CategoryDTO dto) {
        categoryService.create(dto);
        return Result.success();
    }

    /** 管理端：修改 */
    @PutMapping("/api/admin/categories/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid CategoryDTO dto) {
        categoryService.update(id, dto);
        return Result.success();
    }

    /** 管理端：删除 */
    @DeleteMapping("/api/admin/categories/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
