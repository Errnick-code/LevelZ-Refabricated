package dev.errnicraft.levelz_refabricated.network.packet;

import dev.errnicraft.levelz_refabricated.LevelZRefabricated;
import dev.errnicraft.levelz_refabricated.level.restriction.PlayerRestriction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RestrictionPacket(RestrictionRecord blockRestrictions,
                                RestrictionRecord craftingRestrictions,
                                RestrictionRecord entityRestrictions,
                                RestrictionRecord itemRestrictions,
                                RestrictionRecord potionRestrictions,
                                RestrictionRecord miningRestrictions,
                                RestrictionRecord enchantmentRestrictions) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RestrictionPacket> PACKET_ID = new CustomPacketPayload.Type<>(LevelZRefabricated.identifierOf("restriction_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RestrictionPacket> PACKET_CODEC = StreamCodec.ofMember((value, buf) -> {
        value.blockRestrictions.write(buf);
        value.craftingRestrictions.write(buf);
        value.entityRestrictions.write(buf);
        value.itemRestrictions.write(buf);
        value.potionRestrictions.write(buf);
        value.miningRestrictions.write(buf);
        value.enchantmentRestrictions.write(buf);
    }, buf -> new RestrictionPacket(RestrictionRecord.read(buf), RestrictionRecord.read(buf), RestrictionRecord.read(buf), RestrictionRecord.read(buf), RestrictionRecord.read(buf), RestrictionRecord.read(buf), RestrictionRecord.read(buf)));

    public record RestrictionRecord(List<Integer> ids, List<PlayerRestriction> restrictions) {

        public void write(FriendlyByteBuf buf) {
            buf.writeInt(ids().size());
            for (Integer id : ids) {
                buf.writeInt(id);
            }
            buf.writeInt(restrictions().size());
            for (int i = 0; i < restrictions().size(); i++) {
                PlayerRestriction playerRestriction = restrictions().get(i);
                buf.writeInt(playerRestriction.getId());
                buf.writeInt(playerRestriction.getSkillLevelRestrictions().size());
                for (Map.Entry<Integer, Integer> entry : playerRestriction.getSkillLevelRestrictions().entrySet()) {
                    buf.writeInt(entry.getKey());
                    buf.writeInt(entry.getValue());
                }
            }
        }

        public static RestrictionRecord read(FriendlyByteBuf buf) {
            List<Integer> ids = new ArrayList<>();
            int idSize = buf.readInt();
            for (int i = 0; i < idSize; i++) {
                ids.add(buf.readInt());
            }
            List<PlayerRestriction> playerRestrictions = new ArrayList<>();
            int size = buf.readInt();
            for (int i = 0; i < size; i++) {
                int id = buf.readInt();
                int skillLevelSize = buf.readInt();
                Map<Integer, Integer> skillLevelRestrictions = new HashMap<>();
                for (int u = 0; u < skillLevelSize; u++) {
                    int skillId = buf.readInt();
                    int skillLevel = buf.readInt();
                    skillLevelRestrictions.put(skillId, skillLevel);
                }
                playerRestrictions.add(new PlayerRestriction(id, skillLevelRestrictions));
            }
            return new RestrictionRecord(ids, playerRestrictions);
        }

    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return PACKET_ID;
    }

}


