package com.jice19.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章表
 * 说明：content 存 Markdown 原文，是 Phase 2「切片 → 向量化」的输入源
 */
@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    /** Markdown 正文 */
    private String content;

    private String cover;

    private Long categoryId;

    private Long authorId;

    /** 0 草稿 / 1 已发布 */
    private Integer status;

    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
