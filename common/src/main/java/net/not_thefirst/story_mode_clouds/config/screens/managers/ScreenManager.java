package net.not_thefirst.story_mode_clouds.config.screens.managers;

import net.minecraft.client.gui.screens.Screen;
import net.not_thefirst.story_mode_clouds.config.BackendHolder;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

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

    public static Screen buildDeleteConfirmationScreen(String targetName, Screen returnScreen, Runnable deleteAction) {
        var backend = BackendHolder.getBackend();
        var screenBuilder = backend.createScreen("cloudtweaks.confirm_delete.title", () -> {});
        var category = backend.createCategory("cloudtweaks.confirm_delete.category", null)
            .option(backend.stringOption(
                "cloudtweaks.confirm_delete.target",
                "cloudtweaks.confirm_delete.description",
                () -> targetName,
                value -> {}
            ));

        category.action(backend.createAction(
            "cloudtweaks.confirm_delete.confirm",
            "cloudtweaks.confirm_delete.confirm.tooltip",
            () -> {
                deleteAction.run();
            }
        ));
        category.action(backend.createAction(
            "cloudtweaks.confirm_delete.cancel",
            "cloudtweaks.confirm_delete.cancel.tooltip",
            () -> ClientHelper.setScreen(returnScreen)
        ));

        screenBuilder.category(category);
        return screenBuilder.build(returnScreen);
    }
}
