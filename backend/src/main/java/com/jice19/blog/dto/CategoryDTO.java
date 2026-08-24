package com.jice19.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 分类新增/修改入参
 */
@Data
public class CategoryDTO {

    @NotBlank(message = "分类名不能为空")
    private String name;

    @NotBlank(message = "slug 不能为空")
    private String slug;

    private Integer sort;
}
