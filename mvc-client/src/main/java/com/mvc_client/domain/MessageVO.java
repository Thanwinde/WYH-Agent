package com.mvc_client.domain;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;

@Data
public class MessageVO {

    private String content;

    private String type;

    public MessageVO(Message message) {
        this.content = message.getText();
        this.type = String.valueOf(message.getMessageType());
    }
}
