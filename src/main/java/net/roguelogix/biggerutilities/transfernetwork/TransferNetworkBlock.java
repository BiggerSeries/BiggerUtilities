package net.roguelogix.biggerutilities.transfernetwork;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.material.Material;
import net.roguelogix.phosphophyllite.registry.RegisterBlock;
import net.roguelogix.phosphophyllite.tile.PhosphophylliteBlock;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterBlock(name = "transfer_network", tileEntityClass = TransferNetworkTile.class)
public class TransferNetworkBlock extends PhosphophylliteBlock {
    
    @RegisterBlock.Instance
    public static TransferNetworkBlock INSTANCE;
    
    public TransferNetworkBlock() {
        super(TransferNetworkTile::new, Properties.create(Material.IRON).hardnessAndResistance(2, 10).setAllowsSpawn((a, b, c, d) -> false).notSolid());
    }
}
