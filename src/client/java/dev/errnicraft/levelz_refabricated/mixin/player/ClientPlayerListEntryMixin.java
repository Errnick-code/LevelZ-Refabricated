package dev.errnicraft.levelz_refabricated.mixin.player;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.PlayerInfo;
import dev.errnicraft.levelz_refabricated.access.ClientPlayerListAccess;
import dev.errnicraft.levelz_refabricated.level.LevelManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(PlayerInfo.class)
public abstract class ClientPlayerListEntryMixin implements ClientPlayerListAccess {

    @Shadow
    public abstract GameProfile getProfile();

    @Override
    public int getLevel() {
        return LevelManager.PLAYER_LEVELS.getOrDefault(getProfile().id(), 0);
    }

    @Override
    public void setLevel(int level) {
        LevelManager.PLAYER_LEVELS.put(getProfile().id(), level);
    }
}
