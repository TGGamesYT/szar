package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BulletEntity extends ProjectileEntity {

    public BulletEntity(EntityType<? extends BulletEntity> type, World world) {
        super(type, world);
    }

    public BulletEntity(World world, LivingEntity owner) {
        super(Szar.BULLET, world);
        this.setOwner(owner);
        this.setPosition(
                owner.getX(),
                owner.getEyeY() - 0.1,
                owner.getZ()
        );
    }

    @Override
    protected void initDataTracker() {}

    @Override
    public void tick() {
        super.tick();

        Vec3d velocity = this.getVelocity();
        this.setVelocity(velocity.multiply(1.02)); // fast

        HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
        if (hit.getType() != HitResult.Type.MISS) {
            onCollision(hit);
        }

        if (this.age > 60) discard();
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        Entity target = hit.getEntity();
        Entity owner = getOwner();

        target.damage(
                getWorld().getDamageSources().playerAttack((PlayerEntity) owner),
                6.0F
        );

        discard();
    }

    @Override
    protected void onCollision(HitResult hit) {
        if (!getWorld().isClient) discard();
    }
}
