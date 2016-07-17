package ReplaceBlock;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;

@Mod(modid = ReplaceBlock.MOD_ID,
        name = ReplaceBlock.MOD_NAME,
        version = ReplaceBlock.MOD_VERSION,
        dependencies = ReplaceBlock.MOD_DEPENDENCIES,
        useMetadata = true,
        acceptedMinecraftVersions = ReplaceBlock.MOD_MC_VERSION)
public class ReplaceBlock {
    public static final String MOD_ID = "ReplaceBlock";
    public static final String MOD_NAME = "ReplaceBlock";
    public static final String MOD_VERSION = "@VERSION@";
    public static final String MOD_DEPENDENCIES = "required-after:Forge@[12.17.0,)";
    public static final String MOD_MC_VERSION = "[1.9,1.10.99]";

    private static String[] targetBlockID;
    private static String replaceBlockID;
    private static ResourceLocation replaceBlockRL;
    private static int targetYposMax;
    private static int targetYposMin;
    private static int chunkWidely;

    private Chunk lastChunk = null;
    private static final int CHUNK_WIDE = 16;
    protected ArrayList<Integer> ids = new ArrayList<Integer>();

    public class LivingUpdateHook {
        @SubscribeEvent
        public void LivingUpdate(LivingUpdateEvent event) {
            if (!event.getEntityLiving().worldObj.isRemote) {
                int posX = (int) Math.floor(event.getEntityLiving().posX);
                int posZ = (int) Math.floor(event.getEntityLiving().posZ);
                int posY = (int) Math.floor(event.getEntityLiving().posY);
                Chunk chunk = event.getEntityLiving().worldObj.getChunkFromBlockCoords(
                        new BlockPos(event.getEntityLiving().posX, event.getEntityLiving().posY, event.getEntityLiving().posZ));
                if (chunk != lastChunk && targetYposMax + chunkWidely >= posY && posY >= targetYposMin - chunkWidely) {
                    lastChunk = chunk;
                    int chunkX = chunk.xPosition * CHUNK_WIDE;
                    int chunkZ = chunk.zPosition * CHUNK_WIDE;
                    for (int x = chunkX - chunkWidely; x < chunkX + CHUNK_WIDE + chunkWidely; x++) {
                        for (int z = chunkZ - chunkWidely; z < chunkZ + CHUNK_WIDE + chunkWidely; z++) {
                            for (int y = targetYposMax; y >= targetYposMin; y--) {
                                BlockPos blockPos = new BlockPos(x, y, z);
                                if (this.isTargetBlock(event.getEntityLiving().worldObj.getBlockState(blockPos).getBlock())) {
                                    //minecraft.getIntegratedServer().worldServerForDimension(minecraft.thePlayer.dimension).setBlock(x, y, z, replaceBlockID, 0, 3);
                                    Block block = Block.REGISTRY.getObject(replaceBlockRL);
                                    event.getEntityLiving().worldObj.setBlockState(blockPos, block.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }

        private boolean isTargetBlock(Block block) {
            for (String targetId : targetBlockID) {
                if (targetId.equals(block.getRegistryName().toString())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        targetBlockID = config.get(Configuration.CATEGORY_GENERAL, "targetBlockNames", new String[]{"minecraft:dirt", "minecraft:bedrock", "minecraft:gravel"}).getStringList();
        replaceBlockID = config.get(Configuration.CATEGORY_GENERAL, "replaceBlockName", "minecraft:stone", "ReplaceBlockName: (format)ModId:UniqueName").getString();
        targetYposMax = config.get(Configuration.CATEGORY_GENERAL, "targetYposMax", 20, "targetYposMax,min=1,max=255").getInt();
        targetYposMax = (targetYposMax < 1) ? 1 : (targetYposMax > 255) ? 255 : targetYposMax;
        targetYposMin = config.get(Configuration.CATEGORY_GENERAL, "targetYposMin", 1, "targetYposMin,min=1,max=255").getInt();
        targetYposMin = (targetYposMin < 1) ? 1 : (targetYposMin > targetYposMax) ? targetYposMax : targetYposMin;
        chunkWidely = config.get(Configuration.CATEGORY_GENERAL, "chunkWidely", 3, "chankWidely,min=0,max=16").getInt();
        chunkWidely = (chunkWidely < 0) ? 0 : (chunkWidely > 16) ? 16 : chunkWidely;
        replaceBlockRL = new ResourceLocation(replaceBlockID);
        config.save();
    }

    @Mod.EventHandler
    public void load(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new LivingUpdateHook());
    }
}