package dev.tggamesyt.szar;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class BulletEntity extends ThrownItemEntity {

    public BulletEntity(EntityType<? extends BulletEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true); // bullets fly straight
    }

    public BulletEntity(World world, LivingEntity owner) {
        super(Szar.BULLET, owner, world);
        this.setNoGravity(true);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    @Override
    protected Item getDefaultItem() {
        return Szar.AK_AMMO; // used by FlyingItemEntityRenderer
    }

    @Override
    protected void onEntityHit(EntityHitResult hit) {
        Entity target = hit.getEntity();
        Entity owner = getOwner();

        if (owner instanceof LivingEntity livingOwner) {
            target.damage(
                    getWorld().getDamageSources().mobProjectile(this, livingOwner),
                    13.0F
            );
        }

        discard();
    }
}
