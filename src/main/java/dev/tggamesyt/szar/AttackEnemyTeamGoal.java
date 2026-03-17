// AttackEnemyTeamGoal.java
package dev.tggamesyt.szar;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.mob.PathAwareEntity;

public class AttackEnemyTeamGoal extends ActiveTargetGoal<LivingEntity> {

    public AttackEnemyTeamGoal(PathAwareEntity mob, String myTeam) {
        super(mob, LivingEntity.class, true, target -> {
            if (target instanceof TeamMember other) {
                return !other.getTeam().equals(myTeam);
            }
            return false;
        });
    }
}