package CoroUtil.componentAI.jobSystem;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import CoroUtil.componentAI.ICoroAI;
import CoroUtil.pathfinding.PFQueue;

/* JobInvade: For Omnipotent focused invasions */

//NOTE TO SELF: Uses player list for if no close target, huntRange for close frequently called distance based targetting 

public class JobInvade extends JobBase {
	
	public long huntRange = 32;
	public boolean omnipotent = true;
	
	public int retargetDelayCount = 0;
	public int retargetDelay = 20;
	public int retargetDist = 10;
	
	public JobInvade(JobManager jm) {
		super(jm);
		
		//let them be spawned for a bit before targetting
		retargetDelayCount = retargetDelay;
	}
	
	@Override
	public void tick() {
		
		if (retargetDelayCount > 0) retargetDelayCount--;
		
		jobHunter();
	}

	@Override //never called if avoid() returns false
	public void onLowHealth() {
		super.onLowHealth();
		/*if (hitAndRunDelay == 0 && ent.getDistanceToEntity(ent.lastFleeEnt) > 6F) {
			hitAndRunDelay = ent.cooldown_Ranged+1;
			ent.entityToAttack = ent.lastFleeEnt;
			if (ent.entityToAttack != null) ent.faceEntity(ent.entityToAttack, 180F, 180F);
		} else {
			ent.entityToAttack = null;
		}*/
	}
	
	@Override
	public boolean hookHit(DamageSource ds, int damage) {
		if (isEnemy(ds.getEntity())) {
			ai.entityToAttack = ds.getEntity();
		}
		return super.hookHit(ds, damage);
	}
	
	@Override
	public void setJobItems() {
		
		//Melee slot
		//ent.inventory.addItemStackToInventory(new ItemStack(Item.swordWood, 1));
		//Ranged slot
		
		int choice = ent.worldObj.rand.nextInt(2);
		if (choice == 0) {
			//ent.inventory.addItemStackToInventory(new ItemStack(mod_SdkGuns.itemGunAk47, 1));
			//ent.inventory.addItemStackToInventory(new ItemStack(mod_SdkGuns.itemBulletLight, 64));
		} else if (choice == 1) {
			//ent.inventory.addItemStackToInventory(new ItemStack(mod_SdkGuns.itemGunFlamethrower, 1));
			//ent.inventory.addItemStackToInventory(new ItemStack(mod_SdkGuns.itemOil, 64));
		}
	}
	
	@Override
	public boolean avoid(boolean actOnTrue) {
		return false;
	}
	
	@Override
	public boolean checkHunger() {
		
		return false;
	}
	
