package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.minecraft.client.gui.screens.Screen;

// Screen manager
public abstract class ScreenManager {
    protected Screen currentScreen;
    protected Screen parentScreen;

    public ScreenManager() {
        this.currentScreen = null;
        this.parentScreen = null;
    }

    /**
     * Get the currently managed screen instance.
     */
    public Screen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * Get the parent screen for navigation.
     */
    public Screen getParentScreen() {
        return parentScreen;
    }

    /**
     * Set the parent screen for this manager's screens.
     */
    public void setParentScreen(Screen parent) {
        this.parentScreen = parent;
    }

    /**
     * Build or rebuild the screen. Subclasses must update currentScreen.
     */
    public abstract Screen buildScreen();

    /**
     * Called when the screen needs to refresh.
     */
    public Screen refreshScreen() {
        return buildScreen();
    }
}
