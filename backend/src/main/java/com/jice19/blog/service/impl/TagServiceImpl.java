package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.TagDTO;
import com.jice19.blog.entity.ArticleTag;
import com.jice19.blog.entity.Tag;
import com.jice19.blog.mapper.ArticleTagMapper;
import com.jice19.blog.mapper.TagMapper;
import com.jice19.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 标签服务：列表走 Redis 缓存（Cache-Aside），变更时失效。
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private static final String CACHE_KEY = "cache:tags";
    private static final long TTL_HOURS = 1;

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<Tag> listAll() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<Tag>>() {});
            } catch (Exception ignored) {
                // 反序列化失败则回源查库
            }
        }
        List<Tag> list = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
        try {
            redisTemplate.opsForValue().set(CACHE_KEY,
                    objectMapper.writeValueAsString(list), TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
            // 缓存写入失败不影响主流程
        }
        return list;
    }

    @Override
    public void create(TagDTO dto) {
        checkNameUnique(dto.getName(), null);
        Tag t = new Tag();
        t.setName(dto.getName());
        tagMapper.insert(t);
        evictCache();
    }

    @Override
    public void update(Long id, TagDTO dto) {
        Tag t = tagMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "标签不存在");
        }
        checkNameUnique(dto.getName(), id);
        t.setName(dto.getName());
        tagMapper.updateById(t);
        evictCache();
    }

    @Override
    public void delete(Long id) {
        tagMapper.deleteById(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));
        evictCache();
    }

    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Tag> w = new LambdaQueryWrapper<Tag>().eq(Tag::getName, name);
        if (excludeId != null) {
            w.ne(Tag::getId, excludeId);
        }
        Long count = tagMapper.selectCount(w);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "标签名已存在");
        }
    }

    private void evictCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}
