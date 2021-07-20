package net.roguelogix.biggerutilities.transfernetwork;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.phosphophyllite.tile.ITileModule;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoreTileModule implements ITileModule {
    private final TransferNetworkTile tile;
    
    public CoreTileModule(TransferNetworkTile tileEntity) {
        this.tile = tileEntity;
    }
    
    @Override
    public TileEntity getTile() {
        return tile;
    }
    
    @Override
    public String saveKey() {
        return "TransferNetworkCore";
    }
    
    private final CoreTileModule[] neighbors = new CoreTileModule[6];
    
    @Nullable
    CoreTileModule neighbor(Direction direction) {
        return neighbors[direction.getIndex()];
    }
    
    @Override
    public void onBlockUpdate(BlockState neighborBlockState, BlockPos neighborPos) {
        Block block = neighborBlockState.getBlock();
        Direction direction = Util.directionFromPositions(tile.getPos(), neighborPos);
        neighbors[direction.getIndex()] = null;
        if (block == TransferNetworkBlock.INSTANCE) {
            //noinspection ConstantConditions
            TileEntity neighborTile = tile.getWorld().getTileEntity(neighborPos);
            assert neighborTile instanceof TransferNetworkTile;
            // just trust me, it is
            neighbors[direction.getIndex()] = ((TransferNetworkTile) neighborTile).coreTileModule;
        }
    }
}
