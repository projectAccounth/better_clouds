package net.not_thefirst.story_mode_clouds.utils.interp.world;

import java.util.ArrayList;
import java.util.List;

// Simple cyclic number sequence interpolator
public class NumberSequence {
    public static class NumberSequenceKeypoint {
        double time;
        double[] values;

        public NumberSequenceKeypoint(double time, double... values) {
            this.time = time;
            this.values = values;
        }
    }

    private List<NumberSequenceKeypoint> keypoints = new ArrayList<>();
    private double duration = 1.0;

    public NumberSequence(double duration) {
        this.duration = duration;
    }

    public void addKeypoint(double time, double... values) {
        keypoints.add(new NumberSequenceKeypoint(time, values));
        keypoints.sort((a, b) -> Double.compare(a.time, b.time));
    }

    public void clearKeypoints() {
        keypoints.clear();
    }

    public void removeKeypoint(int index) {
        keypoints.remove(index);
    }

    public void removeKeypoint(NumberSequenceKeypoint keypoint) {
        keypoints.remove(keypoint);
    }

    public void removeAllKeypointWithValue(double... values) {
        keypoints.removeIf(kp -> {
            if (kp.values.length != values.length) return false;
            for (int i = 0; i < values.length; i++) {
                if (kp.values[i] != values[i]) return false;
            }
            return true;
        });
    }

    public void removeAllKeypointAtTime(double time) {
        keypoints.removeIf(kp -> kp.time == time);
    }

    public List<NumberSequenceKeypoint> getKeypoints() {
        return keypoints;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public int getKeypointCount() {
        return keypoints.size();
    }

    public double[] evaluate(double t) {
        if (keypoints.isEmpty()) return new double[0];
        int dim = keypoints.get(0).values.length;

        t = t % duration;
        if (t < 0) t += duration;

        if (t <= keypoints.get(0).time) return keypoints.get(0).values;
        if (t >= keypoints.get(keypoints.size() - 1).time) return keypoints.get(keypoints.size() - 1).values;

        NumberSequenceKeypoint prev = keypoints.get(0);
        for (NumberSequenceKeypoint next : keypoints) {
            if (t <= next.time) {
                double factor = (t - prev.time) / (next.time - prev.time);
                double[] result = new double[dim];
                for (int i = 0; i < dim; i++) {
                    result[i] = prev.values[i] + (next.values[i] - prev.values[i]) * factor;
                }
                return result;
            }
            prev = next;
        }

        return new double[dim]; // fallback
    }
}