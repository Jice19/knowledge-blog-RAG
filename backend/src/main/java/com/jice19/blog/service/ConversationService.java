package com.jice19.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jice19.blog.entity.Conversation;
import com.jice19.blog.entity.Message;
import com.jice19.blog.mapper.ConversationMapper;
import com.jice19.blog.mapper.MessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理：多轮对话的窗口与消息持久化
 */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public List<Conversation> list() {
        return conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .orderByDesc(Conversation::getUpdateTime));
    }

    public Conversation create(String title) {
        Conversation c = new Conversation();
        c.setUserId(1L);
        c.setTitle(title == null || title.isBlank() ? "新会话" : title);
        conversationMapper.insert(c);
        return c;
    }

    @Transactional
    public void delete(Long id) {
        messageMapper.delete(new LambdaQueryWrapper<Message>().eq(Message::getConversationId, id));
        conversationMapper.deleteById(id);
    }

    public List<Message> messages(Long conversationId) {
        return messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getId));
    }

    public void addMessage(Long conversationId, String role, String content, String referencesJson) {
        Message m = new Message();
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setReferencesJson(referencesJson);
        messageMapper.insert(m);

        Conversation c = new Conversation();
        c.setId(conversationId);
        c.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(c);
    }

    /** 新会话用首个问题更新标题 */
    public void updateTitleIfDefault(Long conversationId, String question) {
        Conversation c = conversationMapper.selectById(conversationId);
        if (c != null && (c.getTitle() == null || "新会话".equals(c.getTitle()))) {
            c.setTitle(question.length() > 20 ? question.substring(0, 20) : question);
            conversationMapper.updateById(c);
        }
    }

    /** 取最近 limit 轮 (q, a) 作为多轮改写历史 */
    public List<Map<String, String>> history(Long conversationId, int limit) {
        List<Message> msgs = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getId));
        List<Map<String, String>> history = new ArrayList<>();
        for (int i = 0; i + 1 < msgs.size(); i += 2) {
            Message u = msgs.get(i);
            Message a = msgs.get(i + 1);
            if ("user".equals(u.getRole())) {
                Map<String, String> h = new HashMap<>();
                h.put("q", u.getContent());
                if ("assistant".equals(a.getRole())) {
                    h.put("a", a.getContent());
                }
                history.add(h);
            }
        }
        int from = Math.max(0, history.size() - limit);
        return history.subList(from, history.size());
    }
}
