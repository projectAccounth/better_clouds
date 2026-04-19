package net.not_thefirst.story_mode_clouds.mixin;

public class CloudRenderInjection {
    private CloudRenderInjection() {}

    public static final String PRE_CLOUD_RENDERER = "renderClouds";

    public static final String MODERN_FABRIC_RENDER = "method_62205";
    /* signature:
    * void(
    *    int cloudColor,
    *    CloudStatus status,
    *    float cloudHeight,
    *    Vec3 vec3,
    *    long l,
    *    float partialTicks
    * )
    */

    public static final String MODERN_FORGE_RENDER  = "lambda$addCloudsPass$6";
    /* signature:
    * void(
    *    float cHeight/pTicks, 
    *    Vec3 vec3, 
    *    Matrix4f modelView, 
    *    Matrix4f proj, 
    *    int cloudColor, 
    *    CloudStatus status, 
    *    float cHeight/pTicks
    * )
    */

    // or whatever the signatures were I don't remember
}
