package net.not_thefirst.story_mode_clouds.utils.glfw;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ClipboardUtils {
    private static final ByteBuffer BUFFER = BufferUtils.createByteBuffer(8192);

    /**
     * Reads plain text from the system clipboard using the provided window handle.
     * Returns an empty string if the clipboard is empty or invalid.
     * @param windowHandle The GLFW window handle to use for clipboard access.
     * @return The text from the clipboard or an empty string.
     */
    public static String getClipboard(long windowHandle) {
        if (windowHandle == 0L) return "";
        
        String clipboardText = GLFW.glfwGetClipboardString(windowHandle);
        return clipboardText != null ? clipboardText : "";
    }

    /**
     * Writes a text string to the system clipboard using the provided window handle.
     * Uses a fast scratch buffer for short strings, or direct memory for large ones.
     * @param windowHandle The GLFW window handle to use for clipboard access.
     * @param text The text to set in the clipboard. Null is treated as an empty string.
     */
    public static void setClipboard(long windowHandle, String text) {
        if (windowHandle == 0L) return;
        if (text == null) text = "";

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        int totalLength = bytes.length + 1;

        if (totalLength < BUFFER.capacity()) {
            synchronized (BUFFER) {
                writeToGlfw(windowHandle, BUFFER, bytes);
            }
        } else {
            ByteBuffer dynamicBuffer = MemoryUtil.memAlloc(totalLength);
            try {
                writeToGlfw(windowHandle, dynamicBuffer, bytes);
            } finally {
                MemoryUtil.memFree(dynamicBuffer);
            }
        }
    }


    private static void writeToGlfw(long windowHandle, ByteBuffer buffer, byte[] bytes) {
        buffer.clear();
        buffer.put(bytes);
        buffer.put((byte) 0); // null terminator  
        buffer.flip();
        GLFW.glfwSetClipboardString(windowHandle, buffer);
    }
}
