package net.not_thefirst.story_mode_clouds.utils.rendering.gl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.RenderParams;
import net.not_thefirst.lib.gl_render_system.alt.UniformValue;
import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.UniformBufferObject;
import net.not_thefirst.lib.gl_render_system.state.GLStateGuard;

public class GLRenderPass extends AbstractRenderPass<GLPipeline> {

    private final Map<String, ByteBuffer> uniformBlocks =
        new HashMap<>();

    private final Map<GLPipeline,
                  Map<String, UniformBufferObject>> pipelineUbos =
        new HashMap<>();

    private GLStateGuard stateGuard;

    public GLRenderPass(String name, GLPipeline... pipelines) {
        super(name, pipelines);
    }

    @Override
    public void setup() {
        super.setup();
        stateGuard = new GLStateGuard();
    }

    @Override
    public void setMesh(AbstractStaticMesh<?> mesh, int indexCount) {
        if (mesh == null) {
            throw new IllegalArgumentException("Mesh cannot be null");
        }
        if (!(mesh instanceof GLMesh)) {
            throw new IllegalArgumentException("Mesh must be a GLMesh instance");
        }
        this.meshData = mesh;
    }

    public void validateBeforeRender() {
        if (pipelines.isEmpty())
            throw new IllegalStateException("Rendering pass with no pipelines, pass name: " + name);
    }

    @Override
    public void render() {
        super.render();
        validateBeforeRender();
        
        for (GLPipeline pipeline : pipelines) {
            pipeline.bind();

            bindPipelineUBOs(pipeline);
            bindUniforms(pipeline);

            ((GLMesh) this.meshData).draw(this, new RenderParams());

            pipeline.unbind();
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        stateGuard.close();
    }

    private void bindUniforms(GLPipeline pipeline) {
        GLProgram program = pipeline.getProgram();
        for (Entry<String, UniformValue> entry : uniforms.entrySet()) {
            String name = entry.getKey();
            UniformValue value = entry.getValue();

            setUniform(program, name, value);
        }
    }

    private void setUniform(GLProgram program, String name, UniformValue value) {
        switch (value.type()) {
            case INT1: program.setInt(name, (int) value.value()); break;
            case INT2 : {
                int[] arr = (int[]) value.value();
                program.setVec2(name, arr[0], arr[1]);
                break;
            }
            case INT3 : {
                int[] arr = (int[]) value.value();
                program.setVec3(name, arr[0], arr[1], arr[2]);
                break;
                
            }
            case INT4 : {
                int[] arr = (int[]) value.value();
                program.setVec4(name, arr[0], arr[1], arr[2], arr[3]);
                break;
            }
            case FLOAT1 : program.setFloat(name, (float) value.value()); break;
            case FLOAT2 : {
                float[] arr = (float[]) value.value();
                program.setVec2(name, arr[0], arr[1]);
                break;
            }
            case FLOAT3 : {
                float[] arr = (float[]) value.value();
                program.setVec3(name, arr[0], arr[1], arr[2]);
                break;
            }
            case FLOAT4 : {
                float[] arr = (float[]) value.value();
                program.setVec4(name, arr[0], arr[1], arr[2], arr[3]);
                break;
            }
            case MAT4 : program.setMat4(name, new Matrix4f().set((float[]) value.value())); break;
            case MAT3 : program.setMat3(name, new Matrix3f().set((float[]) value.value())); break;
            default : throw new IllegalArgumentException("Unknown uniform type: " + value.type());
        }
    }

    private void bindPipelineUBOs(GLPipeline pipeline) {
        Map<String, UniformBufferObject> ubos =
            pipelineUbos.computeIfAbsent(
                pipeline,
                p -> new HashMap<>()
            );

        for (Entry<String, ByteBuffer> entry : uniformBlocks.entrySet()) {
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

            ubo.update(data);
        }
    }

    @Override
    public void bindUniformBlock(
            String name,
            AbstractUBODataBuffer<?, ?> buffer) {

        if (!(buffer instanceof GLUBODataBuffer)) {
            throw new IllegalArgumentException(
                "UBO must be an instance of GLUBODataBuffer"
            );
        }

        uniformBlocks.put(
            name,
            ((GLUBODataBuffer) buffer).build()
        );
    }

    @Override
    public void bindTexture(String name, int textureId, int slot) {
        throw new UnsupportedOperationException("Unimplemented method 'bindTexture'");
    }

    @Override
    public void bindTextureArray(String name, int textureId, int slot) {
        throw new UnsupportedOperationException("Unimplemented method 'bindTextureArray'");
    }
}
