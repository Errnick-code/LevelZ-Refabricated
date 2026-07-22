package dev.errnicraft.levelz_refabricated.entity;

import dev.errnicraft.levelz_refabricated.access.ServerPlayerSyncAccess;
import dev.errnicraft.levelz_refabricated.init.EntityInit;
import dev.errnicraft.levelz_refabricated.network.packet.OrbPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LevelExperienceOrbEntity extends Entity {

    private int orbAge;
    private int health = 5;
    private int amount;
    private int pickingCount = 1;
    private Player target;
    private Map<Integer, Integer> clumpedMap;

    public LevelExperienceOrbEntity(Level world, double x, double y, double z, int amount) {
        this(EntityInit.LEVEL_EXPERIENCE_ORB, world);
        this.setPos(x, y, z);
        this.setYRot((float) (this.random.nextDouble() * 360.0));
        this.setDeltaMovement(
            (this.random.nextDouble() * 0.2 - 0.1) * 2.0,
            this.random.nextDouble() * 0.2 * 2.0,
            (this.random.nextDouble() * 0.2 - 0.1) * 2.0
        );
        this.amount = amount;
    }

    public LevelExperienceOrbEntity(EntityType<? extends LevelExperienceOrbEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    // Vanilla-matching gravity value
    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        boolean colliding = !this.level().noCollision(this.getBoundingBox());

        if (this.isEyeInFluid(FluidTags.WATER)) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.99F, Math.min(movement.y + 5.0E-4F, 0.06F), movement.z * 0.99F);
        } else if (!colliding) {
            this.applyGravity();
        }

        if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
            this.setDeltaMovement(
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2F,
                0.2F,
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
            );
        }
        if (this.target == null && !this.level().isClientSide() && colliding) {
            boolean nextColliding = !this.level().noCollision(this.getBoundingBox().move(this.getDeltaMovement()));
            if (nextColliding) {
                this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
                this.needsSync = true;
            }
        }

        if (this.tickCount % 20 == 1) {
            this.expensiveUpdate();
        }

        // Follow player logic — matches vanilla exactly
        if (this.target != null && (this.target.isSpectator() || this.target.isDeadOrDying())) {
            this.target = null;
        }
        if (this.target == null || this.target.distanceToSqr(this) > 64.0) {
            Player nearestPlayer = this.level().getNearestPlayer(this, 8.0);
            if (nearestPlayer != null && !nearestPlayer.isSpectator() && !nearestPlayer.isDeadOrDying()) {
                this.target = nearestPlayer;
            } else {
                this.target = null;
            }
        }
        if (this.target != null) {
            Vec3 delta = new Vec3(
                this.target.getX() - this.getX(),
                this.target.getY() + this.target.getEyeHeight() / 2.0 - this.getY(),
                this.target.getZ() - this.getZ()
            );
            double length = delta.lengthSqr();
            double power = 1.0 - Math.sqrt(length) / 8.0;
            this.setDeltaMovement(this.getDeltaMovement().add(delta.normalize().scale(power * power * 0.1)));
        }

        double fallSpeed = this.getDeltaMovement().y;
        this.move(MoverType.SELF, this.getDeltaMovement());

        // Vanilla friction: only XZ horizontal, not Y
        float friction = 0.98F;
        if (this.onGround()) {
            friction = this.level().getBlockState(this.blockPosition().below()).getBlock().getFriction() * 0.98F;
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(friction, 0.98F, friction));

        // Vanilla bounce: -0.4 factor, only when falling fast (not just any ground contact)
        if (this.verticalCollisionBelow && fallSpeed < -this.getGravity()) {
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, -fallSpeed * 0.4, this.getDeltaMovement().z));
        }

        ++this.orbAge;
        if (this.orbAge >= 6000) {
            this.discard();
            return;
        }

        // Pickup check — runs server-side in tick() rather than playerTouch(),
        // so we can pick ONE orb per player per tick, matching vanilla behaviour.
        // Vanilla does this in Player.aiStep(): it collects all EXPERIENCE_ORB
        // entities in the pickup area and calls touch() on only one random one.
        // Our entity type is not EntityType.EXPERIENCE_ORB, so vanilla skips us.
        // We replicate the same logic here on the orb side.
        if (!this.level().isClientSide() && this.orbAge >= 2) {
            AABB pickupArea = this.getBoundingBox().inflate(0.5, 0.0, 0.5);
            List<Player> players = this.level().getEntities(
                EntityTypeTest.forClass(Player.class),
                pickupArea,
                p -> !p.isSpectator() && !p.isDeadOrDying() && p.takeXpDelay == 0
            );
            if (!players.isEmpty()) {
                // Pick the closest player
                Player closest = players.get(0);
                double closestDist = closest.distanceToSqr(this);
                for (Player p : players) {
                    double d = p.distanceToSqr(this);
                    if (d < closestDist) { closestDist = d; closest = p; }
                }
                final Player winner = closest; // final copy for lambda capture
                winner.takeXpDelay = 2;
                winner.take(this, 1);
                getClumpedMap().forEach((value, amount) ->
                    ((ServerPlayerSyncAccess) winner).addLevelExperience(value * amount));
                this.discard();
            }
        }
    }

    private void expensiveUpdate() {
        if (this.level() instanceof ServerLevel) {
            List<LevelExperienceOrbEntity> list = this.level().getEntities(
                EntityTypeTest.forClass(LevelExperienceOrbEntity.class),
                this.getBoundingBox().inflate(0.5),
                this::isMergeable
            );
            for (LevelExperienceOrbEntity orb : list) {
                this.merge(orb);
            }
        }
    }

    public static void spawn(ServerLevel world, Vec3 pos, int amount) {
        while (amount > 0) {
            int i = LevelExperienceOrbEntity.roundToOrbSize(amount);
            amount -= i;
            if (LevelExperienceOrbEntity.wasMergedIntoExistingOrb(world, pos, i)) {
                continue;
            }
            world.addFreshEntity(new LevelExperienceOrbEntity(world, pos.x(), pos.y(), pos.z(), i));
        }
    }

    private static boolean wasMergedIntoExistingOrb(ServerLevel world, Vec3 pos, int amount) {
        AABB box = AABB.ofSize(pos, 1.0, 1.0, 1.0);
        int i = world.getRandom().nextInt(40);
        List<LevelExperienceOrbEntity> list = world.getEntities(
            EntityTypeTest.forClass(LevelExperienceOrbEntity.class),
            box,
            orb -> LevelExperienceOrbEntity.isMergeable(orb, i, amount)
        );
        if (!list.isEmpty()) {
            LevelExperienceOrbEntity orb = list.get(0);
            Map<Integer, Integer> clumpedMap = orb.getClumpedMap();
            orb.setClumpedMap(Stream.of(clumpedMap, Collections.singletonMap(amount, 1))
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum)));
            orb.pickingCount = clumpedMap.values().stream().reduce(Integer::sum).orElse(1);
            orb.orbAge = 0;
            return true;
        }
        return false;
    }

    private boolean isMergeable(LevelExperienceOrbEntity other) {
        return other.isAlive() && other != this;
    }

    private static boolean isMergeable(LevelExperienceOrbEntity orb, int seed, int amount) {
        return orb.isAlive();
    }

    private void merge(LevelExperienceOrbEntity other) {
        Map<Integer, Integer> otherMap = other.getClumpedMap();
        setClumpedMap(Stream.of(getClumpedMap(), otherMap)
            .flatMap(map -> map.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum)));
        this.pickingCount = getClumpedMap().values().stream().reduce(Integer::sum).orElse(1);
        this.orbAge = Math.min(this.orbAge, other.orbAge);
        other.discard();
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (this.isInvulnerableToBase(source)) return false;
        this.markHurt();
        this.health = (int)(this.health - amount);
        if (this.health <= 0) this.discard();
        return true;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        this.health = view.getIntOr("Health", 0);
        this.orbAge = view.getIntOr("Age", 0);
        this.amount = view.getIntOr("Value", 0);
        this.pickingCount = Math.max(view.getIntOr("Count", 1), 1);

        Map<Integer, Integer> map = new HashMap<>();
        if (view.contains("clumpedMap")) {
            ValueInput clumpedMapView = view.childOrEmpty("clumpedMap").childOrEmpty(null);
            for (String key : clumpedMapView.keys()) {
                try {
                    map.put(Integer.parseInt(key), clumpedMapView.getIntOr(key, 0));
                } catch (NumberFormatException ignored) {}
            }
        } else {
            map.put(this.amount, this.pickingCount);
        }
        setClumpedMap(map);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        view.putInt("Health", this.health);
        view.putInt("Age", this.orbAge);
        view.putInt("Value", this.amount);
        view.putInt("Count", this.pickingCount);

        ValueOutput map = view.child("clumpedMap");
        getClumpedMap().forEach((value, count) -> map.putInt(value + "", count));
    }

    @Override
    public void playerTouch(Player player) {
        // Pickup is handled in tick() to match vanilla's one-orb-per-tick behaviour.
        // playerTouch() is intentionally left empty.
    }

    public int getExperienceAmount() {
        return this.amount;
    }

    public int getOrbSize() {
        if (this.amount >= 2477) return 10;
        if (this.amount >= 1237) return 9;
        if (this.amount >= 617)  return 8;
        if (this.amount >= 307)  return 7;
        if (this.amount >= 149)  return 6;
        if (this.amount >= 73)   return 5;
        if (this.amount >= 37)   return 4;
        if (this.amount >= 17)   return 3;
        if (this.amount >= 7)    return 2;
        if (this.amount >= 3)    return 1;
        return 0;
    }

    public static int roundToOrbSize(int value) {
        if (value >= 2477) return 2477;
        if (value >= 1237) return 1237;
        if (value >= 617)  return 617;
        if (value >= 307)  return 307;
        if (value >= 149)  return 149;
        if (value >= 73)   return 73;
        if (value >= 37)   return 37;
        if (value >= 17)   return 17;
        if (value >= 7)    return 7;
        if (value >= 3)    return 3;
        return 1;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        ServerPlayNetworking.send(player, new OrbPacket(this, this.getX(), this.getY(), this.getZ()));
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    private Map<Integer, Integer> getClumpedMap() {
        if (this.clumpedMap == null) {
            this.clumpedMap = new HashMap<>();
            this.clumpedMap.put(this.amount, 1);
        }
        return this.clumpedMap;
    }

    private void setClumpedMap(Map<Integer, Integer> map) {
        this.clumpedMap = map;
        this.amount = getClumpedMap().entrySet().stream()
            .map(entry -> entry.getKey() * entry.getValue())
            .reduce(Integer::sum).orElse(1);
    }
}
