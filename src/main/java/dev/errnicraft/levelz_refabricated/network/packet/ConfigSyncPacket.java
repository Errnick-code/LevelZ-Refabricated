package dev.errnicraft.levelz_refabricated.network.packet;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ConfigSyncPacket(
    float xpCostMultiplicator,
    int xpExponent,
    int xpBaseCost,
    int xpMaxCost,
    int overallMaxLevel,
    int vialMaxCapacity,
    int vialFillAmount
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ConfigSyncPacket> PACKET_ID =
        new CustomPacketPayload.Type<>(LevelZRefabricated.identifierOf("config_sync_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConfigSyncPacket> PACKET_CODEC =
        StreamCodec.ofMember((value, buf) -> {
            buf.writeFloat(value.xpCostMultiplicator());
            buf.writeInt(value.xpExponent());
            buf.writeInt(value.xpBaseCost());
            buf.writeInt(value.xpMaxCost());
            buf.writeInt(value.overallMaxLevel());
            buf.writeInt(value.vialMaxCapacity());
            buf.writeInt(value.vialFillAmount());
        }, buf -> new ConfigSyncPacket(
            buf.readFloat(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt()
        ));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
