package net.not_thefirst.story_mode_clouds.utils.glfw;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;

public class GLFWWindowHelper implements AutoCloseable {
    private GLFWWindowHelper() {}

    private static FramebufferSizeCallback callback;

    public static int getWidth(long windowAddr) {
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetFramebufferSize(windowAddr, width, height);
        return width[0];
    }

    public static int getHeight(long windowAddr) {
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetFramebufferSize(windowAddr, width, height);
        return height[0];
    }

    public static void chainCallback(long windowAddr, GLFWFramebufferSizeCallback cb) {
        callback = FramebufferSizeCallback.intercept(windowAddr, cb);
    }

    @Override
    public void close() {
        callback.free();
    }
}
