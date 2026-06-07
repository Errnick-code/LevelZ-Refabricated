package dev.errnicraft.levelz_refabricated.screen;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.access.ClientPlayerAccess;
import dev.errnicraft.levelz_refabricated.access.LevelManagerAccess;
import dev.errnicraft.levelz_refabricated.init.ConfigInit;
import dev.errnicraft.levelz_refabricated.init.KeyInit;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.Skill;
import dev.errnicraft.levelz_refabricated.level.SkillAttribute;
import dev.errnicraft.levelz_refabricated.mixin.player.PlayerEntityAccessor;
import dev.errnicraft.levelz_refabricated.network.packet.AttributeSyncPacket;
import dev.errnicraft.levelz_refabricated.network.packet.StatPacket;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import net.libz.api.Tab;
import net.libz.util.DrawTabHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import java.util.*;

@Environment(EnvType.CLIENT)

public class PlayerLevelSkillsScreen extends Screen implements Tab {

    public static final Identifier BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "textures/gui/skill_background.png");

    public static final Identifier BACKGROUND_TEXTURE2 = Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "textures/gui/skill_background2.png");

    public static final Identifier ATTRIBUTE_BACKGROUND_TEXTURE = Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "textures/gui/attribute_background.png");

    public static final Identifier ICON_TEXTURE = Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "textures/gui/icons.png");

    private final int backgroundWidth = 200;
    private final int backgroundHeight = 215;

    private int x;
    private int y;

    private LevelManager levelManager;
    private LocalPlayer clientPlayerEntity;
    private final Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI).rotateLocalY(2.7f);
    private boolean turnClientPlayer = false;


    private List<SkillAttribute> attributes = new ArrayList<>();

    private boolean showAttributes = false;
    private int attributeRow = 0;

    private final WidgetButtonPage[] levelButtons = new WidgetButtonPage[12];
    private int skillRow = 0;

    //private final List<Integer> playerSkills = new ArrayList<>();

    public PlayerLevelSkillsScreen() {
        super(Component.translatable("screen.player_level_skills.title"));
    }

    @Override
    protected void init() {
        super.init();
        ClientPlayNetworking.send(new AttributeSyncPacket());

        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        Input playerInput = new Input(
                false,
                false,
                false,
                false,
                false, // jumping
                false, // sneaking
                false
        );

        this.levelManager = ((LevelManagerAccess) this.minecraft.player).getLevelManager();
        this.clientPlayerEntity = this.minecraft.gameMode.createPlayer(this.minecraft.level, this.minecraft.player.getStats(), this.minecraft.player.getRecipeBook(),playerInput, false);
        ((ClientPlayerAccess) this.clientPlayerEntity).setShouldRenderClientName(false);
        byte playerModelParts = this.minecraft.player.getEntityData().get(PlayerEntityAccessor.getPLAYER_MODEL_PARTS());
        this.clientPlayerEntity.getEntityData().set(PlayerEntityAccessor.getPLAYER_MODEL_PARTS(), playerModelParts);

        for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
            if (!this.minecraft.player.getItemBySlot(equipmentSlot).isEmpty()) {
                this.clientPlayerEntity.setItemSlot(equipmentSlot, this.minecraft.player.getItemBySlot(equipmentSlot));
            }
        }

        Map<Integer, SkillAttribute> skillAttributes = new HashMap<>();
        int attributeCount = 0;
        for (Skill skill : LevelManager.SKILLS.values()) {
            for (SkillAttribute skillAttribute : skill.getAttributes()) {
                if (skillAttribute.getId() < 0) {
                    continue;
                }
                skillAttributes.put(skillAttribute.getId(), skillAttribute);
                attributeCount++;

            }
        }
        for (int i = 0; i < attributeCount; i++) {
            this.attributes.add(skillAttributes.get(i));
        }
        for (int i = 0; i < 12; i++) {
            // Calculamos o ID real da skill levando em conta a linha (scroll)
            final int skillId = i + this.skillRow * 2;

            if (LevelManager.SKILLS.size() <= skillId) {
                // Se não existir skill para esse botão, criamos ele invisível/desativado
                this.levelButtons[i] = this.addRenderableWidget(new WidgetButtonPage(this.x + (i % 2 == 0 ? 80 : 169), this.y + 91 + i / 2 * 20, 13, 13, 33, 42, true, true, null, button -> {}));
                this.levelButtons[i].visible = false;
                continue;
            }

            // Pegamos o nome traduzido da skill (ex: Mineração, Alquimia...)
            Component skillName = LevelManager.SKILLS.get(skillId).getText();

            // Criamos o botão passando o skillName para a lista de tooltip dele
            this.levelButtons[i] = this.addRenderableWidget(new WidgetButtonPage(this.x + (i % 2 == 0 ? 80 : 169), this.y + 91 + i / 2 * 20, 13, 13, 33, 42, true, true, skillName, button -> {
                ClientPlayNetworking.send(new StatPacket(skillId, 1));
            }));
        }
        updateLevelButtons();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        //this.renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);

        if (this.minecraft != null && this.minecraft.player != null) {
            Component title = Component.translatable("screen.player_level_skills.player_title", this.minecraft.player.getName().getString());
            context.drawString(this.font, title, this.x + 118 - this.font.width(title) / 2, this.y + 7, 0xFF3F3F3F, false);

            if (!this.attributes.isEmpty()) {
                if (this.showAttributes) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 5, 30, 114, 15, 13, 256, 256);
                    context.blit(RenderPipelines.GUI_TEXTURED, ATTRIBUTE_BACKGROUND_TEXTURE, this.x + 202, this.y, 0, 0, 82, 215, 256, 256);
                    int maxAttributes = Math.min(this.attributes.size(), 15);
                    if (this.attributes.size() > 15) {
                        int scrollLevels = this.attributes.size() - 15;
                        int sliderY = this.attributeRow * 158 / scrollLevels;
                        context.blit(RenderPipelines.GUI_TEXTURED, ATTRIBUTE_BACKGROUND_TEXTURE, this.x + 270, this.y + 8 + sliderY, 82, 0, 6, 41, 256, 256);
                    } else {
                        context.blit(RenderPipelines.GUI_TEXTURED, ATTRIBUTE_BACKGROUND_TEXTURE, this.x + 270, this.y + 8, 88, 0, 6, 41, 256, 256);
                    }
                    context.drawString(this.font, Component.translatable("text.levelz.gui.attributes"), this.x + 214, this.y + 12, 0xFFE0E0E0, false);

                    int k = 27;
                    for (int i = this.attributeRow; i < this.attributeRow + maxAttributes; i++) {
                        var currentAttribute = this.attributes.get(i).getAttibute();
                        String attributeKey = this.attributes.get(i).getAttibute().getRegisteredName();
                         if (attributeKey.contains(":")) {
                            attributeKey = attributeKey.split(":")[1];
                        }
                        context.blit(RenderPipelines.GUI_TEXTURED,LevelZRefabricated.identifierOf("textures/gui/sprites/" + attributeKey + ".png"), this.x + 214, this.y + k, 0, 0, 9, 9, 9, 9);
                        float attributeValue = (float) Math.round(this.minecraft.player.getAttribute(this.attributes.get(i).getAttibute()).getValue() * 100.0D) / 100.0F;
                        context.drawString(this.font, Component.nullToEmpty(String.valueOf(attributeValue)), this.x + 214 + 15, this.y + k, 0xFFE0E0E0, false);

                        // --- NOVA LÓGICA DE TOOLTIP PARA O ATRIBUTO ---
                        // Verifica se o mouse está sobre a linha do atributo (largura aproximada de 60px para cobrir ícone e valor)
                        if (isPointWithinBounds(this.x + 214, this.y + k, 60, 10, mouseX, mouseY)) {
                            // Pega o nome traduzido do atributo (ex: "Velocidade de Movimento")
                            Component tooltipName = Component.translatable(currentAttribute.value().getDescriptionId());
                            context.setTooltipForNextFrame(this.font, tooltipName, mouseX, mouseY);
                        }

                        k += 12;
                    }
                } else {
                    context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 5, 15, 114, 15, 13, 256, 256);
                }
                if (isPointWithinBounds(this.x + 178, this.y + 5, 15, 13, mouseX, mouseY)) {
                    context.setTooltipForNextFrame(this.font, Component.translatable("text.levelz.gui.attributes"), mouseX, mouseY);
                }
            } else {
                context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 5, 0, 114, 15, 13, 256, 256);
            }

            // Level label
            Component skillLevelText = Component.translatable("text.levelz.gui.level",this.levelManager.getOverallLevel()); // this.levelManager.getOverallLevel()
            context.drawString(this.font, skillLevelText, this.x + 62, this.y + 42, 0xFF3F3F3F, false);
            // Point label
            Component skillPointText = Component.translatable("text.levelz.gui.points",this.levelManager.getSkillPoints()); //this.levelManager.getSkillPoints()
            context.drawString(this.font, skillPointText, this.x + 62, this.y + 54, 0xFF3F3F3F, false);
            // Label de Maestria (Voo)
            if (this.levelManager.hasAllSkillsMaxed()) {
                Component flightPowerText = Component.translatable("skill.mastery.flight_info").withStyle(ChatFormatting.DARK_GREEN);
                // x + 62 mantém o alinhamento, y + 66 coloca exatamente abaixo dos pontos
                context.drawString(this.font, flightPowerText, this.x + 62, this.y + 66, 0xFFFFFFFF, false);
            }


            // Experience bar
            context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 62, this.y + 21, 0, 100, 131, 5, 256, 256);

            int nextLevelExperience = this.levelManager.getNextLevelExperience();
            float levelProgress = this.levelManager.getLevelProgress();
            long experience = (int) (nextLevelExperience * levelProgress);

            context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 62, this.y + 21, 0, 105, (int) (130.0f * levelProgress), 5, 256, 256); // chance 5 to levelProgress
            // current xp label
            Component currentXpText = Component.translatable("text.levelz.gui.current_xp", experience, nextLevelExperience); //, experience, nextLevelExperience
            context.drawString(this.font, currentXpText, this.x - this.font.width(currentXpText) / 2 + 127, this.y + 30, 0xFF3F3F3F, false);

            if (!LevelManager.CRAFTING_RESTRICTIONS.isEmpty()) {
                if (isPointWithinBounds(this.x + 178, this.y + 29, 14, 13, mouseX, mouseY)) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 29, 30, 80, 15, 13, 256, 256);
                    context.setTooltipForNextFrame(this.font, Component.translatable("restriction.levelz.crafting"), mouseX, mouseY);
                } else {
                    context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 29, 15, 80, 15, 13, 256, 256);
                }
            } else {
                context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 29, 0, 80, 15, 13, 256, 256);
            }

            if (!LevelManager.MINING_RESTRICTIONS.isEmpty()) {
                if (isPointWithinBounds(this.x + 178, this.y + 45, 14, 13, mouseX, mouseY)) {
                    context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 45, 75, 80, 15, 13, 256, 256);
                    context.setTooltipForNextFrame(this.font, Component.translatable("restriction.levelz.mining"), mouseX, mouseY);
                } else {
                    context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 45, 60, 80, 15, 13, 256, 256);
                }
            } else {
                context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.x + 178, this.y + 45, 45, 80, 15, 13, 256, 256);
            }
        }


        if (this.clientPlayerEntity != null) {
            int x = this.width / 2;
            int y = this.height / 2;
            int size = 30; // Base do tamanho
            float scale = 1.0f; // Multiplicador de escala

            InventoryScreen.renderEntityInInventoryFollowsMouse(context, this.x + 8, this.y-46, this.x + 56,this.y + 76, 30, 1.0f, mouseX, mouseY, this.clientPlayerEntity);


//            if (isPointWithinBounds(this.x + 9, this.y + 67, 15, 10, mouseX, mouseY)) {
//                context.drawTexture(RenderPipelines.GUI_TEXTURED,ICON_TEXTURE, this.x + 9, this.y + 67, 0, 138, 15, 10,256,256);
//            } else {
//                context.drawTexture(RenderPipelines.GUI_TEXTURED,ICON_TEXTURE, this.x + 9, this.y + 67, 0, 128, 15, 10,256,256);
//            }
//            if (isPointWithinBounds(this.x + 41, this.y + 67, 15, 10, mouseX, mouseY)) {
//                context.drawTexture(RenderPipelines.GUI_TEXTURED,ICON_TEXTURE, this.x + 41, this.y + 67, 15, 138, 15, 10,256,256);
//            } else {
//                context.drawTexture(RenderPipelines.GUI_TEXTURED,ICON_TEXTURE, this.x + 41, this.y + 67, 15, 128, 15, 10,256,256);
//            }

        }


    }



    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderTransparentBackground(context);
        context.blit(RenderPipelines.GUI_TEXTURED,BACKGROUND_TEXTURE2, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight,this.backgroundWidth,this.backgroundHeight);

        for (int i = 0; i < 12; i++) {
            int skillId = i + this.skillRow * 2;
            if (LevelManager.SKILLS.size() <= skillId) {
                break;
            }

            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, this.x + (i % 2 == 0 ? 8 : 96), this.y + 87 + i / 2 * 20, 0, 215, 88, 20, 256, 256);
            context.blit(RenderPipelines.GUI_TEXTURED, LevelZRefabricated.identifierOf("textures/gui/sprites/" + LevelManager.SKILLS.get(skillId).getKey() + ".png"), this.x + (i % 2 == 0 ? 11 : 99), this.y + 89 + i / 2 * 20, 0, 0, 16, 16, 16, 16);

            Component skillLevel = Component.translatable("text.levelz.gui.current_level", this.levelManager.getSkillLevel(skillId), LevelManager.SKILLS.get(skillId).getMaxLevel());
            context.drawString(this.font, skillLevel, this.x + (i % 2 == 0 ? 53 : 141) - this.font.width(skillLevel) / 2, this.y + 94 + i / 2 * 20, 0xFF3F3F3F, false);

            if (isPointWithinBounds(this.x + (i % 2 == 0 ? 11 : 99), this.y + 89 + i / 2 * 20, 16, 16, mouseX, mouseY)) {
                context.setTooltipForNextFrame(this.font, LevelManager.SKILLS.get(skillId).getText(), mouseX, mouseY);
            }
        }
        int totalSkills = LevelManager.SKILLS.size();
        if (totalSkills > 12) {
            int scrollLevels = Math.max(1, (int) Math.ceil((totalSkills - 12) / 2.0));
            int sliderY = (int) ((this.skillRow * 86.0f) / scrollLevels);
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, this.x + 186, this.y + 87 + sliderY, 200, 0, 6, 34, 256, 256);
        } else {
            context.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND_TEXTURE, this.x + 186, this.y + 87, 206, 0, 6, 34, 256, 256);
        }
        DrawTabHelper.drawTab(minecraft, context, this, this.x, this.y, mouseX, mouseY);
    }

