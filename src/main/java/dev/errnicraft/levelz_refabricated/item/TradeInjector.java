package dev.errnicraft.levelz_refabricated.item;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.init.ItemInit;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import java.util.Optional;

public class TradeInjector {

    public static void register() {

        TradeOfferHelper.registerVillagerOffers(VillagerProfession.CLERIC, 5, factories -> {
            factories.add((entity,level, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 64), // Preço: 64 Esmeraldas
                    new ItemStack(ItemInit.RARE_CANDY, 1),
                    3,
                    30,
                    0.05f
            ));
            factories.add((entity,level, random) -> new MerchantOffer(
                    new ItemCost(Items.EMERALD, 64),
                    Optional.of(new ItemCost(Items.DIAMOND, 32)),// Preço: 64 Esmeraldas
                    new ItemStack(ItemInit.STRANGE_POTION, 1),
                    1,
                    30,
                    0.05f
            ));
        });

        TradeOfferHelper.registerWanderingTraderOffers(builder -> {
            builder.pool(
                    Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "rare_candy_pool"),
                    1, // O trader vai pescar 1 oferta desta piscina
                    (entity,level, random) -> new MerchantOffer(
                            new ItemCost(Items.EMERALD, 32),
                            new ItemStack(ItemInit.RARE_CANDY, 1),
                            2,
                            15,
                            0.05f
                    )
            );
        });
    }
}

