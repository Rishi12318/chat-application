package com.chatapp.model;

import com.chatapp.util.CustomStringUtil;
import com.chatapp.util.CustomTimeUtil;

public class Message {
    private String sender;
    private String content;
    private long timestamp; // epoch millis
    private MessageType type;
    private String recipient;
    
    public Message(String sender, String content, long timestamp, MessageType type, String recipient) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.type = type;
        this.recipient = recipient;
    }
    
    public String getSender() { return sender; }
    public String getContent() { return content; }
    public long getTimestamp() { return timestamp; }
    public MessageType getType() { return type; }
    public String getRecipient() { return recipient; }
    
    public String getFormattedTime() {
        return CustomTimeUtil.formatTime(timestamp);
    }
    
    @Override
    public String toString() {
        return CustomStringUtil.customFormat("[%s] %s: %s", getFormattedTime(), sender, content);
    }
}
