package dev.errnicraft.levelz_refabricated.mixin.misc;

import dev.errnicraft.levelz_refabricated.util.BonusHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {

    @ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40))
    private int customTooExpensiveLimit(int constant) {
        // Aqui acessamos o jogador (o Minecraft.getInstance().player)
        // Se ele tiver o bônus, aumentamos o limite visual para 1000
        if (BonusHelper.anvilXpCapBonus(Minecraft.getInstance().player)) {
            return Integer.MAX_VALUE;
        }
        return constant; // Para os outros, continua aparecendo "Muito Caro" no 40
    }
}

