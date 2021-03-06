package CoroUtil.bt.nodes;

import java.util.List;
import java.util.Random;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.Vec3;
import CoroUtil.bt.AIBTAgent;
import CoroUtil.bt.Behavior;
import CoroUtil.bt.BlackboardBase;
import CoroUtil.bt.EnumBehaviorState;
import CoroUtil.bt.IBTAgent;
import CoroUtil.bt.selector.Selector;

public class AttackMelee extends Selector {

	//0 = nothing to attack, 1 = attacking, 2 = sanity check says no
	
	public IBTAgent entInt;
	public EntityLiving ent;
	public BlackboardBase blackboard;
	
	public float attackRange = 16;
	public float attackRate = 5;
	
	public float attackCooldown = 0;
	
	public AttackMelee(Behavior parParent, IBTAgent parEnt, BlackboardBase parBB, float parRange, float parRate) {
		super(parParent);
		blackboard = parBB;
		entInt = parEnt;
		ent = (EntityLiving)parEnt;
		attackRange = parRange;
		attackRate = parRate;
	}
	
	public boolean sanityCheck(Entity target) {
		/*if (ent.getHealth() < ent.getMaxHealth() / 4F * 1.5F) {
			return false;
		}*/
		return true;
	}

	@Override
	public EnumBehaviorState tick() {
		
		if (attackCooldown > 0) attackCooldown--;
		
		Entity target = blackboard.getTarget();
		
		if (target != null) {
			double dist = ent.getDistanceToEntity(target);
			
			if (attackCooldown <= 0) {
				if (dist < attackRange) {
					attackMelee(target);
					return super.tick();
				}
			}
		}
		
		if (children.size() > 0) return children.get(0).tick();
		
		return super.tick();
	}
	
	public void attackMelee(Entity parTarget) {
		//cpw.mods.fml.common.FMLLog.info("melee attack - " + ent);
		attackCooldown = attackRate;
		parTarget.attackEntityFrom(new EntityDamageSource("mob", ent), 10);
		
	}
	
}
