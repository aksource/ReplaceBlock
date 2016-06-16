package ReplaceBlock;

import com.google.common.base.Optional;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameData;
import net.minecraftforge.fml.common.registry.GameRegistry;

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
    public static final String MOD_DEPENDENCIES = "required-after:Forge@[11.14.0.1237,)";
    public static final String MOD_MC_VERSION = "[1.8,1.8.9]";

    public static String[] targetBlockID;
    public static String replaceBlockID;
    public static int targetYposMax;
    public static int targetYposMin;
    public static int chunkWidely;

    private Chunk lastChunk = null;
    private static final int CHUNK_WIDE = 16;
    protected ArrayList<Integer> ids = new ArrayList<Integer>();

    public class LivingUpdateHook {
        @SubscribeEvent
        public void LivingUpdate(LivingUpdateEvent event) {
            if (!event.entityLiving.worldObj.isRemote) {
                int posX = (int) Math.floor(event.entityLiving.posX);
                int posZ = (int) Math.floor(event.entityLiving.posZ);
                int posY = (int) Math.floor(event.entityLiving.posY);
                Chunk chunk = event.entityLiving.worldObj.getChunkFromBlockCoords(new BlockPos(event.entityLiving.posX, event.entityLiving.posY, event.entityLiving.posZ));
                if (chunk != lastChunk && targetYposMax + chunkWidely >= posY && posY >= targetYposMin - chunkWidely) {
                    lastChunk = chunk;
                    int chunkX = chunk.xPosition * CHUNK_WIDE;
                    int chunkZ = chunk.zPosition * CHUNK_WIDE;
                    for (int x = chunkX - chunkWidely; x < chunkX + CHUNK_WIDE + chunkWidely; x++) {
                        for (int z = chunkZ - chunkWidely; z < chunkZ + CHUNK_WIDE + chunkWidely; z++) {
                            for (int y = targetYposMax; y >= targetYposMin; y--) {
                                BlockPos blockPos = new BlockPos(x, y, z);
                                if (this.isTargetBlock(event.entityLiving.worldObj.getBlockState(blockPos).getBlock())) {
                                    //minecraft.getIntegratedServer().worldServerForDimension(minecraft.thePlayer.dimension).setBlock(x, y, z, replaceBlockID, 0, 3);
                                    Block block = GameData.getBlockRegistry().getObject(replaceBlockID);
                                    event.entityLiving.worldObj.setBlockState(blockPos, block.getDefaultState());
                                }
                            }
                        }
                    }
                }
            }
        }

        public boolean isTargetBlock(Block block) {
            for (int i = 0; i < targetBlockID.length; i++) {
                if (targetBlockID[i].equals(this.getUniqueStrings(block))) {
                    return true;
                }
            }
            return false;
        }

        public String getUniqueStrings(Object obj) {
            GameRegistry.UniqueIdentifier uId;
            if (obj instanceof Block) {
                uId = GameRegistry.findUniqueIdentifierFor((Block) obj);
            } else {
                uId = GameRegistry.findUniqueIdentifierFor((Item) obj);
            }
            return Optional.fromNullable(uId).or(new GameRegistry.UniqueIdentifier("none:dummy")).toString();

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
        config.save();
    }

    @Mod.EventHandler
    public void load(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new LivingUpdateHook());
    }
}