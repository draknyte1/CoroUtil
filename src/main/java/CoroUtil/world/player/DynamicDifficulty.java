package CoroUtil.world.player;

import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockSapling;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.oredict.OreDictionary;
import CoroUtil.config.ConfigDynamicDifficulty;
import CoroUtil.util.BlockCoord;
import CoroUtil.util.UtilPlayer;
import CoroUtil.world.WorldDirectorManager;
import CoroUtil.world.grid.chunk.ChunkDataPoint;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;

public class DynamicDifficulty {
	
	public static String dataPlayerServerTicks = "HW_dataPlayerServerTicks";
	public static String dataPlayerLastCacheEquipmentRating = "HW_dataPlayerLastCacheEquipmentRating";
	public static String dataPlayerHarvestOre = "HW_dataPlayerHarvestOre";
	public static String dataPlayerHarvestLog = "HW_dataPlayerHarvestLog";
	public static String dataPlayerHarvestRating = "HW_dataPlayerHarvestRating";
	
	private int tickRate = 20;
	
	public static HashMap<Integer, DamageLog> lookupEntToDamageLog = new HashMap<Integer, DamageLog>();

	public void tickServer(ServerTickEvent event) {
		World world = DimensionManager.getWorld(0);
		if (world != null) {
			if (world.getTotalWorldTime() % tickRate == 0) {
				for (Object player : world.playerEntities) {
					if (player instanceof EntityPlayer) {
						tickPlayer((EntityPlayer)player);
					}
				}
			}
		}
	}
	
	public void tickPlayer(EntityPlayer player) {
		long ticksPlayed = player.getEntityData().getLong(dataPlayerServerTicks);
		ticksPlayed += 20;
		//3 hour start debug
		//ticksPlayed = 20*60*60*3;
		player.getEntityData().setLong(dataPlayerServerTicks, ticksPlayed);
		
		
	}
	

	
	public static float getDifficultyScaleAverage(World world, EntityPlayer player, BlockCoord pos) {
		float difficultyPos = getDifficultyScaleForPos(world, pos);
		float difficultyPlayerEquipment = getDifficultyScaleForPlayerEquipment(player);
		float difficultyPlayerServerTime = getDifficultyScaleForPlayerServerTime(player);
		float val = (difficultyPos + difficultyPlayerEquipment + difficultyPlayerServerTime) / 3F;
		val = Math.round(val * 1000F) / 1000F;
		if (val > 1F) val = 1F;
		return val;
	}
	
	public static float getDifficultyScaleForPlayerServerTime(EntityPlayer player) {
		long maxServerTime = ConfigDynamicDifficulty.difficulty_MaxTicksOnServer;
		long curServerTime = player.getEntityData().getLong(dataPlayerServerTicks);
		return MathHelper.clamp_float((float)curServerTime / (float)maxServerTime, 0F, 1F);
	}
	
	public static float getDifficultyScaleForPlayerEquipment(EntityPlayer player) {
		int curRating = 0;
		if (player.getEntityData().hasKey(dataPlayerLastCacheEquipmentRating)) {
			if (player.worldObj.getTotalWorldTime() % 200 == 0) {
				curRating = UtilPlayer.getPlayerRating(player);
				player.getEntityData().setInteger(dataPlayerLastCacheEquipmentRating, curRating);
			} else {
				curRating = player.getEntityData().getInteger(dataPlayerLastCacheEquipmentRating);
			}
		} else {
			curRating = UtilPlayer.getPlayerRating(player);
			player.getEntityData().setInteger(dataPlayerLastCacheEquipmentRating, curRating);
		}
		
		int bestRating = UtilPlayer.getBestPlayerRatingPossible();
		
		//allow a scale value over 1F, means theres equipment in play beyond vanilla stuff, or i miscalculated some things
		return (float)curRating / (float)bestRating;
	}
	
	public static float getDifficultyScaleForHealth(EntityPlayer player) {
		float baseMax = 20F;
		float curMax = player.getMaxHealth();
		float scale = curMax / baseMax;
		return scale;
	}
	
