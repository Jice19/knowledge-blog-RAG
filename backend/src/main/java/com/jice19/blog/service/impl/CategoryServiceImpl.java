package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.CategoryDTO;
import com.jice19.blog.entity.Category;
import com.jice19.blog.mapper.CategoryMapper;
import com.jice19.blog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    @Override
    public void create(CategoryDTO dto) {
        checkNameUnique(dto.getName(), null);
        Category c = new Category();
        c.setName(dto.getName());
        c.setSlug(dto.getSlug());
        c.setSort(dto.getSort() == null ? 0 : dto.getSort());
        categoryMapper.insert(c);
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
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
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
}
