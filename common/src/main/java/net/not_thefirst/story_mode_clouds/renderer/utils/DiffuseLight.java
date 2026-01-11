package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.math.Vector3f;

public class DiffuseLight {
    public Vector3f location;
    public Vector3f color = new Vector3f(1, 1, 1); // no intentions for color at the moment, for future support
    public float intensity;
    
    public DiffuseLight(Vector3f location, float intensity) {
        this.location = location;
        this.intensity = intensity;
    }

    public DiffuseLight normalized() {
        Vector3f normDir = new Vector3f(location.x(), location.y(), location.z());
        normDir.normalize();
        return new DiffuseLight(normDir, intensity);
    }

    public DiffuseLight() {
        this.location = new Vector3f(0.0f, 500.0f, 0.0f);
        this.intensity = 1.0f;
    }

    public void evaluate(long timeTicks, float[] outPos) {
        float t = (timeTicks % 24000L) / 24000.0f;
        float elevation = (t - 0.25f) * (float)Math.PI;

        float sinE = (float)Math.sin(elevation);
        float cosE = (float)Math.cos(elevation);

        float lx = this.location.x();
        float lz = this.location.z();

        float horizLenSq = lx * lx + lz * lz;

        float ax, az;
        if (horizLenSq < 1.0e-6f) {
            ax = 1.0f;
            az = 0.0f;
        } else {
            float invLen = 1.0f / (float)Math.sqrt(horizLenSq);
            ax = lx * invLen;
            az = lz * invLen;
        }

        float radius = this.location.y();

        outPos[0] = ax * cosE * radius;
        outPos[1] = sinE * radius;
        outPos[2] = az * cosE * radius;
    }
}