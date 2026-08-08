package net.not_thefirst.story_mode_clouds.renderer.platform.gl;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

import net.not_thefirst.lib.gl_render_system.alt.AbstractRenderPass;
import net.not_thefirst.lib.gl_render_system.alt.AbstractStaticMesh;
import net.not_thefirst.lib.gl_render_system.alt.AbstractTexture;
import net.not_thefirst.lib.gl_render_system.alt.AbstractUBODataBuffer;
import net.not_thefirst.lib.gl_render_system.alt.UniformValue;
import net.not_thefirst.lib.gl_render_system.shader.GLProgram;
import net.not_thefirst.lib.gl_render_system.shader.GLTextureBinding;
import net.not_thefirst.lib.gl_render_system.shader.UniformBufferObject;
import net.not_thefirst.lib.gl_render_system.state.GLStateGuard;

public class GLRenderPass extends AbstractRenderPass<GLPipeline> {

    private final Map<String, ByteBuffer> uniformBlocks = new HashMap<>();
    private final Map<Integer, GLTextureBinding> textureBindings = new HashMap<>();
    private final Map<Integer, AbstractTexture> boundTextureObjects = new HashMap<>();
    private final Map<String, Integer> samplerBindings = new HashMap<>();

    private GLStateGuard stateGuard;

    public GLRenderPass(String name, GLPipeline... pipelines) {
        super(name, pipelines);
    }

    @Override
    public void setup() {
        super.setup();
        boundTextureObjects.forEach((slot, tex) -> {
            tex.bind(slot);
        });
        // stateGuard = new GLStateGuard();
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

        if (renderTarget != null) {
            if (!(renderTarget instanceof GLFramebuffer)) {
                throw new IllegalStateException("Render target must be a GLFramebufferRenderTarget");
            }
        }
    }

    @Override
    public void render() {
        super.render();

        if (renderTarget != null) {
            if (clearColor.isPresent()) {
                Vector4f color = clearColor.get();
                renderTarget.clearColorAttachment(color.x, color.y, color.z, color.w);
            }
            if (clearDepth.isPresent()) {
                renderTarget.clearDepthAttachment(clearDepth.get());
            }

            renderTarget.bind();
        }

        validateBeforeRender();

        for (GLPipeline pipeline : pipelines) {
            pipeline.bind();

            uploadUniformBlocks(pipeline);
            bindUniforms(pipeline);
            bindTextures(pipeline.getProgram());

            ((GLMesh) meshData).draw(
                this,
                renderParams
            );

            pipeline.unbind();
        }

        if (renderTarget != null) {
            renderTarget.unbind();
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        boundTextureObjects.forEach((slot, tex) -> {
            tex.unbind(slot);
        });
        // stateGuard.close();
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

    private void uploadUniformBlocks(
        GLPipeline pipeline
    ) {

        for (Entry<String, ByteBuffer> entry :
                uniformBlocks.entrySet()) {

            UniformBufferObject ubo =
                pipeline.getUbos()
                        .get(entry.getKey());

            if (ubo == null) {
                continue;
            }

            ByteBuffer data = entry.getValue();

            if (data.remaining() != ubo.size()) {

                throw new IllegalStateException(
                    "UBO size mismatch for block '" +
                    entry.getKey() +
                    "' (shader=" +
                    ubo.size() +
                    ", buffer=" +
                    data.remaining() +
                    ")"
                );
            }

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
    public void bindTexture(String name, AbstractTexture tex, int slot) {
        if (name == null) {
            throw new IllegalArgumentException("Texture sampler name cannot be null");
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Texture slot cannot be negative");
        }
        if (!(tex instanceof GLTexture2D))
            throw new IllegalArgumentException("Incompatible texture type");

        textureBindings.put(slot, new GLTextureBinding(GL11.GL_TEXTURE_2D, ((GLTexture2D) tex).getId(), null));
        boundTextureObjects.put(slot, tex);
        samplerBindings.put(name, slot);
    }

    @Override
    public void bindTextureArray(String name, int textureId, int slot) {
        if (name == null) {
            throw new IllegalArgumentException("Texture sampler name cannot be null");
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Texture slot cannot be negative");
        }

        textureBindings.put(slot, new GLTextureBinding(GL31.GL_TEXTURE_2D_ARRAY, textureId, null));
        samplerBindings.put(name, slot);
    }

    private void bindTextures(GLProgram program) {
        for (Map.Entry<String, Integer> entry : samplerBindings.entrySet()) {
            String samplerName = entry.getKey();
            int unit = entry.getValue();
            GLTextureBinding binding = textureBindings.get(unit);
            if (binding == null) {
                continue;
            }
            program.bindTexture(samplerName, binding, unit);
        }
    }
}
