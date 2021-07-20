package net.roguelogix.biggerutilities.transfernetwork;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.Direction;
import net.roguelogix.phosphophyllite.repack.org.joml.Random;
import org.apache.logging.log4j.core.Core;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface INetworkSearch {
    CoreTileModule next();
    
    void reset();
    
    enum Mode {
        DEFAULT,
//        DFS,
//        BFS
    }
    
    static INetworkSearch createSearcher(Mode mode, CoreTileModule initialNode, Direction nodeSide) {
        switch (mode) {
//            case DFS:
//            case BFS:
            case DEFAULT:
                return new DefaultNetworkSearcher(initialNode, nodeSide);
        }
        throw new IllegalArgumentException("Unsupported search mode");
    }
    
    class DefaultNetworkSearcher implements INetworkSearch {
        private final CoreTileModule initialNode;
        private final Direction nodeSide;
        
        private CoreTileModule currentNode;
        private Direction lastDirection;
        
        private final Random random = new Random();
        private final int[] nextIndexTable = new int[5];
        
        DefaultNetworkSearcher(CoreTileModule initialNode, Direction nodeSide) {
            this.initialNode = initialNode;
            this.nodeSide = nodeSide;
        }
        
        @Override
        public CoreTileModule next() {
            if (currentNode == null) {
                currentNode = initialNode;
                lastDirection = nodeSide.getOpposite();
                return currentNode;
            }
            Direction returnDirection = lastDirection.getOpposite();
            CoreTileModule nextNode = null;
            Direction nextDirection = null;
            
            for (int i = 0; i < 5; i++) {
                nextIndexTable[i] = i;
            }
            for (int i = returnDirection.getIndex(); i < 5; i++) {
                nextIndexTable[i]++;
            }
            
            int checkedDirectionCount = 1;
            while (checkedDirectionCount != 6) {
                int randomNum = random.nextInt(6 - checkedDirectionCount);
                int nextDirectionIndex = nextIndexTable[randomNum];
                Direction direction = Direction.byIndex(nextDirectionIndex);
                nextNode = currentNode.neighbor(direction);
                if (nextNode != null) {
                    nextDirection = direction;
                    break;
                }
                for (int i = randomNum; i < 5; i++) {
                    nextIndexTable[i]++;
                }
                checkedDirectionCount++;
            }
            if (nextDirection == null) {
                currentNode = null;
                return next();
            }
            currentNode = nextNode;
            lastDirection = nextDirection;
            return currentNode;
        }
        
        @Override
        public void reset() {
            currentNode = null;
        }
    }
}
