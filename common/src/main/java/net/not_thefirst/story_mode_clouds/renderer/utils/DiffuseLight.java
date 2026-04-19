package net.not_thefirst.story_mode_clouds.renderer.utils;

import org.joml.Vector3f;

import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration;
import net.not_thefirst.story_mode_clouds.config.CloudsConfiguration.LightingParameters;

public class DiffuseLight {

    private float directionX;
    private float directionY;
    private float directionZ;

    private Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);

    private float intensity;
    private boolean useAzimuthPlane = true;

    public DiffuseLight(Vector3f direction, float intensity) {
        setDirection(direction);
        this.intensity = intensity;
    }

    public DiffuseLight() {
        this(new Vector3f(0.0f, -1.0f, 0.0f), 1.0f);
    }

    public Vector3f direction() {
        return new Vector3f(directionX, directionY, directionZ);
    }

    public Vector3f color() {
        return this.color;
    }

    public float intensity() {
        return this.intensity;
    }

    public boolean usesAzimuth() {
        return this.useAzimuthPlane;
    }

    public void setDirection(Vector3f newDirection) {
        Vector3f normalized = newDirection.normalize();

        this.directionX = normalized.x();
        this.directionY = normalized.y();
        this.directionZ = normalized.z();
    }

    public void setXDirection(float x) {
        this.directionX = x;
    }

    public void setYDirection(float y) {
        this.directionY = y;
    }

    public void setZDirection(float z) {
        this.directionZ = z;
    }

    public float getXDirection() { return this.directionX; }
    public float getYDirection() { return this.directionY; }
    public float getZDirection() { return this.directionZ; }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public void setAzimuthState(boolean newState) {
        this.useAzimuthPlane = newState;
    }

    public void evaluate(long timeTicks, float[] outDir) {
        CloudsConfiguration.LightingParameters cfg =
            CloudsConfiguration.getInstance().LIGHTING;

        long t = Math.floorMod(timeTicks + cfg.DAY_NOON * 2, LightingParameters.DAY_LENGTH);
        float phase = (float)t / (float) LightingParameters.DAY_LENGTH;
        float angle = phase * (float)(Math.PI * 2.0);

        float dx = (float)Math.cos(angle);
        float dy = (float)Math.sin(angle);
        float dz = 0.0f;

        if (useAzimuthPlane) {
            float yaw = (float)Math.atan2(directionZ, directionX);
            float cy = (float)Math.cos(yaw);
            float sy = (float)Math.sin(yaw);

            float rx = dx * cy - dz * sy;
            float rz = dx * sy + dz * cy;

            dx = rx;
            dz = rz;
        }

        float lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq > 1e-6f) {
            float inv = 1.0f / (float)Math.sqrt(lenSq);
            outDir[0] = dx * inv;
            outDir[1] = dy * inv;
            outDir[2] = dz * inv;
        }
    }
}
