package dev.errnicraft.levelz_refabricated.network.packet;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record PlayerSkillSyncPacket(List<Integer> playerSkillIds, List<Integer> playerSkillLevels) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerSkillSyncPacket> PACKET_ID = new CustomPacketPayload.Type<>(LevelZRefabricated.identifierOf("player_skill_sync_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerSkillSyncPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeCollection(value.playerSkillIds, FriendlyByteBuf::writeInt);
        buf.writeCollection(value.playerSkillLevels, FriendlyByteBuf::writeInt);
    }, buf -> new PlayerSkillSyncPacket(buf.readList(FriendlyByteBuf::readInt), buf.readList(FriendlyByteBuf::readInt)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}