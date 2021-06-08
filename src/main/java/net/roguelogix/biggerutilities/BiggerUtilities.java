package net.roguelogix.biggerutilities;

import net.minecraftforge.fml.common.Mod;
import net.roguelogix.phosphophyllite.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(BiggerUtilities.modid)
public class BiggerUtilities {
    public static final String modid = "biggerutilities";

    public static final Logger LOGGER = LogManager.getLogger();

    public BiggerUtilities(){
        new Registry();
    }
}
