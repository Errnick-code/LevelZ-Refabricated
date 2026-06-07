package dev.errnicraft.levelz_refabricated.access;


import dev.errnicraft.levelz_refabricated.network.packet.OrbPacket;

public interface OrbAccess {

    void onLevelExperienceOrbSpawn(OrbPacket packet);
}
