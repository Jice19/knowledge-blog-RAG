package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.TagDTO;
import com.jice19.blog.entity.ArticleTag;
import com.jice19.blog.entity.Tag;
import com.jice19.blog.mapper.ArticleTagMapper;
import com.jice19.blog.mapper.TagMapper;
import com.jice19.blog.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;

    @Override
    public List<Tag> listAll() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
    }

    @Override
    public void create(TagDTO dto) {
        checkNameUnique(dto.getName(), null);
        Tag t = new Tag();
        t.setName(dto.getName());
        tagMapper.insert(t);
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
    }

    @Override
    public void delete(Long id) {
        tagMapper.deleteById(id);
        // 清理文章-标签关联
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));
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
}
