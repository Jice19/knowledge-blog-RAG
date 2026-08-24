package com.jice19.blog.service;

import com.jice19.blog.dto.CategoryDTO;
import com.jice19.blog.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> listAll();

    void create(CategoryDTO dto);

    void update(Long id, CategoryDTO dto);

    void delete(Long id);
}
