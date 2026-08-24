package com.jice19.blog.service;

import com.jice19.blog.common.PageResult;
import com.jice19.blog.dto.ArticleDTO;
import com.jice19.blog.vo.ArticleVO;

import java.util.List;

public interface ArticleService {

    /** 前台：分页查询已发布文章 */
    PageResult<ArticleVO> pagePublished(long page, long size, Long categoryId);

    /** 后台：分页查询全部文章（可按状态过滤） */
    PageResult<ArticleVO> pageAll(long page, long size, Integer status);

    /** 前台：文章详情（浏览量自增，仅已发布） */
    ArticleVO getPublishedDetail(Long id);

    /** 后台：按 id 查文章（任意状态，不自增） */
    ArticleVO getById(Long id);

    /** 前台：热门文章 Top N（Redis ZSet 计数） */
    List<ArticleVO> hotArticles(int limit);

    void create(ArticleDTO dto);

    void update(Long id, ArticleDTO dto);

    void delete(Long id);
}
