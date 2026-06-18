package net.not_thefirst.story_mode_clouds.utils.rendering.opengl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.joml.Matrix4f;

import net.not_thefirst.lib.gl_render_system.mesh.GpuMesh;
import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.UniformBufferObject;
import net.not_thefirst.lib.gl_render_system.state.GLStateGuard;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractRenderPass;
import net.not_thefirst.story_mode_clouds.utils.rendering.AbstractUBODataBuffer;
import net.not_thefirst.story_mode_clouds.utils.rendering.UniformValue;

public class GLRenderPass extends AbstractRenderPass<GLPipeline> {

    private final Map<String, ByteBuffer> uniformBlocks =
        new HashMap<>();

    private final Map<GLPipeline,
                  Map<String, UniformBufferObject>> pipelineUbos =
        new HashMap<>();

    private GpuMesh mesh;
    private GLStateGuard stateGuard;

    private int nextBinding = 0;

    public GLRenderPass(String name, GLPipeline... pipelines) {
        super(name, pipelines);
    }

    @Override
    public void setup() {
        stateGuard = new GLStateGuard();
    }

    public void setMesh(GpuMesh mesh) {
        if (this.mesh != null) {
            throw new IllegalStateException("Mesh is already set for this render pass");
        }
        if (mesh == null) {
            throw new IllegalArgumentException("Mesh cannot be null");
        }
        this.mesh = mesh;
    }

    @Override
    public void render() {
        for (GLPipeline pipeline : pipelines) {
            pipeline.bind();

            bindPipelineUBOs(pipeline);
            bindUniforms(pipeline);

            this.mesh.draw();

            pipeline.unbind();
        }
    }

    @Override
    public void cleanup() {
        stateGuard.close();
    }

    private void bindUniforms(GLPipeline pipeline) {
        GLProgram program = pipeline.getProgram();
        for (var entry : uniforms.entrySet()) {
            String name = entry.getKey();
            UniformValue value = entry.getValue();

            setUniform(program, name, value);
        }
    }

    private void setUniform(GLProgram program, String name, UniformValue value) {
        switch (value.type()) {
            case INT1 -> program.setInt(name, (int) value.value());
            case INT2 -> {
                int[] arr = (int[]) value.value();
                program.setVec2(name, arr[0], arr[1]);
            }
            case INT3 -> {
                int[] arr = (int[]) value.value();
                program.setVec3(name, arr[0], arr[1], arr[2]);
            }
            case INT4 -> {
                int[] arr = (int[]) value.value();
                program.setVec4(name, arr[0], arr[1], arr[2], arr[3]);
            }
            case FLOAT1 -> program.setFloat(name, (float) value.value());
            case FLOAT2 -> {
                float[] arr = (float[]) value.value();
                program.setVec2(name, arr[0], arr[1]);
            }
            case FLOAT3 -> {
                float[] arr = (float[]) value.value();
                program.setVec3(name, arr[0], arr[1], arr[2]);
            }
            case FLOAT4 -> {
                float[] arr = (float[]) value.value();
                program.setVec4(name, arr[0], arr[1], arr[2], arr[3]);
            }
            case MAT4 -> program.setMat4(name, new Matrix4f().set((float[]) value.value()));
            case MAT3 -> throw new UnsupportedOperationException("MAT3 uniforms are not supported yet");
            default -> throw new IllegalArgumentException("Unknown uniform type: " + value.type());
        }
    }

    private void bindPipelineUBOs(GLPipeline pipeline) {
        Map<String, UniformBufferObject> ubos =
            pipelineUbos.computeIfAbsent(
                pipeline,
                p -> new HashMap<>()
            );

        for (var entry : uniformBlocks.entrySet()) {
            String name = entry.getKey();
            ByteBuffer data = entry.getValue();

            Integer slot =
                pipeline.getUniformBlocks().get(name);

            if (slot == null) {
                continue;
            }

            UniformBufferObject ubo =
                ubos.computeIfAbsent(
                    name,
                    n -> new UniformBufferObject(
                        slot,
                        data.limit()
                    )
                );

            ubo.bind();
            ubo.update(data);
        }
    }

    @Override
    public void bindUniformBlock(
            String name,
            AbstractUBODataBuffer buffer) {

        if (!(buffer instanceof GLUBODataBuffer glBuffer)) {
            throw new IllegalArgumentException(
                "UBO must be an instance of GLUBODataBuffer"
            );
        }

        uniformBlocks.put(
            name,
            glBuffer.build()
        );
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
