package net.not_thefirst.story_mode_clouds.renderer.render_system.vertex;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import net.not_thefirst.story_mode_clouds.renderer.render_system.mesh.CompiledMesh;

public final class VertexFormat {

    public enum VertexAttribute {
        POSITION,
        COLOR,
        NORMAL,
        UV0,
        UV1,
        TANGENT
    }

    public static final class Element {

        public final VertexAttribute attribute;
        public final int componentCount;
        public final int glType;
        public final boolean normalized;

        public Element(
            VertexAttribute attribute,
            int componentCount,
            int glType,
            boolean normalized
        ) {
            this.attribute = attribute;
            this.componentCount = componentCount;
            this.glType = glType;
            this.normalized = normalized;
        }

        public int sizeBytes() {
            return componentCount * glTypeSize(glType);
        }

        private static int glTypeSize(int glType) {
            return switch (glType) {
                case GL11.GL_FLOAT -> Float.BYTES;
                case GL11.GL_UNSIGNED_BYTE, GL11.GL_BYTE -> Byte.BYTES;
                case GL11.GL_UNSIGNED_SHORT, GL11.GL_SHORT -> Short.BYTES;
                case GL11.GL_INT -> Integer.BYTES;
                default -> throw new IllegalArgumentException(
                    "Unsupported GL type: " + glType
                );
            };
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Element e)) return false;
            return attribute == e.attribute &&
                   componentCount == e.componentCount &&
                   glType == e.glType &&
                   normalized == e.normalized;
        }

        @Override
        public int hashCode() {
            return Objects.hash(attribute, componentCount, glType, normalized);
        }
    }

    private final List<Element> elements;
    private final int strideBytes;

    private final int[] offsets;
    private final int[] locations;

    public VertexFormat(List<Element> elements) {
        this.elements = List.copyOf(elements);

        this.offsets = new int[elements.size()];
        this.locations = new int[elements.size()];

        int offset = 0;
        for (int i = 0; i < elements.size(); i++) {
            offsets[i] = offset;
            locations[i] = i;
            offset += elements.get(i).sizeBytes();
        }

        this.strideBytes = offset;
    }

    public int strideBytes() {
        return strideBytes;
    }

    public List<Element> elements() {
        return elements;
    }


    public void enable() {
        for (int i = 0; i < elements.size(); i++) {
            Element e = elements.get(i);

            GL20.glEnableVertexAttribArray(locations[i]);
            GL20.glVertexAttribPointer(
                locations[i],
                e.componentCount,
                e.glType,
                e.normalized,
                strideBytes,
                offsets[i]
            );
        }
    }

    public void disable() {
        for (int loc : locations) {
            GL20.glDisableVertexAttribArray(loc);
        }
    }

    public void putVertex(
        ByteBuffer buf,
        int vertexIndex,
        CompiledMesh mesh,
        float offX, float offY, float offZ
    ) {
        float[] positions = mesh.positions();
        float[] normals   = mesh.normals();
        float[] uvs       = mesh.uvs();
        int[]   colors    = mesh.colors();
        for (Element e : elements) {
            switch (e.attribute) {
                case POSITION -> {
                    int p = vertexIndex * 3;
                    buf.putFloat(positions[p]     + offX);
                    buf.putFloat(positions[p + 1] + offY);
                    buf.putFloat(positions[p + 2] + offZ);
                }
                case NORMAL -> {
                    int p = vertexIndex * 3;
                    buf.putFloat(normals[p]);
                    buf.putFloat(normals[p + 1]);
                    buf.putFloat(normals[p + 2]);
                }
                case UV0 -> {
                    int p = vertexIndex * 2;
                    buf.putFloat(uvs[p]);
                    buf.putFloat(uvs[p + 1]);
                }
                case COLOR -> {
                    int c = colors[vertexIndex];
                    buf.put((byte)((c >> 16) & 0xFF)); // R
                    buf.put((byte)((c >> 8) & 0xFF));  // G
                    buf.put((byte)(c & 0xFF));         // B
                    buf.put((byte)((c >> 24) & 0xFF)); // A
                }
                default -> throw new IllegalStateException(
                    "Unhandled attribute: " + e.attribute
                );
            }
        }
    }

    public static final Element POSITION =
        new Element(VertexAttribute.POSITION, 3, GL11.GL_FLOAT, false);

    public static final Element COLOR =
        new Element(VertexAttribute.COLOR, 4, GL11.GL_UNSIGNED_BYTE, true);

    public static final Element NORMAL =
        new Element(VertexAttribute.NORMAL, 3, GL11.GL_FLOAT, true);

    public static final Element UV0 =
        new Element(VertexAttribute.UV0, 2, GL11.GL_FLOAT, false);

    public static final Element UV1 =
        new Element(VertexAttribute.UV1, 2, GL11.GL_FLOAT, false);

    public static final VertexFormat POSITION_COLOR =
        new VertexFormat(List.of(POSITION, COLOR));

    public static final VertexFormat POSITION_COLOR_TEX =
        new VertexFormat(List.of(POSITION, COLOR, UV0));

    public static final VertexFormat POSITION_COLOR_NORMAL =
        new VertexFormat(List.of(POSITION, COLOR, NORMAL));

    public static final VertexFormat POSITION_COLOR_NORMAL_TEX =
        new VertexFormat(List.of(POSITION, COLOR, NORMAL, UV0));
}
