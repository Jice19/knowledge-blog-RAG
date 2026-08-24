package com.jice19.blog.controller;

import com.jice19.blog.common.Result;
import com.jice19.blog.dto.TagDTO;
import com.jice19.blog.entity.Tag;
import com.jice19.blog.service.TagService;
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
 * 标签：公开列表 + 管理端增删改
 */
@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /** 公开：标签列表 */
    @GetMapping("/api/tags")
    public Result<List<Tag>> list() {
        return Result.success(tagService.listAll());
    }

    /** 管理端：新增 */
    @PostMapping("/api/admin/tags")
    public Result<Void> create(@RequestBody @Valid TagDTO dto) {
        tagService.create(dto);
        return Result.success();
    }

    /** 管理端：修改 */
    @PutMapping("/api/admin/tags/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid TagDTO dto) {
        tagService.update(id, dto);
        return Result.success();
    }

    /** 管理端：删除 */
    @DeleteMapping("/api/admin/tags/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
}