//    @Override
//    public void tick() {
//        super.tick();
//        if (this.clientPlayerEntity != null && this.turnClientPlayer) {
//            double mouseX = this.client.mouse.getX() * (double) this.client.getWindow().getScaledWidth() / (double) this.client.getWindow().getWidth();
//            double mouseY = this.client.mouse.getY() * (double) this.client.getWindow().getScaledHeight() / (double) this.client.getWindow().getHeight();
//
//            if (isPointWithinBounds(this.x + 9, this.y + 67, 15, 10, mouseX, mouseY)) {
//                this.quaternionf.rotateLocalY(0.087f);
//            } else if (isPointWithinBounds(this.x + 41, this.y + 67, 15, 10, mouseX, mouseY)) {
//                this.quaternionf.rotateLocalY(-0.087f);
//            } else {
//                this.turnClientPlayer = false;
//            }
//        }
//    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (KeyInit.screenKey.matches(input) || Objects.requireNonNull(minecraft).options.keyInventory.matches(input)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (this.turnClientPlayer) {
            this.turnClientPlayer = false;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        DrawTabHelper.onTabButtonClick(minecraft, this, this.x, this.y, mouseX, mouseY, this.getFocused() != null);

        if (!this.attributes.isEmpty() && isPointWithinBounds(this.x + 178, this.y + 5, 15, 13, mouseX, mouseY)) {
            this.showAttributes = !this.showAttributes;
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (!LevelManager.CRAFTING_RESTRICTIONS.isEmpty() && isPointWithinBounds(this.x + 178, this.y + 29, 14, 13, mouseX, mouseY)) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft.setScreen(new SkillRestrictionScreen(this.levelManager, LevelManager.CRAFTING_RESTRICTIONS, Component.translatable("restriction.levelz.crafting"), 0));
            return true;
        }

        if (!LevelManager.MINING_RESTRICTIONS.isEmpty() && isPointWithinBounds(this.x + 178, this.y + 45, 14, 13, mouseX, mouseY)) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            this.minecraft.setScreen(new SkillRestrictionScreen(this.levelManager, LevelManager.MINING_RESTRICTIONS, Component.translatable("restriction.levelz.mining"), 1));
            return true;
        }

