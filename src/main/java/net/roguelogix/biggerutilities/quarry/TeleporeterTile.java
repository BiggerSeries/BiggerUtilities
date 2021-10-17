package net.roguelogix.biggerutilities.quarry;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.roguelogix.biggerutilities.Config;
import net.roguelogix.phosphophyllite.energy.IPhosphophylliteEnergyStorage;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.registry.RegisterItem;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.threading.Event;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.CompletableFuture;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "teleporeter")
public class TeleporeterTile extends PhosphophylliteTile implements IPhosphophylliteEnergyStorage {
    
    @RegisterTileEntity.Type
    public static BlockEntityType<?> TYPE;
    
    @RegisterItem(name = "mining_tool", creativeTab = false)
    private static class MiningTool extends Item {
        
        @RegisterItem.Instance
        public static MiningTool INSTANCE;
        
        private static ItemStack STACK;
        
        public MiningTool(Properties pProperties) {
            super(pProperties);
            STACK = new ItemStack(this, 1);
        }
        
        @Override
        public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
            return toolAction == ToolActions.AXE_DIG
                    || toolAction == ToolActions.PICKAXE_DIG
                    || toolAction == ToolActions.SHOVEL_DIG
                    || toolAction == ToolActions.SHEARS_DIG
                    || toolAction == ToolActions.SWORD_DIG
                    ;
        }
        
