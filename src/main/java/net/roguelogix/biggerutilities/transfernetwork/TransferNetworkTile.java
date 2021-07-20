package net.roguelogix.biggerutilities.transfernetwork;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.tile.ISidedMultipart;
import net.roguelogix.phosphophyllite.tile.ITileModule;
import net.roguelogix.phosphophyllite.tile.PhosphophylliteTile;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;


@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@RegisterTileEntity(name = "transfernetwork")
public class TransferNetworkTile extends PhosphophylliteTile implements ISidedMultipart {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    public static final TileSupplier SUPPLIER = TransferNetworkTile::new;
    
    @Nonnull
    SidedMultipartModule sidedMultipartModule;
    @Nonnull
    CoreTileModule coreTileModule;
    
    public TransferNetworkTile() {
        super(TYPE, null);
        throw new NotImplementedException("");
        
    }
    
    public TransferNetworkTile(BlockState blockState) {
        super(TYPE, blockState);
        sidedMultipartModule = getModule(ISidedMultipart.class, SidedMultipartModule.class);
        coreTileModule = sidedMultipartModule.getSideModule(null, CoreTileModule.class);
    }
    
    @Override
    public ITileModule initialCoreModule() {
        return new CoreTileModule(this);
    }
}
