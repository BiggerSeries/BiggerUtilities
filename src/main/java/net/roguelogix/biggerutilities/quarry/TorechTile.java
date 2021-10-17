package net.roguelogix.biggerutilities.quarry;

import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2ic;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "torech")
public class TorechTile extends PhosphophylliteTile {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final BlockEntityType.BlockEntitySupplier<TorechTile> SUPPLIER = TorechTile::new;
    
    public TorechTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        this.pos = new Vector2i(pos.getX(), pos.getZ());
    }
    
    final Vector2ic pos;
    Vector2ic nextPosX = null;
    Vector2ic nextNegX = null;
    Vector2ic nextPosZ = null;
    Vector2ic nextNegZ = null;
    
    private Vector2ic linkX = null;
    private Vector2ic linkZ = null;
    
    void notifyLinkChange() {
        if (nextPosX == null) {
            linkX = nextNegX;
        } else if (nextNegX == null) {
            linkX = nextPosX;
        } else {
            int xPos = getBlockPos().getX();
            linkX = xPos - nextNegX.x() > nextPosX.x() - xPos ? nextNegX : nextPosX;
        }
        if (nextPosZ == null) {
            linkZ = nextNegZ;
        } else if (nextNegZ == null) {
            linkZ = nextPosZ;
        } else {
            int zPos = getBlockPos().getZ();
            linkZ = zPos - nextNegZ.y() > nextPosZ.y() - zPos ? nextNegZ : nextPosZ;
        }
        this.setChanged();
    }
    
    @Nullable
    public Pair<Vector2i, Vector2i> getBoundedArea() {
        if (linkX == null || linkZ == null) {
            return null;
        }
        final var min = new Vector2i(Math.min(linkX.x(), linkZ.x()), Math.min(linkX.y(), linkZ.y()));
        final var max = new Vector2i(Math.max(linkX.x(), linkZ.x()), Math.max(linkX.y(), linkZ.y()));
        return new Pair<>(min, max);
    }
    
    @Override
    public void onAdded() {
        TorechRegistry.newTorech(this);
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        if (!chunkUnload) {
            TorechRegistry.removeTorech(this);
        }
    }
    
    @Override
    protected void readNBT(CompoundTag nbt) {
        super.readNBT(nbt);
        if (nbt.contains("npxx")) {
            nextPosX = new Vector2i(nbt.getInt("npxx"), nbt.getInt("npxy"));
        }
        if (nbt.contains("nnxx")) {
            nextNegX = new Vector2i(nbt.getInt("nnxx"), nbt.getInt("nnxy"));
        }
        if (nbt.contains("npzx")) {
            nextPosZ = new Vector2i(nbt.getInt("npzx"), nbt.getInt("npzy"));
        }
        if (nbt.contains("nnzx")) {
            nextNegZ = new Vector2i(nbt.getInt("nnzx"), nbt.getInt("nnzy"));
        }
        notifyLinkChange();
    }
    
    @Override
    protected CompoundTag writeNBT() {
        final var nbt = super.writeNBT();
        if (nextPosX != null) {
            nbt.putInt("npxx", nextPosX.x());
            nbt.putInt("npxy", nextPosX.y());
        }
        if (nextNegX != null) {
            nbt.putInt("nnxx", nextNegX.x());
            nbt.putInt("nnxy", nextNegX.y());
        }
        if (nextPosZ != null) {
            nbt.putInt("npzx", nextPosZ.x());
            nbt.putInt("npzy", nextPosZ.y());
        }
        if (nextNegZ != null) {
            nbt.putInt("nnzx", nextNegZ.x());
            nbt.putInt("nnzy", nextNegZ.y());
        }
        return nbt;
    }
}
