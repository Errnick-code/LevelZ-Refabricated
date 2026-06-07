package dev.errnicraft.levelz_refabricated.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class LevelExperienceOrbEntityRenderer extends EntityRenderer<LevelExperienceOrbEntity, LevelExperienceOrbEntityRenderer.OrbRenderState> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/experience_orb.png");
    private static final RenderType RENDER_TYPE = RenderTypes.itemEntityTranslucentCull(TEXTURE);

    public LevelExperienceOrbEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    @Override
    public OrbRenderState createRenderState() {
        return new OrbRenderState();
    }

    @Override
    public void extractRenderState(LevelExperienceOrbEntity orb, OrbRenderState state, float partialTicks) {
        super.extractRenderState(orb, state, partialTicks);
        // orbSize already returns 0-10 matching vanilla getIcon()
        state.icon = orb.getOrbSize();
    }

    @Override
    protected int getBlockLightLevel(LevelExperienceOrbEntity orb, BlockPos pos) {
        return Mth.clamp(super.getBlockLightLevel(orb, pos) + 7, 0, 15);
    }

    @Override
    public void submit(OrbRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        // Same UV calculation as vanilla (icon directly, no *100)
        int icon = state.icon;
        float u0 = (icon % 4 * 16 + 0) / 64.0F;
        float u1 = (icon % 4 * 16 + 16) / 64.0F;
        float v0 = (icon / 4 * 16 + 0) / 64.0F;
        float v1 = (icon / 4 * 16 + 16) / 64.0F;

        // LevelZ orb color: blue <-> green oscillation (vanilla is yellow <-> green)
        // Green stays 255 (max), red stays 0, blue oscillates 0-255
        // Result: smooth pulse from pure green to cyan and back
        float rr = state.ageInTicks / 2.0F;
        int rc = 0;                                                              // red: always off
        int gc = 255;                                                            // green: always max
        int bc = (int)((Mth.sin(rr) + 1.0F) * 0.5F * 255.0F);                 // blue: 0-255 oscillating

        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(camera.orientation);
        poseStack.scale(0.3F, 0.3F, 0.3F);

        submitNodeCollector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, -0.5F, -0.25F, rc, gc, bc, u0, v1, state.lightCoords);
            vertex(buffer, pose,  0.5F, -0.25F, rc, gc, bc, u1, v1, state.lightCoords);
            vertex(buffer, pose,  0.5F,  0.75F, rc, gc, bc, u1, v0, state.lightCoords);
            vertex(buffer, pose, -0.5F,  0.75F, rc, gc, bc, u0, v0, state.lightCoords);
        });

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, int r, int g, int b,
                               float u, float v, int lightCoords) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(r, g, b, 128)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightCoords)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    public static class OrbRenderState extends EntityRenderState {
        public int icon;
    }
}
