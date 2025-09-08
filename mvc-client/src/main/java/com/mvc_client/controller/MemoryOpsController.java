package com.mvc_client.controller;

import com.mvc_client.Mapper.MemoryMapper;
import com.mvc_client.domain.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai/history")
@RequiredArgsConstructor
class MemoryOpsController {

    private final ChatMemory chatMemory;
    private final MemoryMapper memoryMapper;

    @GetMapping("/{userId}")
    public List<String> getAllChatHistory(@PathVariable String userId) {
        return memoryMapper.getHistoryId();
    }

    @GetMapping("/{userId}/{chatId}")
    public List<MessageVO> getOneChatMemory(@PathVariable String userId, @PathVariable String chatId) {
        List<Message> messages = chatMemory.get(chatId);
        return messages.stream().map(MessageVO::new).toList();
    }

    @GetMapping("/{userId}/clean")
    public void cleanHistory(@PathVariable String userId, @RequestParam String chatId) {
        chatMemory.clear(chatId);
    }
}
