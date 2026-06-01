package net.not_thefirst.story_mode_clouds.config.presets;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;

/**
 * Base interface for all category preset types.
 * Provides common methods for all preset implementations.
 */
public interface Preset {
    
    /**
     * Get the unique identifier for this preset
     */
    String getId();
    
    /**
     * Get the human-readable display name
     */
    String getDisplayName();
    
    /**
     * Get the description of this preset
     */
    String getDescription();
    
    /**
     * Get the timestamp when this preset was last modified
     */
    long getLastModified();
    
    /**
     * Apply this preset's settings to the given configuration
     */
    void applyTo(CloudsConfiguration config);
    
    /**
     * Create a copy of this preset with a new ID and display name
     */
    Preset copy(String newId, String newDisplayName);
}
