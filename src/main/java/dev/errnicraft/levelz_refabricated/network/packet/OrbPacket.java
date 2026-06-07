package dev.errnicraft.levelz_refabricated.network.packet;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.entity.LevelExperienceOrbEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OrbPacket(int entityId, double x, double y, double z, int experience)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OrbPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(LevelZRefabricated.MOD_ID, "orb_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OrbPacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeVarInt(pkt.entityId());
                        buf.writeDouble(pkt.x());
                        buf.writeDouble(pkt.y());
                        buf.writeDouble(pkt.z());
                        buf.writeShort(pkt.experience());
                    },
                    buf -> new OrbPacket(
                            buf.readVarInt(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readShort()
                    )
            );

    public OrbPacket(LevelExperienceOrbEntity orb, double x, double y, double z) {
        this(orb.getId(), x, y, z, orb.getExperienceAmount());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
