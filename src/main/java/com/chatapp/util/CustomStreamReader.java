package com.chatapp.util;

import java.io.IOException;
import java.io.InputStream;

public class CustomStreamReader implements java.io.Closeable {
    private final InputStream in;
    private final byte[] buffer;
    private int bufferPos;
    private int bufferCount;

    public CustomStreamReader(InputStream in) {
        this.in = in;
        this.buffer = new byte[2048];
        this.bufferPos = 0;
        this.bufferCount = 0;
    }

    private int fillBuffer() throws IOException {
        bufferPos = 0;
        bufferCount = in.read(buffer, 0, buffer.length);
        return bufferCount;
    }

    public String readLine() throws IOException {
        CustomStringUtil.CustomStringBuilder sb = null;
        
        while (true) {
            if (bufferPos >= bufferCount) {
                if (fillBuffer() <= 0) {
                    // EOF reached
                    if (sb != null && sb.toString().length() > 0) {
                        return sb.toString();
                    }
                    return null;
                }
            }
            
            // Look for newline
            int start = bufferPos;
            int end = -1;
            boolean hasCarriageReturn = false;
            
            for (int i = bufferPos; i < bufferCount; i++) {
                byte b = buffer[i];
                if (b == '\n') {
                    end = i;
                    break;
                } else if (b == '\r') {
                    // Carriage return could be followed by \n. We handle it.
                    end = i;
                    hasCarriageReturn = true;
                    break;
                }
            }
            
            if (end != -1) {
                // Found delimiter in current buffer
                int chunkLen = end - start;
                String chunk = CustomStringUtil.customCreateString(buffer, start, chunkLen);
                bufferPos = end + 1;
                
                if (hasCarriageReturn) {
                    // Peek next byte to see if it's \n
                    if (bufferPos >= bufferCount) {
                        fillBuffer();
                    }
                    if (bufferPos < bufferCount && buffer[bufferPos] == '\n') {
                        bufferPos++;
                    }
                }
                
                if (sb == null) {
                    return chunk;
                } else {
                    sb.append(chunk);
                    return sb.toString();
                }
            } else {
                // Delimiter not found in current buffer, consume all buffer
                int chunkLen = bufferCount - start;
                String chunk = CustomStringUtil.customCreateString(buffer, start, chunkLen);
                bufferPos = bufferCount;
                
                if (sb == null) {
                    sb = new CustomStringUtil.CustomStringBuilder();
                }
                sb.append(chunk);
            }
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
