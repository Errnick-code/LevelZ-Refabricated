package dev.errnicraft.levelz_refabricated.screen.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import dev.errnicraft.levelz_refabricated.level.restriction.PlayerRestriction;
import dev.errnicraft.levelz_refabricated.mixin.entity.VehicleEntityAccessor;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentRegistry;
import dev.errnicraft.levelz_refabricated.registry.EnchantmentZ;
import dev.errnicraft.levelz_refabricated.screen.PlayerLevelSkillsScreen;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;

@Environment(EnvType.CLIENT)
public class LineWidget {

    private final Minecraft client;
    @Nullable
    private final Component text;
    @Nullable
    private final Map<Integer, PlayerRestriction> restrictions;
    private final int code;

    private Map<Integer, ItemStack> customStacks;
    private Map<Integer, Identifier> customImages;

    /**
     * @param code 0 = item, 1 = block, 2 = entity, 3 = enchantment
     */
    public LineWidget(Minecraft client, @Nullable Component text, @Nullable Map<Integer, PlayerRestriction> restrictions, int code) {
        this.client = client;
        this.text = text;
        this.restrictions = restrictions;
        this.code = code;

        if (this.code == 2) {
            this.customStacks = new HashMap<>();
            this.customImages = new HashMap<>();
            for (Integer id : this.restrictions.keySet()) {
                EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.byId(id);
                boolean imageExists = false;
                try {
                    client.getResourceManager().getResourceOrThrow(LevelZRefabricated.identifierOf("textures/gui/sprites/entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath() + ".png"));
                    imageExists = true;
                } catch (FileNotFoundException ignored) {
                }
                if (imageExists) {
                    this.customImages.put(id, LevelZRefabricated.identifierOf("textures/gui/sprites/entity/" + BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath() + ".png"));
                } else if (SpawnEggItem.byId(entityType) != null) {
                    this.customStacks.put(id, new ItemStack(Objects.requireNonNull(SpawnEggItem.byId(entityType))));
                } else {
                    this.customImages.put(id, LevelZRefabricated.identifierOf("textures/gui/sprites/entity/default.png"));
                }
            }
        }
        else if (this.code == 3) {
            this.customStacks = new HashMap<>();

            for (Integer id : this.restrictions.keySet()) {
                EnchantmentZ enchantmentZ = EnchantmentRegistry.getEnchantmentZ(id);

                ItemStack stack = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable builder = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                builder.upgrade(enchantmentZ.getEntry(), enchantmentZ.getLevel());
                stack.set(DataComponents.STORED_ENCHANTMENTS, builder.toImmutable());

                this.customStacks.put(id, stack);
            }
        }
        else if (this.code == 4) {
            this.customStacks = new HashMap<>();
            for (Integer id : this.restrictions.keySet()) {
                // Agora o ID é 24, 34, etc. O Minecraft vai encontrar!
                net.minecraft.world.item.alchemy.Potion potion = BuiltInRegistries.POTION.byId(id);

                if (potion != null) {
                    var entry = BuiltInRegistries.POTION.wrapAsHolder(potion);
                    // Cria a poção colorida e com nome
                    ItemStack stack = net.minecraft.world.item.alchemy.PotionContents.createItemStack(
                            net.minecraft.world.item.Items.POTION,
                            entry
                    );
                    this.customStacks.put(id, stack);
                } else {
                    this.customStacks.put(id, new ItemStack(net.minecraft.world.item.Items.POTION));
                }
            }
        }


    }

    public void render(GuiGraphics drawContext, int x, int y, int mouseX, int mouseY) {
        if (text != null) {
            drawContext.drawString(this.client.font, this.text, x, y + 4, 0xFF3F3F3F, false);
        } else {
            int separator = 0;
            boolean showTooltip = false;
            for (Map.Entry<Integer, PlayerRestriction> entry : this.restrictions.entrySet()) {
                Component tooltipTitle;
                drawContext.blit(RenderPipelines.GUI_TEXTURED,PlayerLevelSkillsScreen.ICON_TEXTURE, x + separator - 1, y - 1, 0, 148, 18, 18,256,256);
                if (this.code == 0) {
                    Item item = BuiltInRegistries.ITEM.byId(entry.getKey());
                    tooltipTitle = item.getName();
                    drawContext.renderItem(BuiltInRegistries.ITEM.byId(entry.getKey()).getDefaultInstance(), x + separator, y);
                } else if (this.code == 1) {
                    Block block = BuiltInRegistries.BLOCK.byId(entry.getKey());
                    tooltipTitle = block.getName();
                    drawContext.renderItem(block.asItem().getDefaultInstance(), x + separator, y);
                } else if (this.code == 2) {
                    EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.byId(entry.getKey());
                    tooltipTitle = entityType.getDescription();
                    if (this.customStacks.containsKey(entry.getKey())) {
                        drawContext.renderItem(this.customStacks.get(entry.getKey()), x + separator, y);
                    } else {
                        drawContext.blit(RenderPipelines.GUI_TEXTURED,this.customImages.get(entry.getKey()), x + separator, y, 0, 0, 16, 16,16,16);
                    }

                } else if (this.code == 3) {
                    ItemStack stack = this.customStacks.get(entry.getKey());
                    var enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                    // Pega o primeiro encantamento do livro para o título
                    Holder<Enchantment> enchantment = enchantments.keySet().iterator().next();
                    int level = enchantments.getLevel(enchantment);
                    tooltipTitle = Enchantment.getFullname(enchantment, level);
                    drawContext.renderItem(stack, x + separator, y);
                }
                else if (this.code == 4) {
                    ItemStack stack = this.customStacks.get(entry.getKey());
                    var contents = stack.get(DataComponents.POTION_CONTENTS);

                    if (contents != null && contents.potion().isPresent()) {
                        // Pega o ID da poção (ex: "healing", "strong_strength")
                        String potionPath = contents.potion().get().unwrapKey().get().identifier().getPath();

                        // O segredo na 1.21.1: O nome da poção é composto pelo item + o efeito
                        // Ex: item.minecraft.potion.effect.healing
                        String translationKey = "item.minecraft.potion.effect." + potionPath;
                        MutableComponent translatedName = Component.translatable(translationKey);

                        // Se o nome vier vazio ou igual à chave (não traduzido), usa o fallback do item
                        if (translatedName.getString().equals(translationKey)) {
                            translatedName = (MutableComponent) stack.getHoverName();
                        }

                        // Adiciona a distinção visual que fizemos
                        if (potionPath.contains("strong")) {
                            tooltipTitle = translatedName.append(Component.literal(" II").withStyle(ChatFormatting.YELLOW));
                        } else if (potionPath.contains("long")) {
                            tooltipTitle = translatedName.append(Component.literal(" long").withStyle(ChatFormatting.AQUA));
                        } else {
                            tooltipTitle = translatedName;
                        }
                    } else {
                        tooltipTitle = stack.getHoverName();
                    }

                    drawContext.renderItem(stack, x + separator, y);
                }

                else {
                    tooltipTitle = Component.literal("");
                }

                if (!showTooltip && PlayerLevelSkillsScreen.isPointWithinBounds(x + separator, y, 16, 16, mouseX, mouseY)) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(tooltipTitle);
                    for (Map.Entry<Integer, Integer> restriction : entry.getValue().getSkillLevelRestrictions().entrySet()) {
                        tooltip.add(Component.nullToEmpty(LevelManager.SKILLS.get(restriction.getKey()).getText().getString() + " " + Component.translatable("text.levelz.gui.short_level", restriction.getValue()).getString()));
                    }
                    drawContext.setComponentTooltipForNextFrame(this.client.font, tooltip, mouseX, mouseY);
                    showTooltip = true;
                }
                separator += 18;
            }
        }
    }
}

