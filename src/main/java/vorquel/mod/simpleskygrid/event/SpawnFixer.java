package vorquel.mod.simpleskygrid.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import vorquel.mod.simpleskygrid.config.Config;
import vorquel.mod.simpleskygrid.helper.Ref;

public class SpawnFixer {

    //Thank you diesieben07 for this function.
    @SubscribeEvent
    @SuppressWarnings("unused")
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        NBTTagCompound data = event.player.getEntityData();
        NBTTagCompound persistent;
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, (persistent = new NBTTagCompound()));
        } else {
            persistent = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        }

        if (!persistent.hasKey("simpleskygrid.hasLogged")) {
            persistent.setBoolean("simpleskygrid.hasLogged", true);
            playerRespawn(new PlayerEvent.PlayerRespawnEvent(event.player));
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public void playerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if(event.player.worldObj.getWorldInfo().getTerrainType() != Ref.worldType || !Config.dimensionPropertiesMap.containsKey(event.player.dimension))
            return;
        BlockPos pos = event.player.playerLocation;
        BlockPos bedCoordinates = event.player.getBedLocation(event.player.dimension);
        if(pos.getY() <= 1 || bedCoordinates == null || bedCoordinates.distanceSq(pos) > 10) {
            x -= x%4;
            z -= z%4;
            Chunk chunk = event.player.worldObj.getChunkFromBlockCoords(pos);
            int spawnHeight = Config.dimensionPropertiesMap.get(event.player.dimension).spawnHeight;
            int airs = 0;
            for(y=Math.min(chunk.getTopFilledSegment()+16, spawnHeight)+2; y>0; --y) {
                if(airs < 2) {
                    if(chunk.getBlock(x&15, y-1, z&15).getMaterial().blocksMovement()) {
                        airs = 0;
                        continue;
                    }
                    airs++;
                    continue;
                }
                if(chunk.getBlock(x&15, y-1, z&15).getMaterial().blocksMovement())
                    break;
            }
            event.player.setPositionAndUpdate(x + .5, y, z + .5);
        }
    }
}
