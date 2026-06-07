package dev.errnicraft.levelz_refabricated.network.packet;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record PlayerLevelSyncPacket(Map<UUID, Integer> levelMap) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerLevelSyncPacket> PACKET_ID =
            new CustomPacketPayload.Type<>(LevelZRefabricated.identifierOf("player_level_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLevelSyncPacket> PACKET_CODEC =
            StreamCodec.ofMember(
                    (pkt, buf) -> {
                        buf.writeInt(pkt.levelMap().size());
                        for (Map.Entry<UUID, Integer> entry : pkt.levelMap().entrySet()) {
                            buf.writeUUID(entry.getKey());
                            buf.writeInt(entry.getValue());
                        }
                    },
                    buf -> {
                        int size = buf.readInt();
                        Map<UUID, Integer> map = new HashMap<>(size);
                        for (int i = 0; i < size; i++) {
                            map.put(buf.readUUID(), buf.readInt());
                        }
                        return new PlayerLevelSyncPacket(map);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }
}
