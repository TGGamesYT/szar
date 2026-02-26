package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

import static dev.tggamesyt.szar.Szar.*;

public class FaszItem extends BlockItem {

    private static final int MAX_CHARGE_CLICKS = 4;
    private static final int COOLDOWN_TICKS = 1; // 1 tick
    private static final String BURST_KEY = "BurstCount";
    private static final Random RANDOM = new Random();
    private static final ItemStack CNDM = new ItemStack(Szar.CNDM);

    public FaszItem(Block block, Settings settings) {
        super(block, settings.maxDamage(MAX_CHARGE_CLICKS));
    }

    // Allow sneak-place
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() != null && context.getPlayer().isSneaking()) {
            return ActionResult.SUCCESS;
        }
        return ActionResult.FAIL;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (hand != Hand.MAIN_HAND) return TypedActionResult.success(stack);

        if (world.isClient) return TypedActionResult.success(stack);

        boolean isCreative = user.isCreative();

        int damage = stack.getDamage();
        int maxDamage = stack.getMaxDamage();
        NbtCompound nbt = stack.getOrCreateNbt();
        int burstCount = nbt.getInt(BURST_KEY);

        // In creative, ignore charging/cooldown
        if (isCreative) {
            if (user.getOffHandStack().getItem() == Szar.CNDM) {user.getOffHandStack().decrement(1);}
            spawnParticlesAndDamage((ServerWorld) world, user);
            return TypedActionResult.success(stack);
        }

        // Charging phase
        if (damage < maxDamage) {
            stack.setDamage(damage + 1);
            return TypedActionResult.success(stack);
        }

// Burst phase (after full charge)
        int burstClicks = nbt.getInt("BurstClicks"); // NEW NBT counter for burst clicks
        if (burstClicks < 4) { // do 4 burst clicks
            spawnParticlesAndDamage((ServerWorld) world, user);

            // Optional: handle offhand special item
            if (user.getOffHandStack().getItem() == Szar.CNDM) {
                user.dropStack(new ItemStack(Szar.WHITE_LIQUID));
                user.getOffHandStack().decrement(1);
            }

            burstClicks++;
            nbt.putInt("BurstClicks", burstClicks);

            // After 4 burst clicks → cooldown + reset
            if (burstClicks >= 4) {
                user.getItemCooldownManager().set(this, COOLDOWN_TICKS);
                stack.setDamage(0);
                nbt.putInt("BurstClicks", 0);
            }

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.success(stack);
    }

    private void spawnParticlesAndDamage(ServerWorld world, PlayerEntity user) {
        var lookVec = user.getRotationVec(1.0F);
        double px = user.getX() + lookVec.x * 2;
        double py = user.getBodyY(0.5);
        double pz = user.getZ() + lookVec.z * 2;

        // Spawn particles
        world.spawnParticles(
                new BlockStateParticleEffect(ParticleTypes.BLOCK, Szar.FASZ_BLOCK.getDefaultState()),
                px, py, pz,
                20,
                0.3, 0.3, 0.3,
                0.05
        );

        // Damage ANY entity whose hitbox contains the particle
        world.getEntitiesByClass(Entity.class, user.getBoundingBox().expand(2), e -> e != user).forEach(e -> {
            if (e.getBoundingBox().contains(px, py, pz)) {
                if (e instanceof LivingEntity living) {
                    // Always deal half a heart
                    RegistryEntry<DamageType> radiationEntry = SERVER.getRegistryManager()
                            .get(RegistryKeys.DAMAGE_TYPE)
                            .getEntry(FCK_DAMAGE)
                            .orElseThrow(() -> new IllegalStateException("FCK DamageType not registered!"));
                    living.damage(new DamageSource(radiationEntry, user), 1.0F);

                    // If the entity is a player → apply special effect logic
                    if (living instanceof PlayerEntity target) {
                        int chance = 5; // 1/5 default
                        ItemStack offhand = user.getOffHandStack();
                        if (!offhand.isEmpty() && offhand.isOf(CNDM.getItem())) {
                            chance = 100; // 1/100 if special offhand
                        }

                        if (RANDOM.nextInt(chance) == 0) {
                            // Apply status effect
                            target.addStatusEffect(new StatusEffectInstance(Szar.PREGNANT, 20 * 60 * 20));
                            // If special offhand → break 1 item
                            if (chance == 100) offhand.decrement(1);
                        }
                    }
                }
            }
        });
    }
}