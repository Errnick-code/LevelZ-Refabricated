package dev.errnicraft.levelz_refabricated.mixin;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.screen.PlayerLevelSkillsScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenInfoMixin<T extends AbstractContainerMenu> extends Screen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow protected T menu;

    @Unique
    private static final Identifier SKILL_ICON = Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "textures/gui/skill_book.png");

    protected HandledScreenInfoMixin(Component title) {
        super(title);
    }

    @Unique
    private boolean player_level_skills$shouldAttach() {
        return this.menu instanceof net.minecraft.world.inventory.InventoryMenu;
    }

    @Inject(method = "renderBackground", at = @At("TAIL"))
    private void player_level_skills$drawBackground(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!player_level_skills$shouldAttach()) {
            return;
        }

        assert this.minecraft != null;
        assert this.minecraft.player != null;

        LevelManager levelManager = ((LevelManagerAccess) this.minecraft.player).getLevelManager();

        int iconSize = 9;
        int buttonX = this.leftPos + this.imageWidth / 2 + ConfigInit.CONFIG.inventorySkillLevelPosX;
        int buttonY = this.topPos + ConfigInit.CONFIG.inventorySkillLevelPosY;
        int textX = (56 + ConfigInit.CONFIG.inventorySkillLevelPosX + this.leftPos);
        int textY = (8 + ConfigInit.CONFIG.inventorySkillLevelPosY + this.topPos + font.lineHeight / 2);

        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + iconSize
                && mouseY >= buttonY && mouseY <= buttonY + iconSize;

        boolean hovereded = mouseX >= textX && mouseX <= textX + 14
                && mouseY >= (textY-3) && mouseY <= textY + 2;

        if (ConfigInit.CONFIG.inventorySkillLevel) {




            int color = 0xFFFFFFFF;
            if (levelManager.getSkillPoints() > 0) {
                color = 0xFF00E5FF;
            }

            context.pose().pushMatrix();
            context.pose().scale(0.6F, 0.6F);
            context.pose().translate(
                    (56 + ConfigInit.CONFIG.inventorySkillLevelPosX + this.leftPos) / 0.6F,
                    (8 + ConfigInit.CONFIG.inventorySkillLevelPosY + this.topPos + font.lineHeight / 2F) / 0.6F);
            context.drawString(this.font, Component.translatable("text.levelz.gui.short_level", levelManager.getOverallLevel()), 0, -font.lineHeight / 2, color, false);
            context.pose().popMatrix();
            if (hovereded) {
                context.setTooltipForNextFrame(this.font, Component.translatable("key.player_level_skills.open_screen", levelManager.getOverallLevel()).withStyle(ChatFormatting.AQUA), mouseX, mouseY);
            }
        }
// Icone pré preparado
//        int color = hovered ? 0xFFFFFFFF : 0xA9A9A9A9;
//
//        context.drawTexture(RenderPipelines.GUI_TEXTURED, SKILL_ICON, buttonX, buttonY, 0, 0, iconSize, iconSize, 9, 9, color);
//
//        if (hovered) {
//            int color2 = 0xFFFFFFFF;
//            if (levelManager.getSkillPoints() > 0) {
//                color2 = 0xFF55FF55;
//            }
//            context.drawTooltip(this.textRenderer, Text.translatable("key.player_level_skills.open_screen", levelManager.getOverallLevel()).formatted(Formatting.RED), mouseX, mouseY);
//        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void player_level_skills$onMouseClicked(MouseButtonEvent click, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (!player_level_skills$shouldAttach()) {
            return;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        int iconSize = 9;
        int buttonX = this.leftPos + this.imageWidth / 2 + ConfigInit.CONFIG.inventorySkillLevelPosX;
        int buttonY = this.topPos + ConfigInit.CONFIG.inventorySkillLevelPosY;
        int textX = (56 + ConfigInit.CONFIG.inventorySkillLevelPosX + this.leftPos);
        int textY = (8 + ConfigInit.CONFIG.inventorySkillLevelPosY + this.topPos + font.lineHeight / 2);

        if (mouseX >= textX && mouseX <= textX + 14
                && mouseY >= (textY-3) && mouseY <= textY + 2) {

            assert this.minecraft != null;

            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft.setScreen(null);

            this.minecraft.execute(() -> {
                Minecraft client = Minecraft.getInstance();
                if (client.player != null) {
                    client.setScreen(new PlayerLevelSkillsScreen());
                }
            });

            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void player_level_skills$onMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (!player_level_skills$shouldAttach()) {
            return;
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void player_level_skills$onMouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (!player_level_skills$shouldAttach()) {
            return;
        }
    }
}