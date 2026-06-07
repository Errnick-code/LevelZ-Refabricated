package dev.errnicraft.levelz_refabricated.init;

import dev.errnicraft.levelz_refabricated.screen.PlayerLevelSkillsScreen;
import dev.errnicraft.levelz_refabricated.screen.SkillInfoScreen;
import dev.errnicraft.levelz_refabricated.screen.SkillRestrictionScreen;
import dev.errnicraft.levelz_refabricated.screen.widget.LevelzTab;
import dev.errnicraft.levelz_refabricated.screen.widget.VanillaInventoryTab;
import dev.errnicraft.levelz_refabricated.util.TooltipUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.entity.render.LevelExperienceOrbEntityRenderer;
import net.libz.registry.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class RenderInit {

    public static final Identifier SKILL_TAB_ICON = LevelZRefabricated.identifierOf("textures/gui/sprites/skill_tab_icon.png");
    public static final Identifier BAG_TAB_ICON = LevelZRefabricated.identifierOf("textures/gui/sprites/bag_tab_icon.png");

    public static final Identifier MINEABLE_INFO = LevelZRefabricated.identifierOf("mineable_info");
    public static final Identifier MINEABLE_LEVEL_INFO = LevelZRefabricated.identifierOf("mineable_level_info");

    public static void init() {
        EntityRendererRegistry.register(EntityInit.LEVEL_EXPERIENCE_ORB, LevelExperienceOrbEntityRenderer::new);

       TabRegistry.registerInventoryTab(new VanillaInventoryTab(Component.translatable("container.crafting"), BAG_TAB_ICON, 0, InventoryScreen.class));
       TabRegistry.registerInventoryTab(new LevelzTab(Component.translatable("screen.levelz.skill_screen"), SKILL_TAB_ICON, 1, PlayerLevelSkillsScreen.class, SkillInfoScreen.class, SkillRestrictionScreen.class));

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            TooltipUtil.renderTooltip(Minecraft.getInstance(), drawContext);
        });

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            TooltipUtil.renderItemTooltip(Minecraft.getInstance(), stack, lines);
        });
    }
}
