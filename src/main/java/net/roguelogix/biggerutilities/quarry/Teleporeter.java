package net.roguelogix.biggerutilities.quarry;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.roguelogix.phosphophyllite.modular.block.PhosphophylliteBlock;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import org.apache.logging.log4j.core.jmx.Server;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class Teleporeter extends PhosphophylliteBlock implements EntityBlock {
    
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    
    @RegisterBlock(name = "teleporeter", tileEntityClass = TeleporeterTile.class)
    public static final Teleporeter INSTANCE = new Teleporeter();
    
    public Teleporeter() {
        super(Properties.of(Material.METAL));
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeleporeterTile(pos, state);
    }
    
    @Override
    protected void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.buildStateDefinition(builder);
        builder.add(ACTIVE);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        return (level1, state, type, entity) -> {
            assert entity instanceof TeleporeterTile;
            ((TeleporeterTile) entity).tick();
        };
    }
    
    @Override
    public InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isCrouching() && hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
            if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof TeleporeterTile tile) {
                tile.onRightClick();
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUse(state, level, pos, player, hand, hitResult);
    }
    
    @Override
    public void onNeighborChange(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChange(state, level, pos, blockIn, fromPos, isMoving);
        if (level.getBlockEntity(pos) instanceof TeleporeterTile tile) {
            tile.neighborChanged();
        }
    }
}