	public static float getDifficultyScaleForPos(World world, BlockCoord pos) {
		/**
		 * 1 chunk calc
		 */
		/*Chunk chunk = world.getChunkFromBlockCoords(pos);
		if (chunk != null) {
			long inhabTime = chunk.getInhabitedTime();
			float scale = convertInhabTimeToDifficultyScale(inhabTime);
			return scale;
			
		}
		return 0F;*/
		
		/**
		 * average radius calc
		 */
		int chunkRange = 3;
		int chunkX = pos.getX() / 16;
		int chunkZ = pos.getZ() / 16;
		int count = 0;
		long totalTime = 0;
		for (int x = chunkX - chunkRange; x < chunkX + chunkRange; x++) {
			for (int z = chunkZ - chunkRange; z < chunkZ + chunkRange; z++) {
				BlockCoord checkPos = new BlockCoord(chunkX * 16 + 8, 128, chunkZ * 16 + 8);
				if (world.checkChunksExist(checkPos.posX, checkPos.posY, checkPos.posZ, checkPos.posX, checkPos.posY, checkPos.posZ)) {
					Chunk chunk = world.getChunkFromBlockCoords(checkPos.posX, checkPos.posZ);
					if (chunk != null) {
						totalTime += chunk.inhabitedTime;
						count++;
					}
				}
			}
		}
		long averageTime = 0;
		if (count > 0) {
			averageTime = totalTime / count;
		}
		
		float scale = convertInhabTimeToDifficultyScale(averageTime);
		return scale;
		
		/**
		 * best chunk count
		 */
		/*int chunkRange = 4;
		int chunkX = pos.getX() / 16;
		int chunkZ = pos.getZ() / 16;
		//int count = 0;
		long bestTime = 0;
		for (int x = chunkX - chunkRange; x < chunkX + chunkRange; x++) {
			for (int z = chunkZ - chunkRange; z < chunkZ + chunkRange; z++) {
				BlockCoord checkPos = new BlockCoord(chunkX * 16 + 8, 128, chunkZ * 16 + 8);
				if (world.isBlockLoaded(checkPos)) {
					Chunk chunk = world.getChunkFromBlockCoords(checkPos);
					if (chunk != null) {
						if (chunk.getInhabitedTime() > bestTime) {
							bestTime = chunk.getInhabitedTime();
						}
					}
				}
			}
		}
		//long averageTime = bestTime / count;
		
		float scale = convertInhabTimeToDifficultyScale(bestTime);
		return scale;*/
	}
	
	/**
	 * 
	 * Returns value between 0 and 1 based on configured values
	 * 
	 * @param inhabTime
	 * @return
	 */
	public static float convertInhabTimeToDifficultyScale(long inhabTime) {
		float scale = (float)inhabTime / (float)ConfigDynamicDifficulty.difficulty_MaxTicksInChunk;
		return scale;
	}
	
	public static void handleHarvest(HarvestDropsEvent event) {
		if (event.harvester != null) {
			if (event.world.playerEntities.contains(event.harvester)) {
				
				NBTTagCompound nbt = event.harvester.getEntityData();//WorldDirectorMultiDim.getPlayerNBT(CoroUtilEntity.getName(event.harvester));
				if (event.block instanceof BlockOre) {
					int curVal = nbt.getInteger(dataPlayerHarvestOre);
					curVal++;
					nbt.setInteger(dataPlayerHarvestOre, curVal);
					//System.out.println("increment!");
				} else if (event.block instanceof BlockLog) {
					int curVal = nbt.getInteger(dataPlayerHarvestLog);
					curVal++;
					nbt.setInteger(dataPlayerHarvestLog, curVal);
				}
				
				/*float curVal = nbt.getFloat(dataPlayerHarvestRating);
				curVal += getBlockImportanceValue(event.block);
				nbt.setFloat(dataPlayerHarvestRating, curVal);*/
				increaseInvadeRating(event.harvester, getBlockImportanceValue(event.block));
				
				//System.out.println("harvested block for " + event.harvester.username + " - " + event.block);
			}
		}
	}
	
