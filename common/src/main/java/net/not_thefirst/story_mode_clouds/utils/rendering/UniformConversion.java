package net.not_thefirst.story_mode_clouds.utils.rendering;

public class UniformConversion {
    private UniformConversion() {}

    public static int[] convertToIntArray(UniformValue uniformValue) {
        if (uniformValue.type() == UniformType.INT1) {
            return new int[]{(int) uniformValue.value()};
        } else if (uniformValue.type() == UniformType.INT2) {
            return (int[]) uniformValue.value();
        } else if (uniformValue.type() == UniformType.INT3) {
            return (int[]) uniformValue.value();
        } else if (uniformValue.type() == UniformType.INT4) {
            return (int[]) uniformValue.value();
        } else {
            throw new IllegalArgumentException("Cannot convert non-integer uniform value to int array");
        }
    }

    public static float[] convertToFloatArray(UniformValue uniformValue) {
        if (uniformValue.type() == UniformType.FLOAT1) {
            return new float[]{(float) uniformValue.value()};
        } else if (uniformValue.type() == UniformType.FLOAT2) {
            return (float[]) uniformValue.value();
        } else if (uniformValue.type() == UniformType.FLOAT3) {
            return (float[]) uniformValue.value();
        } else if (uniformValue.type() == UniformType.FLOAT4) {
            return (float[]) uniformValue.value();
        } else if (uniformValue.type() == UniformType.MAT3) {
            return (float[]) uniformValue.value();
        } else if (uniformValue.type() == UniformType.MAT4) {
            return (float[]) uniformValue.value();
        } else {
            throw new IllegalArgumentException("Cannot convert non-float uniform value to float array");
        }
    }
}
