package com.jice19.blog.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章出参（聚合分类名 / 标签 / 作者名）
 */
@Data
public class ArticleVO {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private List<TagVO> tags;
    private Long authorId;
    private String authorName;
    private Integer status;
    private Integer viewCount;
    private Integer vectorStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
