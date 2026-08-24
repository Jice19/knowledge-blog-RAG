package com.jice19.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 文章新增/修改入参
 */
@Data
public class ArticleDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String summary;

    @NotBlank(message = "内容不能为空")
    private String content;

    private String cover;

    private Long categoryId;

    private List<Long> tagIds;

    /** 0 草稿 / 1 已发布 */
    private Integer status;
}
