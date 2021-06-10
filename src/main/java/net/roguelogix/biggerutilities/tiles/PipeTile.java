package net.roguelogix.biggerutilities.tiles;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import net.roguelogix.phosphophyllite.Phosphophyllite;
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
        this(TYPE);
    }

    public PipeTile(TileEntityType<?> TYPE) {
        super(TYPE);
    }

    private final PipeTile[] neighbors = new PipeTile[6];
    protected final IItemHandler[] neighborItemCaps = new IItemHandler[]{
        EmptyHandler.INSTANCE,
        EmptyHandler.INSTANCE,
        EmptyHandler.INSTANCE,
        EmptyHandler.INSTANCE,
        EmptyHandler.INSTANCE,
        EmptyHandler.INSTANCE,
    };

    @Nullable
    public PipeTile getNeighbor(Direction direction) {
        return neighbors[direction.getIndex()];
    }

    public void neighborChanged(Direction direction) {
        assert world != null;
        TileEntity tile = world.getTileEntity(pos.offset(direction));
        if (tile instanceof PipeTile || tile == null) {
            if (!canConnect(direction) || tile == null || !((PipeTile) tile).canConnect(direction.getOpposite())) {
                neighbors[direction.getIndex()] = null;
            } else {
                neighbors[direction.getIndex()] = (PipeTile) tile;
            }
        } else {
            neighborItemCaps[direction.getIndex()] = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElse(EmptyHandler.INSTANCE);
        }
        updateBlockState();
    }

    protected boolean canConnect(Direction direction) {
        return true;
    }

    protected boolean shouldConnect(Direction direction, @Nullable TileEntity tile) {
        return true;
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        state = state.with(TOP_CONNECTED_PROPERTY, isConnected(Direction.UP));
        state = state.with(BOTTOM_CONNECTED_PROPERTY, isConnected(Direction.DOWN));
        state = state.with(NORTH_CONNECTED_PROPERTY, isConnected(Direction.NORTH));
        state = state.with(SOUTH_CONNECTED_PROPERTY, isConnected(Direction.SOUTH));
        state = state.with(EAST_CONNECTED_PROPERTY, isConnected(Direction.EAST));
        state = state.with(WEST_CONNECTED_PROPERTY, isConnected(Direction.WEST));
        assert world != null;
        world.setBlockState(pos, state, 2);
    }

    private boolean isConnected(Direction direction){
        return neighbors[direction.getIndex()] != null || neighborItemCaps[direction.getIndex()] != EmptyHandler.INSTANCE;
    }

    @Override
    public void onLoad() {
        Phosphophyllite.serverQueue.enqueue(this::firstTick);
    }

    public void firstTick() {
        for (Direction value : Direction.values()) {
            neighborChanged(value);
        }
    }
}
