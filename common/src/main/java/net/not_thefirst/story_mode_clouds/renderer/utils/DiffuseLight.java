package net.not_thefirst.story_mode_clouds.renderer.utils;

import com.mojang.math.Vector3f;

public class DiffuseLight {
    private float locationX;
    private float locationY;
    private float locationZ;

    private Vector3f color = new Vector3f(1, 1, 1); // no intentions for color at the moment, for future support

    private float intensity;
    private boolean useAzimuthPlane = false;
    
    public DiffuseLight(Vector3f location, float intensity) {
        this.locationX = location.x();
        this.locationY = location.y();
        this.locationZ = location.z();
        this.intensity = intensity;
    }

    public DiffuseLight normalized() {
        Vector3f normDir = new Vector3f(locationX, locationY, locationZ);
        normDir.normalize();
        return new DiffuseLight(normDir, intensity);
    }

    public DiffuseLight() {
        this(new Vector3f(0.0f, 500.0f, 0.0f), 1.0f);
    }

    public Vector3f location() { return new Vector3f(locationX, locationY, locationZ); }
    public Vector3f color() { return this.color; }
    public float intensity() { return this.intensity; }
    public boolean usesAzimuth() { return this.useAzimuthPlane; }

    public void setLocation(Vector3f newLocation) {
        this.locationX = newLocation.x();
        this.locationY = newLocation.y();
        this.locationZ = newLocation.z();
    }

    public void setXLocation(float newXPos) {
        this.locationX = newXPos;
    }

    public void setYLocation(float newYPos) {
        this.locationY = newYPos;
    }

    public void setZLocation(float newZPos) {
        this.locationZ = newZPos;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public void setAzimuthState(boolean newState) {
        this.useAzimuthPlane = newState;
    }

    public void evaluate(long timeTicks, float[] outPos) {
        float t = (timeTicks % 24000L) / 24000.0f;

        // Full day rotation
        float angle = t * (float)Math.PI * 2.0f;
        float sinE = (float)Math.sin(angle);
        float cosE = (float)Math.cos(angle);

        float centerX = this.locationX;
        float centerY = this.locationY;
        float centerZ = this.locationZ;

        float ax = 1.0f;
        float az = 0.0f;

        if (useAzimuthPlane) {
            float horizX = this.locationX;
            float horizZ = this.locationZ;
            float horizLenSq = horizX * horizX + horizZ * horizZ;

            if (horizLenSq > 1e-6f) {
                float invLen = 1.0f / (float)Math.sqrt(horizLenSq);
                ax = horizX * invLen;
                az = horizZ * invLen;
            }
        }

        // Pos on sky dome
        outPos[0] = centerX + ax * centerY * cosE;
        outPos[1] = centerY * sinE;
        outPos[2] = centerZ + az * centerY * cosE;
    }
}