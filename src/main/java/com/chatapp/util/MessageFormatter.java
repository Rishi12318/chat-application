package com.chatapp.util;

import com.chatapp.model.Message;

public class MessageFormatter {
    
    public static String format(Message message) {
        String time = message.getFormattedTime();
        
        switch (message.getType()) {
            case SYSTEM:
                if (CustomStringUtil.customStartsWith(message.getContent(), "USERS:")) {
                    return formatUserList(message.getContent());
                }
                return CustomStringUtil.customFormat("[%s] 🔵 SYSTEM: %s", time, message.getContent());
                
            case PRIVATE:
                return CustomStringUtil.customFormat("[%s] 💜 PRIVATE from %s: %s", time, message.getSender(), message.getContent());
                
            case ERROR:
                return CustomStringUtil.customFormat("[%s] ❌ ERROR: %s", time, message.getContent());
                
            case PUBLIC:
            default:
                if (CustomStringUtil.customEqualsIgnoreCase(message.getSender(), "SERVER")) {
                    return CustomStringUtil.customFormat("[%s] 🟢 %s", time, message.getContent());
                }
                return CustomStringUtil.customFormat("[%s] 💬 %s: %s", time, message.getSender(), message.getContent());
        }
    }
    
    private static String padRight(String s, int width) {
        if (s == null) s = "null";
        int len = s.length();
        if (len >= width) return s;
        char[] chars = new char[width];
        for (int i = 0; i < len; i++) {
            chars[i] = s.charAt(i);
        }
        for (int i = len; i < width; i++) {
            chars[i] = ' ';
        }
        return new String(chars);
    }
    
    private static String formatUserList(String content) {
        String users = CustomStringUtil.customSubstringFrom(content, 6);
        String[] userArray = CustomStringUtil.customSplit(users, ',', 0);
        CustomStringUtil.CustomStringBuilder sb = new CustomStringUtil.CustomStringBuilder();
        sb.append("\n┌────────────────────────────────────────┐\n");
        sb.append("│        📍 ONLINE USERS                 │\n");
        sb.append("├────────────────────────────────────────┤\n");
        
        for (int i = 0; i < userArray.length; i++) {
            String user = userArray[i];
            if (user != null && user.length() > 0) {
                sb.append("│  👤 " + padRight(user, 36) + "│\n");
            }
        }
        
        sb.append("├────────────────────────────────────────┤\n");
        sb.append("│  Total: " + padRight(CustomStringUtil.customIntegerToString(userArray.length), 37) + "│\n");
        sb.append("└────────────────────────────────────────┘");
        
        return sb.toString();
    }
}
