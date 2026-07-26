package net.not_thefirst.story_mode_clouds.renderer.platform.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;

import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.RenderParams;
import net.not_thefirst.lib.gl_render_system.mesh.utils.GLPrimitive;
import net.not_thefirst.lib.gl_render_system.vertex.VertexFormat;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;

public class B3DMesh extends AbstractStaticMesh<B3DRenderPass> {

    private final GpuBuffer meshData;
    private final int indexCount;
    private final int vertexCount;

    B3DMesh(
        final GLPrimitive primitive,
        final VertexFormat format,
        final int vertexCount,
        final int indexCount,
        GpuBuffer data)  {
        super(primitive, format);

        this.indexCount = indexCount;
        this.vertexCount = vertexCount;
        this.meshData = data;
    }

    @Override
    public void setup(final B3DRenderPass pass) {
        pass.getPass().setVertexBuffer(0, this.meshData.slice());
    }

    @Override
    public void draw(B3DRenderPass pass, RenderParams params) {
        pass.getPass()
            .drawIndexed(
                this.indexCount, 
                params.instanceCount(), 
                params.firstIndex(), 
                params.vertexOffset(), 
                params.firstInstance()
            );
    }

    @Override
    public void cleanup(final B3DRenderPass pass) {
        // no-op
    }

    @Override
    public void close() {
        if (meshData.isClosed()) {
            throw new IllegalStateException("Attempted to close a closed mesh");
        }
        meshData.close();
        LoggerProvider.get().warn("Mesh closed");
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public int getIndexCount() {
        return indexCount;
    }
 
    public static final class Builder implements AbstractStaticMesh.Builder<Builder, B3DMesh> {

        private BufferBuilder buildingMesh;
        private ByteBufferBuilder backBuffer;
        private final VertexFormat format;
        private final GLPrimitive primitive;

        public Builder(VertexFormat format, GLPrimitive primitive) {
            this.primitive = primitive;

            backBuffer = new ByteBufferBuilder(1024 * format.strideBytes());
            this.buildingMesh = new BufferBuilder(
                backBuffer, 
                B3DConversions.toPrimitiveTopology(primitive), 
                B3DConversions.toVanillaVertexFormat(format));

            this.format = format;
        }

        @Override
        public Builder addVertex(float x, float y, float z) {
            buildingMesh.addVertex(x, y, z);
            return this;
        }

        @Override
        public Builder setColor(int r, int g, int b, int a) {
            buildingMesh.setColor(r, g, b, a);
            return this;
        }

        @Override
        public Builder setColor(int color) {
            buildingMesh.setColor(color);
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
        public Builder setLineWidth(float width) {
            return this;
        }

        @Override
        public B3DMesh build() {
            try (MeshData data = buildingMesh.buildOrThrow()) {
                int indexCount = data.drawState().indexCount();
                int vertexCount = data.drawState().vertexCount();
                return new B3DMesh(primitive, format, vertexCount, indexCount, RenderSystem.getDevice().createBuffer(
                    () -> "Mesher",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    data.vertexBuffer()
                ));
            }
            catch (Exception e) {
                LoggerProvider.get().error("Exception building mesh: {}", e);
                return null;
            }
            finally {
                backBuffer.close();
            }
        }
    }
}
