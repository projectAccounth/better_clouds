package net.not_thefirst.story_mode_clouds.renderer.render_system.shader;

public final class Std140SizeCalculator {

    private int offset = 0;

    public int offset() {
        return offset;
    }

    private void alignInternal(int align) {
        offset = (offset + align - 1) & ~(align - 1);
    }

    public Std140SizeCalculator align(int align) {
        alignInternal(align);
        return this;
    }

    public Std140SizeCalculator addFloat() {
        alignInternal(4);
        offset += 4;
        return this;
    }

    public Std140SizeCalculator addInt() {
        alignInternal(4);
        offset += 4;
        return this;
    }

    public Std140SizeCalculator addVec2() {
        alignInternal(8);
        offset += 8;
        return this;
    }

    public Std140SizeCalculator addVec3() {
        alignInternal(16);
        offset += 16;
        return this;
    }

    public Std140SizeCalculator addVec4() {
        alignInternal(16);
        offset += 16;
        return this;
    }

    public Std140SizeCalculator addIVec4() {
        alignInternal(16);
        offset += 16;
        return this;
    }

    public Std140SizeCalculator addMat4() {
        // mat4 = 4 vec4 columns
        for (int i = 0; i < 4; i++) {
            addVec4();
        }
        return this;
    }

    public Std140SizeCalculator finish() {
        alignInternal(16);
        return this;
    }

    public static int getFloatSize() {
        return 4;
    }

    public static int getIntSize() {
        return 4;
    }

    public static int getVec4Size() {
        return getFloatSize() * 4;
    }

    public static int getMat4Size() {
        return getVec4Size() * 4;
    }

    public static int getIVec4Size() {
        return getIntSize() * 4;
    }

    public static int getVec3Size() {
        return getFloatSize() * 3;
    }

    public static int getMat3Size() {
        return getVec3Size() * 3;
    }

    public static int getVec2Size() {
        return getFloatSize() * 2;
    }

    public static int getMat2Size() {
        return getVec2Size() * 2;
    }
}