	protected void jobHunter() {
	
		
		
		//this whole function is crap, redo it bitch
		
		//a use for the states
		
		//responding to alert, so you know to cancel it if alert entity / active target is dead
		
		/*if (tryingToFlee && (onGround || isInWater())) {
			tryingToFlee = false;
			fleeFrom(lastFleeEnt);
		}*/
		
		
		
		//retargetDelay = 20;
		
		
		//if (true) return;
		
		//health = 8;
		/*if (health < getMaxHealth() * 0.75F) {
			avoid();
			if (rand.nextInt(5) == 0) entityToAttack = null;
		} else {*/
			//setJobState(EnumJobState.IDLE);
			Entity clEnt = null;
			
		
			if (/*ent.getEntityHealth() > ent.getMaxHealth() * 0.90F && */(ai.entityToAttack == null || ent.worldObj.rand.nextInt(20) == 0)) {
				boolean found = false;
				
				float closest = 9999F;
		    	List list = ent.worldObj.getEntitiesWithinAABBExcludingEntity(ent, ent.boundingBox.expand(huntRange, huntRange/2, huntRange));
		        for(int j = 0; j < list.size(); j++)
		        {
		            Entity entity1 = (Entity)list.get(j);
		            if(isEnemy(entity1))
		            {
		            	//if (((EntityLivingBase) entity1).canEntityBeSeen(ent)) {
		            		if (sanityCheck(entity1) && entity1 instanceof EntityLivingBase && ((EntityLivingBase)entity1).getHealth() > 0) {
		            			float dist = ent.getDistanceToEntity(entity1);
		            			if (dist < closest) {
		            				closest = dist;
		            				clEnt = entity1;
		            			}
			            		
			            		//found = true;
			            		//break;
		            		}
		            		//this.hasAttacked = true;
		            		//getPathOrWalkableBlock(entity1, 16F);
		            	//}
		            }
		        }
		        if (clEnt != null) {
		        	//THIS IS WHERE YOU SHOULD PUT LOGIC CHECK FOR EQUAL HEIGHT or something
		        	
		        	if (isSolidPath(clEnt)) {
		        		
		        		if (!((ICoroAI)ent).isBreaking()) {
		        			ai.huntTarget(clEnt, -1);
		        		}
		        		//cpw.mods.fml.common.FMLLog.info("huntTarget instant");
		        	} else {
		        		float dist = clEnt.getDistanceToEntity(ent);
		        		if (retargetDelayCount == 0 && ((dist < retargetDist && dist > 2F) || ai.entityToAttack == null)) {
		        			//Only retarget if they can be seen, to prevent weird long distance pf derps?
		        			
		        			if (ent.getNavigator().noPath() || ((EntityLivingBase) clEnt).canEntityBeSeen(ent)) {
		        				retargetDelayCount = retargetDelay;
		        				if (!((ICoroAI)ent).isBreaking()) {
		        					ai.huntTarget(clEnt);
		        				}
		        			}
		        			
		        		}
		        	}
		        } else {
		        	
		        }
		        /*if (!found) {
		        	setState(EnumKoaActivity.IDLE);
		        }*/
			} else {
				
				if (ai.entityToAttack != null) {
					
					float dist = ent.getDistanceToEntity(ai.entityToAttack);
					//ent.getLookHelper().setLookPositionWithEntity(ent.entityToAttack, 10.0F, (float)ent.getVerticalFaceSpeed());
					
					if (((ent.getNavigator().noPath()) && (retargetDelayCount == 0 && dist > 2F/* && ent.entityToAttack.getDistanceToEntity(ent) < retargetDist*/))/* && ent.getDistanceToEntity(ent.entityToAttack) > 5F*/) {
						retargetDelayCount = retargetDelay;
						if (!((ICoroAI)ent).isBreaking()) {
							if (PFQueue.getPath(ent, ai.entityToAttack, ai.maxPFRange)) {
								//cpw.mods.fml.common.FMLLog.info("huntTarget repath");
							}
						}
					}
				}
				
			}
			
			if (clEnt == null && ai.entityToAttack == null) {
				//GET PLAYER SINCE NO CLOSE TARGETS!!!!!
	        	EntityPlayer entP = getClosestPlayerToEntity(ent, -1F, false);
	        	if (entP != null && entP.getHealth() > 0) {
		        	if (ent.getNavigator().noPath()) {
		        		//cpw.mods.fml.common.FMLLog.info("huntTarget far");
	        			ai.huntTarget(entP);
	        		}
	        	}
			}
			
			//derp
			/*if (ent.entityToAttack == null && ent.rand.nextInt(6000) == 0) {
				ent.walkTo(ent, ent.homeX, ent.homeY, ent.homeZ, ent.maxPFRange, 600);
			}*/
			
		//}
		ent.prevHealth = ent.getHealth();
	}
	
	//copied from c_EnhAI, might be replacable with new isMovementSafe stuff
	public boolean isSolidPath(Entity var1) {
        return ent.canEntityBeSeen(var1) && (ent.getDistanceToEntity(var1) < 3.0F) && Math.abs(ent.posY - (double)ent.yOffset - (var1.posY - (double)var1.yOffset)) <= 2.5D;
    }
	
	public boolean sanityCheck(Entity target) {
		return true;
	}
	
}
