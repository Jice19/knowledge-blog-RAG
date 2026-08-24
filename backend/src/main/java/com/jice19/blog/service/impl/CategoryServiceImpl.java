package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.CategoryDTO;
import com.jice19.blog.entity.Category;
import com.jice19.blog.mapper.CategoryMapper;
import com.jice19.blog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分类服务：列表走 Redis 缓存（Cache-Aside），变更时失效。
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String CACHE_KEY = "cache:categories";
    private static final long TTL_HOURS = 1;

    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<Category> listAll() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<Category>>() {});
            } catch (Exception ignored) {
                // 反序列化失败则回源查库
            }
        }
        List<Category> list = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        try {
            redisTemplate.opsForValue().set(CACHE_KEY,
                    objectMapper.writeValueAsString(list), TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
            // 缓存写入失败不影响主流程
        }
        return list;
    }

    @Override
    public void create(CategoryDTO dto) {
        checkNameUnique(dto.getName(), null);
        Category c = new Category();
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        categoryMapper.insert(c);
        evictCache();
    }

    @Override
    public void update(Long id, CategoryDTO dto) {
        Category c = categoryMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类不存在");
        }
        checkNameUnique(dto.getName(), id);
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        categoryMapper.updateById(c);
        evictCache();
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
        evictCache();
    }

    private void checkNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<Category> w = new LambdaQueryWrapper<Category>().eq(Category::getName, name);
        if (excludeId != null) {
            w.ne(Category::getId, excludeId);
        }
        Long count = categoryMapper.selectCount(w);
        if (count != null && count > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "分类名已存在");
        }
    }

    private void evictCache() {
        redisTemplate.delete(CACHE_KEY);
    }
}