//        if (this.clientPlayerEntity != null) {
//            if (isPointWithinBounds(this.x + 9, this.y + 67, 15, 10, mouseX, mouseY)) {
//                this.turnClientPlayer = true;
//                this.client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//                return true;
//            } else if (isPointWithinBounds(this.x + 41, this.y + 67, 15, 10, mouseX, mouseY)) {
//                this.turnClientPlayer = true;
//                this.client.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.UI_BUTTON_CLICK, 1.0F));
//                return true;
//            }
//        }

        for (int i = 0; i < 12; i++) {
            int skillId = i + this.skillRow * 2;
            if (LevelManager.SKILLS.size() <= skillId) {
                break;
            }

            int iconX = this.x + (i % 2 == 0 ? 11 : 99);
            int iconY = this.y + 89 + i / 2 * 20;

            if (isPointWithinBounds(iconX, iconY, 16, 16, mouseX, mouseY)) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                this.minecraft.setScreen(new SkillInfoScreen(this.levelManager, skillId));
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.showAttributes && this.attributes.size() > 15 && isPointWithinBounds(this.x + 209, this.y + 7, 68, 201, mouseX, mouseY)) {
            int maxAttributeRow = this.attributes.size() - 15;
            int newAttributeRow = this.attributeRow - (int) verticalAmount;
            this.attributeRow = Math.max(0, Math.min(newAttributeRow, maxAttributeRow));
            return true;
        }

        int totalSkills = LevelManager.SKILLS.size();
        if (totalSkills > 12 && isPointWithinBounds(this.x + 7, this.y + 86, 186, 122, mouseX, mouseY)) {
            int maxSkillRow = Math.max(0, (int) Math.ceil((totalSkills - 12) / 2.0));

            int newSkillRow = this.skillRow - (int) verticalAmount;
            this.skillRow = Math.max(0, Math.min(newSkillRow, maxSkillRow));

            updateLevelButtons();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!turnClientPlayer) return false;
        return mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void updateLevelButtons() {
        for (int i = 0; i < this.levelButtons.length; i++) {
            int skillId = i + this.skillRow * 2;

            if (LevelManager.SKILLS.size() <= skillId) {
                this.levelButtons[i].visible = false;
                continue;
            } else {
                this.levelButtons[i].visible = true;
            }

            // --- CORREÇÃO AQUI ---
            // Removi a trava do overallMaxLevel. Agora o botão só desativa se:
            // A skill já atingiu o nível máximo dela OU o player não tem pontos.
            if (LevelManager.SKILLS.get(skillId).getMaxLevel() <= this.levelManager.getSkillLevel(skillId)) {
                this.levelButtons[i].active = false;
            } else {
                // Se ainda não atingiu o máximo da skill, o botão fica ativo se houver pontos
                this.levelButtons[i].active = this.levelManager.getSkillPoints() > 0;
            }

            // Lógica de "Permitir Níveis Extras" (caso todas as skills estejam no máximo)
            if (ConfigInit.CONFIG.allowHigherSkillLevel && this.levelManager.getSkillPoints() > 0) {
                boolean maxedAllSkills = true;
                for (Skill skillCheck : LevelManager.SKILLS.values()) {
                    if (skillCheck.getMaxLevel() > this.levelManager.getSkillLevel(skillCheck.getId())) {
                        maxedAllSkills = false;
                        break;
                    }
                }
                if (maxedAllSkills) {
                    this.levelButtons[i].active = true;
                }
            }
        }
    }


    public static boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY) {
        return pointX >= (double) (x - 1) && pointX < (double) (x + width + 1) && pointY >= (double) (y - 1) && pointY < (double) (y + height + 1);
    }

    private static class WidgetButtonPage extends Button {

        private final boolean hoverOutline;
        private final boolean clickable;
        private final int textureX;
        private final int textureY;
        private final List<net.minecraft.network.chat.@Nullable Component> tooltip = new ArrayList<net.minecraft.network.chat.@Nullable Component>();
        private int clickedKey = -1;



        public WidgetButtonPage(int x, int y, int sizeX, int sizeY, int textureX, int textureY, boolean hoverOutline, boolean clickable, @Nullable net.minecraft.network.chat.Component tooltip, Button.OnPress onPress) {
            super(x, y, sizeX, sizeY, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
            this.hoverOutline = hoverOutline;
            this.clickable = clickable;
            this.textureX = textureX;
            this.textureY = textureY;
            this.width = sizeX;
            this.height = sizeY;

            if (tooltip != null) {
                this.tooltip.add(tooltip);
            }
        }

        @Override
        protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
            Minecraft minecraftClient = Minecraft.getInstance();


            context.pose().pushMatrix();
            int i = hoverOutline ? this.getTextureY() : 0;
            context.blit(RenderPipelines.GUI_TEXTURED, ICON_TEXTURE, this.getX(), this.getY(), this.textureX + i * this.width, this.textureY, this.width, this.height, 256, 256);
            context.pose().popMatrix();


            if (this.isHovered() && !this.tooltip.isEmpty()) {
                // Pega o primeiro item da lista (o nome da skill)
                net.minecraft.network.chat.Component skillName = this.tooltip.get(0);

                // Cria o texto final usando o argumento
                net.minecraft.network.chat.Component finalTooltip = net.minecraft.network.chat.Component.translatable("text.levelz.gui.up_level", skillName);

                context.setTooltipForNextFrame(minecraftClient.font, finalTooltip, mouseX, mouseY);
            }

        }

        //@Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.clickedKey = button;
            if (!this.clickable) {
                return false;
            }
            return true;
        }

        @Override
        protected boolean isValidClickButton(MouseButtonInfo input) {
            return super.isValidClickButton(input);
        }

        @Override
        public boolean keyPressed(KeyEvent input) {
            if (!this.clickable) {
                return false;
            }
            return super.keyPressed(input);
        }
        public void addTooltip(net.minecraft.network.chat.@Nullable Component text) {
            this.tooltip.add(text);
        }

        public boolean wasMiddleButtonClicked() {
            return clickedKey == 2;
        }

        public boolean wasRightButtonClicked() {
            return clickedKey == 1;
        }

        private int getTextureY() {
            int i = 1;
            if (!this.active) {
                i = 0;
            } else if (this.isHovered()) {
                i = 2;
            }
            return i;
        }
    }
}