        @Override
        public boolean isCorrectToolForDrops(BlockState pBlock) {
            return true;
        }
    }
    
    public TeleporeterTile(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
        
    }
    
    final ObjectArrayList<ItemStack> internalBuffer = new ObjectArrayList<>();
    
    @Override
    public void onAdded() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        Queues.serverThread.enqueue(this::neighborChanged);
        Queues.serverThread.enqueue(() -> {
            var pos = getBlockPos();
            fakePlayer = FakePlayerFactory.get((ServerLevel) level, new GameProfile(net.minecraft.Util.NIL_UUID, "Telep-ore-ter_x" + pos.getX() + "_y" + pos.getY() + "_z" + pos.getZ()));
        });
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        unForceChunk();
    }
    
    private void unForceChunk() {
        if (lastForcedChunk != null) {
            assert level != null;
            level.getChunkSource().updateChunkForced(lastForcedChunk, false);
        }
    }
    
    void tick() {
        mine();
        itemOutput.ifPresent(itemOutput -> {
            final int slotCount = itemOutput.getSlots();
            while (!internalBuffer.isEmpty()) {
                var stack = internalBuffer.pop();
                for (int i = 0; i < slotCount; i++) {
                    var currentStack = itemOutput.getStackInSlot(i);
                    if (currentStack.isEmpty() || currentStack.sameItem(stack)) {
                        stack = itemOutput.insertItem(i, stack, false);
                    }
                    if (stack.isEmpty()) {
                        break;
                    }
                }
                if (!stack.isEmpty()) {
                    internalBuffer.push(stack);
                    return;
                }
            }
        });
        if(activeBlockState != isMining){
            activeBlockState = isMining;
            assert level != null;
            level.setBlockAndUpdate(getBlockPos(), getBlockState().setValue(Teleporeter.ACTIVE, activeBlockState));
        }
    }
    
    boolean connected = false;
    LazyOptional<IItemHandler> itemOutput = LazyOptional.empty();
    
    public void neighborChanged() {
        itemOutput = LazyOptional.empty();
        assert level != null;
        var te = level.getBlockEntity(worldPosition.relative(Direction.UP));
        if (te == null) {
            connected = false;
            return;
        }
        itemOutput = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, Direction.DOWN);
        itemOutput.addListener(iItemHandlerLazyOptional -> this.neighborChanged());
        connected = itemOutput.isPresent();
    }
    
    private FakePlayer fakePlayer; // TODO: fire events?
    
    private boolean activeBlockState = false;
    private boolean isMining = false;
    private boolean needsNewPos = false;
    
    private final Long2ObjectLinkedOpenHashMap<BlockState> newStates = new Long2ObjectLinkedOpenHashMap<>();
    
    void onRightClick() {
        isMining = false;
        
        BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();
        assert level != null;
        boolean foundTorech = false;
        TorechTile torech = null;
        for (Direction value : Direction.values()) {
            if (level.getBlockEntity(mutableBlockPos.set(worldPosition).move(value)) instanceof TorechTile currentTorech) {
                if (foundTorech) {
                    return;
                }
                foundTorech = true;
                torech = currentTorech;
            }
        }
        if (!foundTorech) {
            return;
        }
        var miningArea = torech.getBoundedArea();
        if (miningArea == null) {
            return;
        }
        start.set(miningArea.getFirst()).add(1, 1);
        end.set(miningArea.getSecond()).sub(1, 1);
        maxY = worldPosition.getY() + 1;
        minY = level.getMinBuildHeight();
        resetIteration();
        if (start.x == worldPosition.getX() || end.x == worldPosition.getX()
                || start.y == worldPosition.getZ() || end.y == worldPosition.getZ()) {
            return;
        }
        isMining = true;
    }
    
    private void mine() {
        if (!isMining) {
            return;
        }
        if (!internalBuffer.isEmpty()) {
            return;
        }
        resetExponent();
        newStates.clear();
        for (int i = 0; i < Config.CONFIG.Teleporeter.MAX_BLOCKS_PER_TICK; i++) {
            if (!nextPos()) {
                isMining = false;
                unForceChunk();
                break;
            }
            assert level != null;
            BlockState blockState = getBlock();
            if (blockState == null) {
                break;
            }
            if (blockState.isAir()) {
                needsNewPos = true;
                continue;
            }
            LootContext.Builder lootContext = new LootContext.Builder((ServerLevel) level);
            lootContext.withRandom(level.random);
            lootContext.withParameter(LootContextParams.ORIGIN, new Vec3(pos.x, pos.y, pos.z));
            lootContext.withParameter(LootContextParams.TOOL, MiningTool.STACK);
            lootContext.withOptionalParameter(LootContextParams.BLOCK_ENTITY, getTile());
            final var drops = blockState.getDrops(lootContext);
            if (drops.isEmpty()) {
                needsNewPos = true;
                continue;
            }
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, blockPos, blockState, fakePlayer);
            MinecraftForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
                needsNewPos = true;
                continue;
            }
            if (!consumeEnergy()) {
                break;
            }
            needsNewPos = true;
            internalBuffer.addAll(drops);
            newStates.put(blockPos.asLong(), Blocks.AIR.defaultBlockState());
        }
        Util.setBlockStatesAndUpdateLight(newStates, level);
        this.setChanged();
    }
    
    private final Vector3i pos = new Vector3i();
    private final BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
    private ChunkPos lastForcedChunk;
    
    long chunkPosLong = 0;
    ChunkAccess lastChunk = null;
    
    private int minY = 0, maxY = 0;
    private final Vector2i start = new Vector2i();
    private final Vector2i end = new Vector2i();
    private final Vector2i startChunk = new Vector2i();
    private final Vector2i endChunk = new Vector2i();
    
    private final Vector2i currentChunkPos = new Vector2i();
    
    private final Vector2i chunkMin = new Vector2i();
    private final Vector2i chunkMax = new Vector2i();
    
    void resetIteration() {
        Vector2i scratchVec = new Vector2i();
        start.min(end, scratchVec);
        start.max(end, end);
        start.set(scratchVec);
        
        startChunk.x = start.x >> 4;
        startChunk.y = start.y >> 4;
        endChunk.x = end.x >> 4;
        endChunk.y = end.y >> 4;
        currentChunkPos.set(startChunk);
        
        chunkMax.set(currentChunkPos).mul(16).add(15, 15).max(start).min(end);
        chunkMin.set(currentChunkPos).mul(16).max(start).min(end);
        
        pos.x = chunkMin.x;
        pos.y = maxY;
        pos.z = chunkMin.y;
        blockPos.set(pos.x, pos.y, pos.z);
        
        needsNewPos = false;
    }
    
    private boolean nextPos() {
        if (!needsNewPos) {
            return true;
        }
        pos.x++;
        if (pos.x > chunkMax.x) {
            pos.x = chunkMin.x;
            pos.z++;
        }
        if (pos.z > chunkMax.y) {
            pos.z = chunkMin.y;
            pos.y--;
        }
        if (pos.y < minY) {
            currentChunkPos.x++;
            if (currentChunkPos.x > endChunk.x) {
                currentChunkPos.x = startChunk.x;
                currentChunkPos.y++;
                if (currentChunkPos.y > endChunk.y) {
                    return false;
                }
            }
            chunkMin.set(currentChunkPos).mul(16).max(start).min(end);
            chunkMax.set(currentChunkPos).mul(16).add(15, 15).max(start).min(end);
            
            pos.x = chunkMin.x;
            pos.y = maxY;
            pos.z = chunkMin.y;
        }
        blockPos.set(pos.x, pos.y, pos.z);
        needsNewPos = false;
        return true;
    }
    
    @Nullable
    private BlockState getBlock() {
        if (lastChunk == null || ChunkPos.asLong(blockPos) != chunkPosLong) {
            assert level != null;
            lastChunk = level.getChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()), ChunkStatus.FULL, false);
            if (lastChunk == null) {
                if (lastForcedChunk == null || lastForcedChunk.x != currentChunkPos.x || lastForcedChunk.z != currentChunkPos.y) {
                    if (lastForcedChunk != null) {
                        level.getChunkSource().updateChunkForced(lastForcedChunk, false);
                    }
                    lastForcedChunk = new ChunkPos(blockPos);
                    level.getChunkSource().updateChunkForced(lastForcedChunk, true);
                }
                return null;
            }
            chunkPosLong = lastChunk.getPos().toLong();
        }
        return lastChunk.getBlockState(blockPos);
    }
    
    @Nullable
    private BlockEntity getTile() {
        if (lastChunk == null || ChunkPos.asLong(blockPos) != chunkPosLong) {
            assert level != null;
            lastChunk = level.getChunk(blockPos);
            chunkPosLong = lastChunk.getPos().toLong();
        }
        return lastChunk.getBlockEntity(blockPos);
    }
    
    @Override
    protected CompoundTag writeNBT() {
        final var nbt = super.writeNBT();
        
        nbt.putInt("startx", start.x);
        nbt.putInt("starty", minY);
        nbt.putInt("staryz", start.y);
        
        nbt.putInt("endx", end.x);
        nbt.putInt("endy", maxY);
        nbt.putInt("endz", end.y);
        
        nbt.putInt("posx", pos.x);
        nbt.putInt("posy", pos.y);
        nbt.putInt("posz", pos.z);
        
        nbt.putBoolean("needsNewPos", needsNewPos);
        nbt.putBoolean("isMining", isMining);
        
        final var internalBufferNBT = new CompoundTag();
        
        internalBufferNBT.putInt("count", internalBuffer.size());
        for (int i = 0; i < internalBuffer.size(); i++) {
            internalBufferNBT.put(Integer.toString(i), internalBuffer.get(i).serializeNBT());
        }
        
        nbt.put("internalBuffer", internalBufferNBT);
        
        return nbt;
    }
    
    @Override
    protected void readNBT(CompoundTag nbt) {
        super.readNBT(nbt);
        
        start.x = nbt.getInt("startx");
        minY = nbt.getInt("starty");
        start.y = nbt.getInt("staryz");
        
        end.x = nbt.getInt("endx");
        maxY = nbt.getInt("endy");
        end.y = nbt.getInt("endz");
        
        pos.x = nbt.getInt("posx");
        pos.y = nbt.getInt("posy");
        pos.z = nbt.getInt("posz");
        blockPos.set(pos.x, pos.y, pos.z);
        
        isMining = nbt.getBoolean("isMining");
        needsNewPos = nbt.getBoolean("needsNewPos");
        
        final var internalBufferNBT = nbt.getCompound("internalBuffer");
        final var internalBufferSize = internalBufferNBT.getInt("count");
        internalBuffer.clear();
        internalBuffer.ensureCapacity(internalBufferSize);
        for (int i = 0; i < internalBufferSize; i++) {
            final var elementNBT = internalBufferNBT.getCompound(Integer.toString(i));
            final var itemStack = ItemStack.of(elementNBT);
            internalBuffer.add(itemStack);
        }
        
        startChunk.x = start.x >> 4;
        startChunk.y = start.y >> 4;
        endChunk.x = end.x >> 4;
        endChunk.y = end.y >> 4;
        currentChunkPos.set(pos.x >> 4, pos.z >> 4);
        chunkMin.set(currentChunkPos).mul(16).max(start).min(end);
        chunkMax.set(currentChunkPos).mul(16).add(15, 15).max(start).min(end);
    }
    
    private long energy = 0;
    private long consumedLastTick = 0;
    private long energyBufferSize = 0;
    private double currentEnergyMultiplier = 1;
    
    private void resetExponent() {
        energyBufferSize = (long) (Config.CONFIG.Teleporeter.BASE_ENERGY_AMOUNT * Math.pow(Config.CONFIG.Teleporeter.ENERGY_EXPONENT, Config.CONFIG.Teleporeter.MAX_BLOCKS_PER_TICK)) + 1000;
        currentEnergyMultiplier = 1;
        consumedLastTick = 0;
    }
    
    private boolean consumeEnergy() {
        double energyToConsume = Config.CONFIG.Teleporeter.BASE_ENERGY_AMOUNT * currentEnergyMultiplier;
        if (energy < energyToConsume) {
            return false;
        }
        consumedLastTick += energyToConsume;
        energy -= energyToConsume;
        currentEnergyMultiplier *= Config.CONFIG.Teleporeter.ENERGY_EXPONENT;
        return true;
    }
    
    @Override
    protected <T> LazyOptional<T> capability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityEnergy.ENERGY) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.capability(cap, side);
    }
    
    @Override
    public long insertEnergy(long maxInsert, boolean simulate) {
        if (!isMining) {
            return 0;
        }
        long toInsert = energyBufferSize - energy;
        toInsert = Math.min(toInsert, maxInsert);
        if (!simulate) {
            energy += toInsert;
        }
        return toInsert;
    }
    
    @Override
    public long extractEnergy(long maxExtract, boolean simulate) {
        return 0;
    }
    
    @Override
    public long energyStored() {
        return energy;
    }
    
    @Override
    public long maxEnergyStored() {
        return Long.MAX_VALUE;
    }
    
    @Override
    public boolean canInsert() {
        return true;
    }
    
    @Override
    public boolean canExtract() {
        return false;
    }
    
}
