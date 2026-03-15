package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BulletEntity extends ThrownItemEntity {

    private static final float BASE_DAMAGE = 13.0F;
    private static final float PIERCE_BREAK_THRESHOLD = 0.4F;

    private float pierceValue = 1.0F;
    private int stillTicks = 0;
    private double lastX, lastY, lastZ;

    public BulletEntity(EntityType<? extends BulletEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);
    }

    public BulletEntity(World world, LivingEntity owner) {
        super(Szar.BULLET, owner, world);
        this.setNoGravity(true);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    public void tick() {
        super.tick();

        if (!getWorld().isClient) {
            double dx = getX() - lastX;
            double dy = getY() - lastY;
            double dz = getZ() - lastZ;
            double movedSq = dx * dx + dy * dy + dz * dz;

            if (movedSq < 0.0001) {
                stillTicks++;
                if (stillTicks >= 3) {
                    discard();
                    return;
                }
            } else {
                stillTicks = 0;
            }

            lastX = getX();
            lastY = getY();
            lastZ = getZ();
        }
    }

    @Override
    protected Item getDefaultItem() {
        return Szar.BULLET_ITEM;
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        Entity target = hit.getEntity();
        Entity owner = getOwner();

        if (owner instanceof LivingEntity livingOwner) {
            DamageSource source = new DamageSource(
                    getWorld().getRegistryManager()
                            .get(RegistryKeys.DAMAGE_TYPE)
                            .entryOf(Szar.BULLET_DAMAGE),
                    this,
                    livingOwner
            );
            // Damage scaled by remaining pierce value
            target.damage(source, BASE_DAMAGE * pierceValue);
        }

        // Don't discard — bullet continues through entities
        // But reduce pierce value a bit for entity hits
        pierceValue -= 0.3F;
        if (pierceValue <= 0) {
            discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        if (getWorld().isClient) return;

        BlockPos blockPos = hit.getBlockPos();
        BlockState state = getWorld().getBlockState(blockPos);
        Vec3d pos = hit.getPos();
        Direction face = hit.getSide();

        float resistance = state.getBlock().getBlastResistance();

        if (!state.isAir()) {
            pierceValue -= resistance;
        }

        if (pierceValue <= 0) {
            // Bullet stopped — spawn impact and discard
            spawnImpact(pos, face);
            discard();
            return;
        }

        if (resistance < PIERCE_BREAK_THRESHOLD && !state.isAir()) {
            // Break the block
            if (getWorld() instanceof ServerWorld serverWorld) {
                // Play break sound
                getWorld().playSound(
                        null,
                        blockPos,
                        state.getSoundGroup().getBreakSound(),
                        SoundCategory.BLOCKS,
                        1.0F,
                        1.0F
                );
                serverWorld.breakBlock(blockPos, true, getOwner());
            }
            // Bullet continues — don't call super, don't discard
        } else {
            // Block too strong to break — bullet stops here
            spawnImpact(pos, face);
            discard();
        }
    }

    private void spawnImpact(Vec3d pos, Direction face) {
        if (!(getWorld() instanceof ServerWorld serverWorld)) return;

        serverWorld.spawnParticles(ParticleTypes.SMOKE,
                pos.x, pos.y, pos.z, 5, 0.05, 0.05, 0.05, 0.02);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeEnumConstant(face);

        PlayerLookup.tracking(this).forEach(player ->
                ServerPlayNetworking.send(player, Szar.BULLET_IMPACT, buf)
        );
    }
}