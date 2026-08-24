package com.jice19.blog.controller;

import com.jice19.blog.common.Result;
import com.jice19.blog.entity.Conversation;
import com.jice19.blog.entity.Message;
import com.jice19.blog.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话窗口管理
 */
@RestController
@RequestMapping("/api/rag/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** 会话列表 */
    @GetMapping
    public Result<List<Conversation>> list() {
        return Result.success(conversationService.list());
    }

    /** 新建会话 */
    @PostMapping
    public Result<Conversation> create(@RequestParam(required = false) String title) {
        return Result.success(conversationService.create(title));
    }

    /** 删除会话（级联删消息） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return Result.success();
    }

    /** 某会话的全部消息 */
    @GetMapping("/{id}/messages")
    public Result<List<Message>> messages(@PathVariable Long id) {
        return Result.success(conversationService.messages(id));
    }
}
