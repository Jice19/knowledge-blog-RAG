package com.jice19.blog.service;

import com.jice19.blog.dto.TagDTO;
import com.jice19.blog.entity.Tag;

import java.util.List;

public interface TagService {

    List<Tag> listAll();

    void create(TagDTO dto);

    void update(Long id, TagDTO dto);

    void delete(Long id);
}
