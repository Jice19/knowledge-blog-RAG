package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jice19.blog.common.PageResult;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.ArticleDTO;
import com.jice19.blog.entity.Article;
import com.jice19.blog.entity.ArticleTag;
import com.jice19.blog.entity.Category;
import com.jice19.blog.entity.Tag;
import com.jice19.blog.entity.User;
import com.jice19.blog.mapper.ArticleMapper;
import com.jice19.blog.mapper.ArticleTagMapper;
import com.jice19.blog.mapper.CategoryMapper;
import com.jice19.blog.mapper.TagMapper;
import com.jice19.blog.mapper.UserMapper;
import com.jice19.blog.security.UserContext;
import com.jice19.blog.service.ArticleService;
import com.jice19.blog.vo.ArticleVO;
import com.jice19.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 文章服务。
 * 说明：列表查询使用「批量查询 + 内存组装」，避免 N+1（每篇文章单独查分类/标签/作者）。
 */
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<ArticleVO> pagePublished(long page, long size, Long categoryId) {
        LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<Article>().eq(Article::getStatus, 1);
        if (categoryId != null) {
            w.eq(Article::getCategoryId, categoryId);
        }
        w.orderByDesc(Article::getId);
        return doPage(page, size, w);
    }

    @Override
    public PageResult<ArticleVO> pageAll(long page, long size, Integer status) {
        LambdaQueryWrapper<Article> w = new LambdaQueryWrapper<>();
        if (status != null) {
            w.eq(Article::getStatus, status);
        }
        w.orderByDesc(Article::getId);
        return doPage(page, size, w);
    }

    private PageResult<ArticleVO> doPage(long page, long size, LambdaQueryWrapper<Article> w) {
        Page<Article> p = articleMapper.selectPage(new Page<>(page, size), w);
        List<ArticleVO> records = batchToVO(p.getRecords());
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), records);
    }

    @Override
    public ArticleVO getPublishedDetail(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null || a.getStatus() != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章不存在");
        }
        // 浏览量原子自增
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id)
                .setSql("view_count = view_count + 1"));
        a.setViewCount(a.getViewCount() + 1);
        ArticleVO vo = batchToVO(List.of(a)).get(0);
        vo.setContent(a.getContent());
        return vo;
    }

    @Override
    public ArticleVO getById(Long id) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章不存在");
        }
        ArticleVO vo = batchToVO(List.of(a)).get(0);
        vo.setContent(a.getContent());
        return vo;
    }

    @Override
    @Transactional
    public void create(ArticleDTO dto) {
        Article a = new Article();
        a.setTitle(dto.getTitle());
        a.setSummary(dto.getSummary());
        a.setContent(dto.getContent());
        a.setCover(dto.getCover());
        a.setCategoryId(dto.getCategoryId());
        a.setAuthorId(UserContext.get().userId());
        a.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        a.setViewCount(0);
        articleMapper.insert(a);
        saveTags(a.getId(), dto.getTagIds());
    }

    @Override
    @Transactional
    public void update(Long id, ArticleDTO dto) {
        Article a = articleMapper.selectById(id);
        if (a == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "文章不存在");
        }
        a.setTitle(dto.getTitle());
        a.setSummary(dto.getSummary());
        a.setContent(dto.getContent());
        a.setCover(dto.getCover());
        a.setCategoryId(dto.getCategoryId());
        a.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        articleMapper.updateById(a);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        saveTags(id, dto.getTagIds());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        articleMapper.deleteById(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
    }

    private void saveTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        }
    }

    /**
     * 批量组装 VO：一次性查出所有分类 / 作者 / 标签，再内存映射，避免 N+1。
     */
    private List<ArticleVO> batchToVO(List<Article> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }
        List<Long> categoryIds = articles.stream()
                .map(Article::getCategoryId).filter(Objects::nonNull).distinct().toList();
        List<Long> authorIds = articles.stream()
                .map(Article::getAuthorId).distinct().toList();
        List<Long> articleIds = articles.stream().map(Article::getId).toList();

        Map<Long, Category> catMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, List<TagVO>> tagMap = batchQueryTags(articleIds);

        return articles.stream().map(a -> toVO(a, catMap, userMap, tagMap)).toList();
    }

    private Map<Long, List<TagVO>> batchQueryTags(List<Long> articleIds) {
        List<ArticleTag> ats = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().in(ArticleTag::getArticleId, articleIds));
        if (ats.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = ats.stream().map(ArticleTag::getTagId).distinct().toList();
        Map<Long, Tag> tagById = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, t -> t));

        Map<Long, List<TagVO>> result = new HashMap<>();
        for (ArticleTag at : ats) {
            Tag t = tagById.get(at.getTagId());
            if (t != null) {
                result.computeIfAbsent(at.getArticleId(), k -> new ArrayList<>())
                        .add(new TagVO(t.getId(), t.getName()));
            }
        }
        return result;
    }

    private ArticleVO toVO(Article a, Map<Long, Category> catMap,
                           Map<Long, User> userMap, Map<Long, List<TagVO>> tagMap) {
        ArticleVO vo = new ArticleVO();
        vo.setId(a.getId());
        vo.setTitle(a.getTitle());
        vo.setSummary(a.getSummary());
        vo.setCover(a.getCover());
        vo.setCategoryId(a.getCategoryId());
        vo.setAuthorId(a.getAuthorId());
        vo.setStatus(a.getStatus());
        vo.setViewCount(a.getViewCount());
        vo.setCreateTime(a.getCreateTime());
        vo.setUpdateTime(a.getUpdateTime());

        if (a.getCategoryId() != null) {
            Category c = catMap.get(a.getCategoryId());
            if (c != null) {
                vo.setCategoryName(c.getName());
                vo.setCategorySlug(c.getSlug());
            }
        }
        vo.setTags(tagMap.getOrDefault(a.getId(), List.of()));

        User u = userMap.get(a.getAuthorId());
        if (u != null) {
            vo.setAuthorName(u.getNickname() != null ? u.getNickname() : u.getUsername());
        }
        return vo;
    }
}
