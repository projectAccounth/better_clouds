package net.not_thefirst.story_mode_clouds.utils.rendering.blaze3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;

import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.RenderParams;
import net.not_thefirst.story_mode_clouds.utils.logging.LoggerProvider;
import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;

public class B3DRenderPass extends AbstractRenderPass<B3DPipeline> {

    private final RenderPass pass;
    private final Map<String, GpuBuffer> uniformBlocks = new HashMap<>();

    private int instanceCount;
    public B3DRenderPass(String name, B3DPipeline... pipelines) {
        super(name, pipelines);

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        RenderTarget rt = ClientHelper.GameRendererHelper.getRenderer().mainRenderTarget();
        GpuTextureView colorTex;
        GpuTextureView depthTex;
        
        colorTex = rt.getColorTextureView();
        depthTex = rt.getDepthTextureView();

        pass = encoder.createRenderPass(
            () -> name,
            colorTex,
            Optional.empty(),
            depthTex,
            OptionalDouble.empty() 
        );
    }

    @Override
    public void setup() {
        // nothing at the moment 
    }

    public RenderPass getPass() {
        return this.pass;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void validateBeforeRender() {
        if (pipelines.isEmpty())
            throw new IllegalStateException("Rendering pass with no pipelines, pass name: " + name);
    }

    @Override
    public void render() {
        validateBeforeRender();

        GpuBufferSlice slice = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrixCopy());
        
        try {
            RenderSystem.bindDefaultUniforms(pass);

            pass.setUniform("DynamicTransforms", slice);
            bindPipelineUBOs();

            ((B3DMesh) meshData).setup(this);

            RenderSystem.AutoStorageIndexBuffer indices =
                RenderSystem.getSequentialBuffer(this.pipelines.get(0).getPipeline().getPrimitiveTopology());

            GpuBuffer indexBuf = indices.getBuffer(indexCount);
            pass.setIndexBuffer(indexBuf, indices.type());

            for (B3DPipeline pipeline : this.pipelines) {
                RenderPipeline vanillaPipeline = pipeline.getPipeline();
                
                pass.setPipeline(vanillaPipeline);
                ((B3DMesh) meshData).draw(this, new RenderParams(1, 0, 0, 0));
            }
        }
        catch (Exception exception) {
            LoggerProvider.get().error("Error while rendering: ", exception);
        }
    }

    private void bindPipelineUBOs() {
        for (var entry : uniformBlocks.entrySet()) {
            String name = entry.getKey();
            GpuBuffer buffer = entry.getValue();

            pass.setUniform(name, buffer);
            // LoggerProvider.get().info("Bound block {}", name);
        }
    }

    @Override
    public void cleanup() {
        if (pass != null) {
            pass.close();
        }
    }

    @Override
    public void bindUniformBlock(String name, AbstractUBODataBuffer<?, ?> buffer) {
        if (!(buffer instanceof B3DUBODataBuffer b3dbuf)) {
            throw new IllegalArgumentException(
                "UBO must be an instance of B3DUBODataBuffer"
            );
        }

        uniformBlocks.put(name, b3dbuf.build());
    }

    @Override
    public void bindTexture(String name, int textureId, int slot) {
        throw new UnsupportedOperationException("Unimplemented method 'bindTexture'");
    }

    @Override
    public void bindTextureArray(String name, int textureId, int slot) {
        throw new UnsupportedOperationException("Unimplemented method 'bindTextureArray'");
    }

    @Override
    public void setMesh(AbstractStaticMesh<?> mesh, int indexCount) {
        if (mesh == null) {
            throw new IllegalArgumentException("Mesh cannot be null");
        }
        if (!(mesh instanceof B3DMesh)) {
            throw new IllegalArgumentException("Mesh must be a B3DMesh instance");
        }
        this.meshData = mesh;
        this.indexCount = mesh.getIndexCount();
    }
}