	public static void increaseInvadeRating(EntityPlayer parPlayer, float parVal) {
		NBTTagCompound nbt = parPlayer.getEntityData();//WorldDirectorMultiDim.getPlayerNBT(CoroUtilEntity.getName(parPlayer));
		float curVal = nbt.getFloat(dataPlayerHarvestRating);
		curVal += parVal;
		nbt.setFloat(dataPlayerHarvestRating, curVal);
		
		//System.out.println("curVal: " + curVal);
	}
	
	public static void decreaseInvadeRating(EntityPlayer parPlayer, float parVal) {
		NBTTagCompound nbt = parPlayer.getEntityData();//WorldDirectorMultiDim.getPlayerNBT(CoroUtilEntity.getName(parPlayer));
		float curVal = nbt.getFloat(dataPlayerHarvestRating);
		curVal -= parVal;
		nbt.setFloat(dataPlayerHarvestRating, curVal);
	}
	
	public static float getHarvestRatingInvadeThreshold() {
		return 30F;
	}
	
	public static boolean isInvadeable(EntityPlayer parPlayer) {
		return parPlayer.getEntityData().getFloat(dataPlayerHarvestRating) >= getHarvestRatingInvadeThreshold();
	}
	
	public static float getBlockImportanceValue(Block block) {
		
		boolean test = false;
		if (test) {
			System.out.println("TEST INVADE IS ON!");
			return 30;
		}
		
		float scaleBase = 1F;
		float defaultIron = scaleBase * 0.3F;
		
		if (block instanceof BlockLog) {
			return scaleBase * 0.1F;
		} else if (block instanceof BlockSapling) {
			return scaleBase * 0.3F;
		} else if (block instanceof BlockOre) {
			if (block == Blocks.coal_ore) {
				return scaleBase * 0.2F;
			} else if (block == Blocks.iron_ore) {
				return defaultIron;
			} else if (block == Blocks.gold_ore) {
				return scaleBase * 0.4F;
			} else if (block == Blocks.lit_redstone_ore || block == Blocks.redstone_ore) {
				return scaleBase * 0.5F;
			} else if (block == Blocks.lapis_ore) {
				return scaleBase * 0.6F;
			} else if (block == Blocks.diamond_ore) {
				return scaleBase * 1F;
			} else if (block == Blocks.emerald_ore) {
				return scaleBase * 1.2F;
			} else {
				return defaultIron;
			}
		} else if (OreDictionary.getOres(Block.blockRegistry.getNameForObject(block)).size() > 0) {
			return defaultIron;
		} else {
			return 0;
		}
	}
	
	public static EntityPlayer getBestPlayerForArea(World world, BlockCoord pos) {
		
		EntityPlayer player = world.getClosestPlayer(pos.posX, pos.posY, pos.posZ, -1);
		
		return player;
	}
	
	public static void logDamage(LivingHurtEvent event) {
		if (event.entity.worldObj.isRemote) return;
		if (ConfigDynamicDifficulty.trackChunkData) {
			
			Entity ent = event.entity;
			World world = ent.worldObj;
			
			if (ent instanceof IMob && ent instanceof EntityCreature) {
				EntityCreature entC = (EntityCreature) ent;
				
				//dont log common occuring damages, sun burning, random wall glitching
				if (event.source == DamageSource.inWall || 
						event.source == DamageSource.inFire || 
						event.source == DamageSource.onFire || 
						event.source == DamageSource.drown/* || event.source == DamageSource.lava*/) {
					return;
				}
				
				
				
				DamageLog log = null;
				if (!lookupEntToDamageLog.containsKey(ent.getEntityId())) {
					log = new DamageLog(entC);
					lookupEntToDamageLog.put(ent.getEntityId(), log);
				} else {
					int lastLogTimeThreshold = 20*5;
					log = lookupEntToDamageLog.get(ent.getEntityId());
					if (log.getLastLogTime() + lastLogTimeThreshold < world.getTotalWorldTime()) {
						logToChunk(log);
						log.cleanup();
						log = new DamageLog(entC);
						lookupEntToDamageLog.put(ent.getEntityId(), log);
					} else {
						
					}
				}
				
				float damageToLog = event.ammount;
				if (log.getLastDamage() > 0) {
					long timeDiff = world.getTotalWorldTime() - log.getLastLogTime();
					float timeDiffSeconds = (float)timeDiff / 20F;
					if (timeDiff > 0) {
						float damage = log.getLastDamage() / timeDiffSeconds;
						
						log.getListDPSs().add(damage);
						
						System.out.println("dps log: " + damage + " new Damage: " + event.ammount + " tickDiff: " + timeDiff + " source: " + event.source.damageType + " ID: " + ent.getEntityId());
						
						
					} else {
						//if no time passed, just add last entry onto current entry
						damageToLog += log.getLastDamage();
					}
					
					
				}
				
				log.setLastDamage(damageToLog);
				log.setLastLogTime(world.getTotalWorldTime());
			}
			
			
		}
		
	}
	
