package net.roguelogix.biggerutilities;

import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;
import net.roguelogix.biggerutilities.quarry.TorechRegistry;
import net.roguelogix.phosphophyllite.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(BiggerUtilities.modid)
public class BiggerUtilities {
    public static final String modid = "biggerutilities";
    
    public static final Logger LOGGER = LogManager.getLogger();
    
    public BiggerUtilities() {
        new Registry();
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    void onWorldLoad(final WorldEvent.Load worldUnloadEvent) {
        TorechRegistry.loadLevel((Level) worldUnloadEvent.getWorld());
    }
        @SubscribeEvent
    void onWorldUnload(final WorldEvent.Unload worldUnloadEvent) {
        TorechRegistry.unloadLevel((Level) worldUnloadEvent.getWorld());
    }
    
    @SubscribeEvent
    public void advanceTick(TickEvent.ServerTickEvent e) {
        TorechRegistry.tick();
    }
    
    @SubscribeEvent
    public void serverStop(FMLServerStoppedEvent e) {
        TorechRegistry.serverStop();
    }
}
