package net.not_thefirst.story_mode_clouds.renderer.shader;

public final class Std140SizeCalculator {

    private int offset = 0;

    public int offset() {
        return offset;
    }

    public int align(int align) {
        offset = (offset + align - 1) & ~(align - 1);
        return offset;
    }

    public int addFloat() {
        align(4);
        offset += 4;
        return offset;
    }

    public int addInt() {
        align(4);
        offset += 4;
        return offset;
    }

    public int addVec2() {
        align(8);
        offset += 8;
        return offset;
    }

    public int addVec3() {
        align(16);
        offset += 16;
        return offset;
    }

    public int addVec4() {
        align(16);
        offset += 16;
        return offset;
    }

    public int addIVec4() {
        align(16);
        offset += 16;
        return offset;
    }

    public int addMat4() {
        // mat4 = 4 vec4 columns
        for (int i = 0; i < 4; i++) {
            addVec4();
        }
        return offset;
    }

    public int finish() {
        align(16);
        return offset;
    }
}
