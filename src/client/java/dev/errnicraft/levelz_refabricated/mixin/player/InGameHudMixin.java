package dev.errnicraft.levelz_refabricated.mixin.player;

import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ContextualBarRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(ContextualBarRenderer.class)
public interface InGameHudMixin {

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private static void player_level_skills$drawExperienceLevel(GuiGraphics context, Font textRenderer, int level, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        boolean hasAvailableLevel = ((LevelManagerAccess) client.player).getLevelManager().hasAvailableLevel();
        if (!hasAvailableLevel) {
            return;
        }

        Component text = Component.literal(Integer.toString(level));
        int x = context.guiWidth() / 2 - textRenderer.width(text) / 2;
        int y = context.guiHeight() - 35;

        // sombra preta manual
        context.drawString(textRenderer, text, x + 1, y, 0xFF000000, false);
        context.drawString(textRenderer, text, x - 1, y, 0xFF000000, false);
        context.drawString(textRenderer, text, x, y + 1, 0xFF000000, false);
        context.drawString(textRenderer, text, x, y - 1, 0xFF000000, false);

        // texto principal em ciano
        context.drawString(textRenderer, text, x, y, 0xFF00E5FF, false);

        ci.cancel();
    }
}