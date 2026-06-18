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

import net.not_thefirst.story_mode_clouds.utils.minecraft.ClientHelper;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractRenderPass;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractUBODataBuffer;

public class B3DRenderPass extends AbstractRenderPass<B3DPipeline> {

    private RenderPass pass;
    private final Map<String, B3DUBODataBuffer> uniformBlocks = new HashMap<>();

    private GpuBuffer vertexBuffer;
    private int instanceCount;
    private int idxCount;

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

    public void setMesh(GpuBuffer vertexBuffer, int idxCount) {
        this.vertexBuffer = vertexBuffer;
        this.idxCount = idxCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    @Override
    public void render() {
        GpuBufferSlice slice = RenderSystem.getDynamicUniforms()
			.writeTransform(RenderSystem.getModelViewMatrixCopy());

        try {
            RenderSystem.bindDefaultUniforms(pass);

            bindPipelineUBOs();
            pass.setUniform("DynamicTransforms", slice);

            pass.setVertexBuffer(0, vertexBuffer.slice());
            
            for (B3DPipeline pipeline : this.pipelines) {
                RenderPipeline vanillaPipeline = pipeline.getPipeline();
                RenderSystem.AutoStorageIndexBuffer indices =
                    RenderSystem.getSequentialBuffer(vanillaPipeline.getPrimitiveTopology());

                GpuBuffer indexBuf = indices.getBuffer(idxCount);
                pass.setIndexBuffer(indexBuf, indices.type());

                pass.setPipeline(vanillaPipeline);
                pass.drawIndexed(idxCount, instanceCount, 0, 0, 0);
            }
        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void bindPipelineUBOs() {
        for (var entry : uniformBlocks.entrySet()) {
            String name = entry.getKey();
            B3DUBODataBuffer buffer = entry.getValue();

            pass.setUniform(name, buffer.build());
        }
    }

    @Override
    public void cleanup() {
        if (pass != null) {
            pass.close();
        }
    }

    @Override
    public void bindUniformBlock(String name, AbstractUBODataBuffer buffer) {
        if (!(buffer instanceof B3DUBODataBuffer b3dbuf)) {
            throw new IllegalArgumentException(
                "UBO must be an instance of B3DUBODataBuffer"
            );
        }

        uniformBlocks.put(name, b3dbuf);
    }

    @Override
    public void bindTexture(String name, int textureId, int slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bindTexture'");
    }

    @Override
    public void bindTextureArray(String name, int textureId, int slot) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bindTextureArray'");
    }
}
