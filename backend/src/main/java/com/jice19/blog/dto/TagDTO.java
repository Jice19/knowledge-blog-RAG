package com.jice19.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 标签新增/修改入参
 */
@Data
public class TagDTO {

    @NotBlank(message = "标签名不能为空")
    private String name;
}
