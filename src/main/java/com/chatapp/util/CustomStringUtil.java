package com.chatapp.util;

public class CustomStringUtil {

    public static String customSubstring(String s, int start, int end) {
        if (s == null) return null;
        if (start < 0) start = 0;
        if (end > s.length()) end = s.length();
        if (start > end) return "";
        
        int len = end - start;
        char[] chars = new char[len];
        for (int i = 0; i < len; i++) {
            chars[i] = s.charAt(start + i);
        }
        return new String(chars);
    }

    public static String customSubstringFrom(String s, int start) {
        if (s == null) return null;
        return customSubstring(s, start, s.length());
    }

    public static boolean customStartsWith(String s, String prefix) {
        if (s == null || prefix == null) return false;
        if (s.length() < prefix.length()) return false;
        for (int i = 0; i < prefix.length(); i++) {
            if (s.charAt(i) != prefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean customEqualsIgnoreCase(String s1, String s2) {
        if (s1 == s2) return true;
        if (s1 == null || s2 == null) return false;
        if (s1.length() != s2.length()) return false;
        for (int i = 0; i < s1.length(); i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            if (charToLowerCase(c1) != charToLowerCase(c2)) {
                return false;
            }
        }
        return true;
    }

    public static char charToLowerCase(char c) {
        if (c >= 'A' && c <= 'Z') {
            return (char) (c + 32);
        }
        return c;
    }

    public static String customToLowerCase(String s) {
        if (s == null) return null;
        char[] chars = new char[s.length()];
        for (int i = 0; i < s.length(); i++) {
            chars[i] = charToLowerCase(s.charAt(i));
        }
        return new String(chars);
    }

    public static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n';
    }

    public static String customTrim(String s) {
        if (s == null) return null;
        int len = s.length();
        int start = 0;
        while (start < len && isWhitespace(s.charAt(start))) {
            start++;
        }
        int end = len;
        while (end > start && isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return customSubstring(s, start, end);
    }

    public static String[] customSplit(String s, char delimiter, int limit) {
        if (s == null) return new String[0];
        
        // First pass: count splits
        int occurrences = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == delimiter) {
                occurrences++;
                if (limit > 0 && occurrences >= limit - 1) {
                    break;
                }
            }
        }
        
        String[] result = new String[occurrences + 1];
        int resultIndex = 0;
        int start = 0;
        
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == delimiter) {
                if (limit > 0 && resultIndex >= limit - 1) {
                    break;
                }
                result[resultIndex++] = customSubstring(s, start, i);
                start = i + 1;
            }
        }
        result[resultIndex] = customSubstringFrom(s, start);
        return result;
    }

    public static String customJoin(String delimiter, String[] elements, int count) {
        if (elements == null || count <= 0) return "";
        CustomStringBuilder sb = new CustomStringBuilder();
        for (int i = 0; i < count; i++) {
            if (elements[i] != null) {
                sb.append(elements[i]);
            }
            if (i < count - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static int customParseInt(String s) {
        if (s == null) throw new NumberFormatException("null");
        s = customTrim(s);
        if (s.length() == 0) throw new NumberFormatException("empty string");
        
        int i = 0;
        boolean negative = false;
        char firstChar = s.charAt(0);
        if (firstChar == '-') {
            negative = true;
            i++;
        } else if (firstChar == '+') {
            i++;
        }
        
        int result = 0;
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c < '0' || c > '9') {
                throw new NumberFormatException("Invalid character: " + c);
            }
            int digit = c - '0';
            // Simple overflow check
            if (result > Integer.MAX_VALUE / 10 || (result == Integer.MAX_VALUE / 10 && digit > 7)) {
                if (negative && result == Integer.MAX_VALUE / 10 && digit == 8) {
                    return Integer.MIN_VALUE;
                }
                throw new NumberFormatException("Integer overflow");
            }
            result = result * 10 + digit;
        }
        return negative ? -result : result;
    }

    public static String customIntegerToString(int n) {
        if (n == 0) return "0";
        if (n == Integer.MIN_VALUE) return "-2147483648";
        
        boolean negative = n < 0;
        if (negative) {
            n = -n;
        }
        
        char[] temp = new char[12];
        int idx = 0;
        while (n > 0) {
            temp[idx++] = (char) ('0' + (n % 10));
            n /= 10;
        }
        
        int totalLen = idx + (negative ? 1 : 0);
        char[] chars = new char[totalLen];
        int writeIdx = 0;
        if (negative) {
            chars[writeIdx++] = '-';
        }
        for (int i = idx - 1; i >= 0; i--) {
            chars[writeIdx++] = temp[i];
        }
        return new String(chars);
    }

    public static String customFormat(String template, Object... args) {
        if (template == null) return "";
        CustomStringBuilder sb = new CustomStringBuilder();
        int argIndex = 0;
        int len = template.length();
        for (int i = 0; i < len; i++) {
            if (i < len - 1 && template.charAt(i) == '%' && (template.charAt(i + 1) == 's' || template.charAt(i + 1) == 'd')) {
                if (args != null && argIndex < args.length) {
                    Object arg = args[argIndex++];
                    if (arg == null) {
                        sb.append("null");
                    } else if (arg instanceof Integer) {
                        sb.append(customIntegerToString((Integer) arg));
                    } else {
                        sb.append(arg.toString());
                    }
                } else {
                    sb.append("%unknown");
                }
                i++; // Skip the 's' or 'd'
            } else {
                sb.append(template.charAt(i));
            }
        }
        return sb.toString();
    }

    public static byte[] customGetBytes(String s) {
        if (s == null) return null;
        int len = s.length();
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) s.charAt(i);
        }
        return bytes;
    }

    public static String customCreateString(byte[] bytes, int offset, int length) {
        if (bytes == null) return null;
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (bytes[offset + i] & 0xFF);
        }
        return new String(chars);
    }

    public static class CustomStringBuilder {
        private char[] buffer;
        private int length;

        public CustomStringBuilder() {
            this.buffer = new char[16];
            this.length = 0;
        }

        private void ensureCapacity(int minCapacity) {
            if (minCapacity > buffer.length) {
                int newCapacity = buffer.length * 2;
                if (newCapacity < minCapacity) {
                    newCapacity = minCapacity;
                }
                char[] newBuffer = new char[newCapacity];
                for (int i = 0; i < length; i++) {
                    newBuffer[i] = buffer[i];
                }
                buffer = newBuffer;
            }
        }

        public void append(char c) {
            ensureCapacity(length + 1);
            buffer[length++] = c;
        }

        public void append(String s) {
            if (s == null) s = "null";
            int len = s.length();
            ensureCapacity(length + len);
            for (int i = 0; i < len; i++) {
                buffer[length++] = s.charAt(i);
            }
        }

        @Override
        public String toString() {
            return new String(buffer, 0, length);
        }
    }
}
