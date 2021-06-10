package net.roguelogix.biggerutilities.tiles;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.util.BlockStates;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "item_transfer_node")
public class ItemTransferNodeTile extends PipeTile implements ITickableTileEntity, IItemHandler {
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;

    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = ItemTransferNodeTile::new;

    public ItemTransferNodeTile() {
        super(TYPE);
    }

    private Direction facingDirection;

    private Direction faceDirection() {
        if (facingDirection == null) {
            facingDirection = getBlockState().get(BlockStates.FACING);
        }
        return facingDirection;
    }

    @Override
    public void neighborChanged(Direction direction) {
//        Direction dir = Direction.byLong(neighbor.getX() - pos.getX(), neighbor.getY() - pos.getY(), neighbor.getZ() - pos.getZ());
//        if(dir == getBlockState().get(BlockStates.FACING)){
//            return;
//        }
        if (direction == faceDirection()) {
            grabCapability();
        } else {
            super.neighborChanged(direction);
        }
    }

    @Override
    protected boolean canConnect(Direction direction) {
        if (direction == faceDirection()) {
            return false;
        }
        return super.canConnect(direction);
    }

    @Override
    public void firstTick() {
        super.firstTick();
        grabCapability();
    }

    private IItemHandler itemInput;

    private void grabCapability() {
        Direction facing = faceDirection();
        assert world != null;
        TileEntity tile = world.getTileEntity(pos.offset(facing));
        if (tile != null) {
            LazyOptional<IItemHandler> itemHandlerOptional = tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
            itemInput = null;
            itemHandlerOptional.ifPresent(iItemHandler -> {
                itemInput = iItemHandler;
            });
        }
    }

    @Override
    public void tick() {
        if (itemInput != null) {
            if (Phosphophyllite.tickNumber() % 10 == 0) {
                pullItems(1);
            }
        }
        if (!buffer.isEmpty()) {
            if (Phosphophyllite.tickNumber() % 5 == 0) {
                stepSearch();
            }
        }
    }

    Direction marchDirection = null;
    PipeTile searchPipe = this;

    public void stepSearch() {
        boolean found = false;
        for (Direction value : Direction.values()) {
            if (value.getOpposite() != marchDirection) {
                PipeTile pipe = searchPipe.getNeighbor(value);
                if (pipe != null) {
                    searchPipe = pipe;
                    marchDirection = value;
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            searchPipe = this;
            marchDirection = null;
        }
        int startingItems = buffer.getCount();
        for (IItemHandler itemHandler : searchPipe.neighborItemCaps) {
            for (int i = 0; i < itemHandler.getSlots(); i++) {
                buffer = itemHandler.insertItem(i, buffer, false);
                if (startingItems != buffer.getCount()) {
                    return;
                }
            }
        }
    }

    @Nonnull
    private ItemStack buffer = ItemStack.EMPTY;

    private void pullItems(int maxItems) {
        if (buffer.getCount() >= buffer.getMaxStackSize()) {
            return;
        }
        for (int slot = itemInput.getSlots() - 1; slot >= 0; slot--) {
            ItemStack slotStack = itemInput.getStackInSlot(slot);
            if (slotStack.isEmpty()) {
                continue;
            }
            if (buffer.isEmpty()) {
                buffer = slotStack.copy();
                buffer.setCount(0);
            } else if (!slotStack.isItemEqual(buffer)) {
                continue;
            }
            int toMove = buffer.getMaxStackSize() - buffer.getCount();
            toMove = Math.max(slotStack.getCount(), toMove);
            toMove = Math.min(maxItems, toMove);
            if (toMove == 0) {
                return;
            }
            ItemStack stack = itemInput.extractItem(slot, toMove, false);
            assert stack.isItemEqual(buffer);
            buffer.setCount(buffer.getCount() + stack.getCount());
            maxItems -= stack.getCount();
            if (maxItems == 0) {
                return;
            }
        }
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return buffer;
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if (slot != 0) {
            return stack;
        }
        ItemStack buffer = this.buffer;
        if (buffer.isEmpty()) {
            buffer = stack.copy();
            buffer.setCount(0);
        } else if (!buffer.isItemEqual(stack)) {
            return stack;
        }
        if (buffer.getCount() >= buffer.getMaxStackSize()) {
            return stack;
        }
        stack = stack.copy();
        int toMove = buffer.getMaxStackSize() - buffer.getCount();
        toMove = Math.max(stack.getCount(), toMove);
        stack.setCount(stack.getCount() - toMove);
        if (!simulate) {
            buffer.setCount(buffer.getCount() + toMove);
            this.buffer = buffer;
        }
        return stack;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot != 0) {
            return 0;
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return slot == 0;
    }
}
