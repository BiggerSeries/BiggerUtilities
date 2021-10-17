package net.roguelogix.biggerutilities.quarry;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2ic;

import java.util.Comparator;
import java.util.Objects;

@SuppressWarnings("DuplicatedCode")
public class TorechRegistry {
    
    private static final ObjectArrayList<TorechTile> newToreches = new ObjectArrayList<>();
    private static final Int2ObjectOpenHashMap<ObjectArrayList<Int2ObjectOpenHashMap<ObjectArrayList<Vector2ic>>>> torechesByX = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<ObjectArrayList<Int2ObjectOpenHashMap<ObjectArrayList<Vector2ic>>>> torechesByZ = new Int2ObjectOpenHashMap<>();
    
    public static void loadLevel(Level level) {
        int hashCode = level.hashCode();
        
        var byX = new ObjectArrayList<Int2ObjectOpenHashMap<ObjectArrayList<Vector2ic>>>(level.getHeight());
        var byZ = new ObjectArrayList<Int2ObjectOpenHashMap<ObjectArrayList<Vector2ic>>>(level.getHeight());
        
        for (int i = 0; i < level.getHeight(); i++) {
            byX.add(new Int2ObjectOpenHashMap<>());
            byZ.add(new Int2ObjectOpenHashMap<>());
        }
        
        torechesByX.put(hashCode, byX);
        torechesByZ.put(hashCode, byZ);
    }
    
    public static void unloadLevel(Level level) {
        int hashCode = level.hashCode();
        torechesByX.remove(hashCode);
        torechesByZ.remove(hashCode);
    }
    
    public static void serverStop() {
        torechesByX.clear();
        torechesByZ.clear();
    }
    
    static void newTorech(TorechTile torech) {
        newToreches.add(torech);
    }
    
    public static void tick() {
        for (int i = 0; i < newToreches.size(); i++) {
            addTorech(newToreches.get(i));
        }
        newToreches.clear();
    }
    
    private static void addTorech(TorechTile torech) {
        if (Objects.requireNonNull(torech.getLevel()).isClientSide) {
            return;
        }
        final var level = torech.getLevel();
        assert level != null;
        final int hashCode = level.hashCode();
        final var pos = torech.getBlockPos();
        final int yIndex = pos.getY() - level.getMinBuildHeight();
        final var torechesXMap = torechesByX.get(hashCode).get(yIndex);
        final var torechesZMap = torechesByZ.get(hashCode).get(yIndex);
        final int xPos = pos.getX();
        final int zPos = pos.getZ();
        var torechesX = torechesXMap.get(xPos);
        var torechesZ = torechesZMap.get(zPos);
        if (torechesX == null) {
            torechesX = new ObjectArrayList<>();
            torechesXMap.put(xPos, torechesX);
        }
        if (torechesZ == null) {
            torechesZ = new ObjectArrayList<>();
            torechesZMap.put(zPos, torechesZ);
        }
        
        // chunk reloads can cause duplicate adds
        if (!torechesX.contains(torech.pos)) {
            torechesX.add(torech.pos);
        }
        if (!torechesZ.contains(torech.pos)) {
            torechesZ.add(torech.pos);
        }
        
        // im expecting the counts here to be pretty small, so this shouldn't cause problems
        torechesX.sort(Comparator.comparingInt(Vector2ic::y));
        torechesZ.sort(Comparator.comparingInt(Vector2ic::x));
        int xIndex = torechesX.indexOf(torech.pos);
        int zIndex = torechesZ.indexOf(torech.pos);
        
        final var blockpos = new BlockPos.MutableBlockPos();
        
        torech.nextNegZ = null;
        if (xIndex != 0) {
            final var nextNegZ = torechesX.get(xIndex - 1);
            blockpos.set(nextNegZ.x(), pos.getY(), nextNegZ.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextPosZ = torech.pos;
                torech.nextNegZ = nextTorech.pos;
                nextTorech.notifyLinkChange();
            }
        }
        xIndex++;
        torech.nextPosZ = null;
        if (xIndex != torechesX.size()) {
            final var nextPosZ = torechesX.get(xIndex);
            blockpos.set(nextPosZ.x(), pos.getY(), nextPosZ.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextNegZ = torech.pos;
                torech.nextPosZ = nextTorech.pos;
                nextTorech.notifyLinkChange();
            }
        }
        
        torech.nextNegX = null;
        if (zIndex != 0) {
            final var nextNegX = torechesZ.get(zIndex - 1);
            blockpos.set(nextNegX.x(), pos.getY(), nextNegX.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextPosX = torech.pos;
                torech.nextNegX = nextTorech.pos;
                nextTorech.notifyLinkChange();
            }
        }
        zIndex++;
        torech.nextPosX = null;
        if (zIndex != torechesZ.size()) {
            final var nextPosX = torechesZ.get(zIndex);
            blockpos.set(nextPosX.x(), pos.getY(), nextPosX.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextNegX = torech.pos;
                torech.nextPosX = nextTorech.pos;
                nextTorech.notifyLinkChange();
            }
        }
        torech.notifyLinkChange();
    }
    
    static void removeTorech(TorechTile torech) {
        final var level = torech.getLevel();
        assert level != null;
        final int hashCode = level.hashCode();
        final var pos = torech.getBlockPos();
        final int yIndex = pos.getY() - level.getMinBuildHeight();
        final var torechesXMap = torechesByX.get(hashCode).get(yIndex);
        final var torechesZMap = torechesByZ.get(hashCode).get(yIndex);
        final int xPos = pos.getX();
        final int zPos = pos.getZ();
        final var torechesX = torechesXMap.get(xPos);
        final var torechesZ = torechesZMap.get(zPos);
        if (torechesX != null) {
            torechesX.remove(torech.pos);
        }
        if (torechesZ != null) {
            torechesZ.remove(torech.pos);
        }
        final var blockpos = new BlockPos.MutableBlockPos();
        if (torech.nextPosX != null) {
            blockpos.set(torech.nextPosX.x(), pos.getY(), torech.nextPosX.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextNegX = torech.nextNegX;
                nextTorech.notifyLinkChange();
            }
        }
        if (torech.nextNegX != null) {
            blockpos.set(torech.nextNegX.x(), pos.getY(), torech.nextNegX.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextPosX = torech.nextPosX;
                nextTorech.notifyLinkChange();
            }
        }
        if (torech.nextPosZ != null) {
            blockpos.set(torech.nextPosZ.x(), pos.getY(), torech.nextPosZ.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextNegZ = torech.nextNegZ;
                nextTorech.notifyLinkChange();
            }
        }
        if (torech.nextNegZ != null) {
            blockpos.set(torech.nextNegZ.x(), pos.getY(), torech.nextNegZ.y());
            if (level.getBlockEntity(blockpos) instanceof TorechTile nextTorech) {
                nextTorech.nextPosZ = torech.nextPosZ;
                nextTorech.notifyLinkChange();
            }
        }
        
    }
}
