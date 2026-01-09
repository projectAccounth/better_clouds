package net.not_thefirst.story_mode_clouds.renderer.render_system.state;

public final class RenderStateBuilder {

    private DepthTestState depth = DepthTestState.LEQUAL;
    private BlendState blend = BlendState.NONE;
    private CullState cull = CullState.CULL;
    private MaskState mask = MaskState.COLOR_DEPTH;
    private ShadeState shade = ShadeState.SMOOTH;

    public RenderStateBuilder shade(ShadeState state) {
        this.shade = state;
        return this;
    }

    public RenderStateBuilder depthTest(DepthTestState state) {
        this.depth = state;
        return this;
    }

    public RenderStateBuilder mask(MaskState state) {
        this.mask = state;
        return this;
    }

    public RenderStateBuilder blend(BlendState state) {
        this.blend = state;
        return this;
    }

    public RenderStateBuilder cull(CullState state) {
        this.cull = state;
        return this;
    }

    public CompositeRenderState build() {
        return new CompositeRenderState(
            depth,
            blend,
            cull,
            mask,
            shade
        );
    }
}
