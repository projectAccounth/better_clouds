package net.not_thefirst.story_mode_clouds.utils.glfw;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;

public class FramebufferSizeCallback extends GLFWFramebufferSizeCallback {
    private final List<GLFWFramebufferSizeCallback> chain = new ArrayList<>();

    public static FramebufferSizeCallback intercept(long window, GLFWFramebufferSizeCallback newCallback) {
        GLFWFramebufferSizeCallback existing = GLFW.glfwSetFramebufferSizeCallback(window, null);
        
        FramebufferSizeCallback master;
        if (existing instanceof FramebufferSizeCallback) {
            master = (FramebufferSizeCallback) existing;
        } else {
            master = new FramebufferSizeCallback();
            if (existing != null) {
                master.chain.add(existing);
            }
        }
        
        if (newCallback != null) {
            master.chain.add(newCallback);
        }
        
        GLFW.glfwSetFramebufferSizeCallback(window, master);
        return master;
    }

    @Override
    public void invoke(long window, int width, int height) {
        for (int i = 0; i < chain.size(); i++) {
            chain.get(i).invoke(window, width, height);
        }
    }

    @Override
    public void free() {
        super.free();
        for (int i = 0; i < chain.size(); i++) {
            chain.get(i).free();
        }
        chain.clear();
    }
}