	public static void logDeath(LivingDeathEvent event) {
		if (event.entity.worldObj.isRemote) return;
		if (ConfigDynamicDifficulty.trackChunkData) {
			
			Entity ent = event.entity;
			World world = ent.worldObj;
			
			if (ent instanceof IMob && ent instanceof EntityCreature) {
				if (lookupEntToDamageLog.containsKey(ent.getEntityId())) {
					DamageLog log = lookupEntToDamageLog.get(ent.getEntityId());
					logToChunk(log);
					log.cleanup();
					lookupEntToDamageLog.remove(ent.getEntityId());
				}
				
			}
			
		}
	}
	
	public static void logToChunk(DamageLog log) {
		int maxShortTermSize = 50;
		int maxLongTermSize = 50;
		int recalcRate = 20*2;
		
		EntityCreature ent = log.getEnt();
		World world = ent.worldObj;
		int chunkX = MathHelper.floor_double(ent.posX / 16);
		int chunkZ = MathHelper.floor_double(ent.posZ / 16);
		ChunkDataPoint cdp = WorldDirectorManager.instance().getChunkDataGrid(world).getChunkData(chunkX, chunkZ);
		
		if (log.getListDPSs().size() == 0 && log.getLastDamage() > 0) {
			//add an insta kill dps that assumes can be done every half second
			float instaKillDPSCalc = log.getLastDamage() * 2;
			log.getListDPSs().add(instaKillDPSCalc);
		}
		
		if (log.getListDPSs().size() > 0) {
			float avgDPS = 0;
			for (float val : log.getListDPSs()) {
				avgDPS += val;
			}
			avgDPS /= log.getListDPSs().size();
			cdp.listDPSAveragesShortTerm.add(avgDPS);
		}
		
		//trim list
		if (cdp.listDPSAveragesShortTerm.size() > maxShortTermSize) {
			cdp.listDPSAveragesShortTerm.remove(0);
		}
		
		//if time to do a full recalc
		if (cdp.lastDPSRecalc + recalcRate < world.getTotalWorldTime()) {
			
			if (cdp.listDPSAveragesShortTerm.size() > 0) {
				float avgDPS2 = 0;
				for (float val : cdp.listDPSAveragesShortTerm) {
					avgDPS2 += val;
				}
				avgDPS2 /= cdp.listDPSAveragesShortTerm.size();
				cdp.listDPSAveragesLongTerm.add(avgDPS2);
			}
			if (cdp.listDPSAveragesLongTerm.size() > maxLongTermSize) {
				cdp.listDPSAveragesLongTerm.remove(0);
			}
			
			if (cdp.listDPSAveragesLongTerm.size() > 0) {
				float avgDPS3 = 0;
				for (float val : cdp.listDPSAveragesLongTerm) {
					avgDPS3 += val;
				}
				avgDPS3 /= cdp.listDPSAveragesLongTerm.size();
				
				cdp.averageDPS = avgDPS3;
				
				System.out.println("average of the average of the average: " + avgDPS3);
				
				if (avgDPS3 > 0) {
					
				} else {
					System.out.println("wat");
				}
			}
			
			cdp.lastDPSRecalc = world.getTotalWorldTime();
		}
	}
	
}