package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.math.Vector3f;

public class DiffuseLight {

    private float directionX;
    private float directionY;
    private float directionZ;

    private Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);

    private float intensity;
    private boolean useAzimuthPlane = false;

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
        float x = newDirection.x();
        float y = newDirection.y();
        float z = newDirection.z();

        float lenSq = x * x + y * y + z * z;
        if (lenSq > 1e-6f) {
            float invLen = 1.0f / (float)Math.sqrt(lenSq);
            this.directionX = x * invLen;
            this.directionY = y * invLen;
            this.directionZ = z * invLen;
        }
    }

    public void setXDirection(float x) {
        setDirection(new Vector3f(x, directionY, directionZ));
    }

    public void setYDirection(float y) {
        setDirection(new Vector3f(directionX, y, directionZ));
    }

    public void setZDirection(float z) {
        setDirection(new Vector3f(directionX, directionY, z));
    }

    public float getXDirection() { return this.directionX; }
    public float getYDirection() { return this.directionX; }
    public float getZDirection() { return this.directionX; }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public void setAzimuthState(boolean newState) {
        this.useAzimuthPlane = newState;
    }

    public void evaluate(long timeTicks, float[] outDir) {
        float t = (timeTicks % 24000L) / 24000.0f;

        float angle = t * (float)Math.PI * 2.0f;
        float sinE = (float)Math.sin(angle);
        float cosE = (float)Math.cos(angle);

        float ax = 1.0f;
        float az = 0.0f;

        if (useAzimuthPlane) {
            float horizX = directionX;
            float horizZ = directionZ;
            float horizLenSq = horizX * horizX + horizZ * horizZ;

            if (horizLenSq > 1e-6f) {
                float invLen = 1.0f / (float)Math.sqrt(horizLenSq);
                ax = horizX * invLen;
                az = horizZ * invLen;
            }
        }

        float dx = ax * cosE;
        float dy = sinE;
        float dz = az * cosE;

        float lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq > 1e-6f) {
            float invLen = 1.0f / (float)Math.sqrt(lenSq);
            outDir[0] = dx * invLen;
            outDir[1] = dy * invLen;
            outDir[2] = dz * invLen;
        }
    }
}
