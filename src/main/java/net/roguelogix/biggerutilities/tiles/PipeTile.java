package net.roguelogix.biggerutilities.tiles;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.multiblock.generic.ConnectedTextureStates.*;
import static net.roguelogix.phosphophyllite.multiblock.generic.ConnectedTextureStates.WEST_CONNECTED_PROPERTY;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "pipe")
public class PipeTile extends TileEntity {
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;

    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = PipeTile::new;

    public PipeTile() {
        super(TYPE);
    }

    public PipeTile(TileEntityType<?> TYPE) {
        super(TYPE);
    }

    private final PipeTile[] neighbors = new PipeTile[6];

    @Nullable
    public PipeTile getNeighbor(Direction direction) {
        return neighbors[direction.getIndex()];
    }

    public void neighborChanged(BlockPos neighbor) {
        assert world != null;
        TileEntity tile = world.getTileEntity(neighbor);
        if (tile instanceof PipeTile || tile == null) {
            Direction dir = Direction.byLong(neighbor.getX() - pos.getX(), neighbor.getY() - pos.getY(), neighbor.getZ() - pos.getZ());
            if (dir == null) {
                return;
            }
            neighbors[dir.getIndex()] = (PipeTile) tile;
            updateBlockState();
        }
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        state = state.with(TOP_CONNECTED_PROPERTY, neighbors[Direction.UP.getIndex()] != null);
        state = state.with(BOTTOM_CONNECTED_PROPERTY, neighbors[Direction.DOWN.getIndex()] != null);
        state = state.with(NORTH_CONNECTED_PROPERTY, neighbors[Direction.NORTH.getIndex()] != null);
        state = state.with(SOUTH_CONNECTED_PROPERTY, neighbors[Direction.SOUTH.getIndex()] != null);
        state = state.with(EAST_CONNECTED_PROPERTY, neighbors[Direction.EAST.getIndex()] != null);
        state = state.with(WEST_CONNECTED_PROPERTY, neighbors[Direction.WEST.getIndex()] != null);
        assert world != null;
        world.setBlockState(pos, state, 2);
    }

}
