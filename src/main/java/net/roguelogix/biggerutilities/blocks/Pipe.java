package net.roguelogix.biggerutilities.blocks;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.roguelogix.biggerutilities.tiles.PipeTile;
import net.roguelogix.phosphophyllite.multiblock.generic.ConnectedTextureStates;
import net.roguelogix.phosphophyllite.registry.CreativeTabBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static net.roguelogix.phosphophyllite.multiblock.generic.ConnectedTextureStates.*;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@CreativeTabBlock
@RegisterBlock(name = "pipe", tileEntityClass = PipeTile.class)
public class Pipe extends Block {

    @RegisterBlock.Instance
    public static Pipe INSTANCE;

    public Pipe() {
        super(Properties.create(Material.IRON).hardnessAndResistance(2, 10).setAllowsSpawn((a, b, c, d) -> false).notSolid());
        BlockState defaultState = this.getDefaultState();
        defaultState = defaultState.with(TOP_CONNECTED_PROPERTY, false);
        defaultState = defaultState.with(BOTTOM_CONNECTED_PROPERTY, false);
        defaultState = defaultState.with(NORTH_CONNECTED_PROPERTY, false);
        defaultState = defaultState.with(SOUTH_CONNECTED_PROPERTY, false);
        defaultState = defaultState.with(EAST_CONNECTED_PROPERTY, false);
        defaultState = defaultState.with(WEST_CONNECTED_PROPERTY, false);
        this.setDefaultState(defaultState);
    }

    @OnlyIn(Dist.CLIENT)
    public float getAmbientOcclusionLightValue(@Nonnull BlockState state, @Nonnull IBlockReader worldIn, @Nonnull BlockPos pos) {
        return 1.0F;
    }

    public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull IBlockReader reader, @Nonnull BlockPos pos) {
        return true;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new PipeTile();
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos neighbor, boolean isMoving) {
        super.neighborChanged(state, world, pos, blockIn, neighbor, isMoving);
        PipeTile tile = ((PipeTile) world.getTileEntity(pos));
        if (tile == null) {
            return;
        }
        tile.neighborChanged(neighbor);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        PipeTile tile = ((PipeTile) world.getTileEntity(pos));
        if (tile == null) {
            return;
        }
        for (Direction value : Direction.values()) {
            tile.neighborChanged(pos.offset(value));
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(ConnectedTextureStates.TOP_CONNECTED_PROPERTY);
        builder.add(ConnectedTextureStates.BOTTOM_CONNECTED_PROPERTY);
        builder.add(ConnectedTextureStates.NORTH_CONNECTED_PROPERTY);
        builder.add(ConnectedTextureStates.SOUTH_CONNECTED_PROPERTY);
        builder.add(ConnectedTextureStates.EAST_CONNECTED_PROPERTY);
        builder.add(ConnectedTextureStates.WEST_CONNECTED_PROPERTY);
    }
}
