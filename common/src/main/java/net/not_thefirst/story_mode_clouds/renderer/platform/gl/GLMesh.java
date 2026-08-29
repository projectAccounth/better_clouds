package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.RenderParams;
import net.not_thefirst.lib.gl_render_system.mesh.GpuMesh;
import net.not_thefirst.lib.gl_render_system.mesh.IndexedBuildingMesh;
import net.not_thefirst.lib.gl_render_system.mesh.IndexedCompiledMesh;
import net.not_thefirst.lib.gl_render_system.mesh.MeshUploader;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.vertex.InstanceFormat;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.lib.utils.math.ARGB;

public class GLMesh extends AbstractStaticMesh<GLRenderPass> {

    private GpuMesh meshData;
    private int vertexCount;

    GLMesh(GpuMesh meshData, GLPrimitive prim, VertexFormat format, int vertexCount) {
        super(prim, format);
        this.meshData = meshData;
        this.vertexCount = vertexCount;

    }

    @Override
    public void setup(final GLRenderPass pass) {
        // no-op
    }

    @Override
    public void draw(GLRenderPass pass, RenderParams params) {
        this.meshData.drawInstanced(params.instanceCount());
    }

    @Override
    public void cleanup(final GLRenderPass pass) {
        //
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public void close() {
        this.meshData.close();
    }

    public static final class Builder implements AbstractStaticMesh.Builder<Builder, GLMesh> {

        private final IndexedBuildingMesh buildingMesh;
        private final VertexFormat format;
        private final GLPrimitive primitive;

        public Builder(VertexFormat format, InstanceFormat instanceFormat, GLPrimitive primitive) {
            this.primitive = primitive;
            this.buildingMesh = new IndexedBuildingMesh(format, instanceFormat, primitive);
            this.format = format;
        }

        public Builder(VertexFormat format, GLPrimitive primitive) {
            this(format, InstanceFormat.NONE, primitive);
        }

        @Override
        public Builder addVertex(float x, float y, float z) {
            buildingMesh.addVertex(x, y, z);
            return this;
        }

        @Override
        public Builder setColor(int r, int g, int b, int a) {
            buildingMesh.setColor(r / 255.0f, g / 255.0f, b / 255.0f, a / 255.0f);
            return this;
        }

        @Override
        public Builder setColor(int color) {
            buildingMesh.setColor(ARGB.fromRGBA(color));
            return this;
        }

        @Override
        public Builder setUv(float u, float v) {
            buildingMesh.setUv(u, v);
            return this;
        }

        @Override
        public Builder setUv1(int u, int v) {
            throw new UnsupportedOperationException("Lightmap is not supported for OpenGL pipelines");
        }

        @Override
        public Builder setUv2(int u, int v) {
            throw new UnsupportedOperationException("UV2 is not supported for OpenGL pipelines");
        }

        @Override
        public Builder setNormal(float x, float y, float z) {
            buildingMesh.setNormal(x, y, z);
            return this;
        }

        @Override
        public GLMesh build() {
            IndexedCompiledMesh compiled = buildingMesh.compile();
            GpuMesh uploaded = MeshUploader.uploadIndexed(compiled);

            return new GLMesh(
                uploaded, 
                this.primitive,
                format,
                compiled.vertexCount());
        }
    }
}
