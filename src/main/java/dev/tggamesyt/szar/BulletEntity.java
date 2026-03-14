package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BulletEntity extends ThrownItemEntity {

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
                if (stillTicks >= 3) { // discard after 3 ticks of no movement
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
            target.damage(source, 13.0F);
        }

        discard();
    }

    @Override
    protected void onBlockHit(net.minecraft.util.hit.BlockHitResult hit) {
        super.onBlockHit(hit);
        // Use exact hit position + nudge along face normal to sit on surface
        Vec3d pos = hit.getPos();
        Direction face = hit.getSide();
        spawnImpact(pos, face);
        discard();
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