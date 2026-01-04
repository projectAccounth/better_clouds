package net.not_thefirst.story_mode_clouds.renderer.utils;

import org.joml.Vector3f;

public class DiffuseLight {
    public Vector3f direction;
    public float intensity;
    
    public DiffuseLight(Vector3f direction, float intensity) {
        this.direction = direction;
        this.intensity = intensity;
    }

    public DiffuseLight normalized() {
        Vector3f normDir = new Vector3f(direction).normalize();
        return new DiffuseLight(normDir, intensity);
    }

    public DiffuseLight() {
        this.direction = new Vector3f(0.0f, -1.0f, 0.0f);
        this.intensity = 1.0f;
    }
}