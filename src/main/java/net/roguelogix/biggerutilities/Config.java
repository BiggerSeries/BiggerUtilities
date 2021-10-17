package net.roguelogix.biggerutilities;

import net.roguelogix.phosphophyllite.PhosphophylliteConfig;
import net.roguelogix.phosphophyllite.config.ConfigFormat;
import net.roguelogix.phosphophyllite.config.ConfigValue;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;

@SuppressWarnings({"FieldMayBeFinal", "unused"})

public class Config {
    
    @RegisterConfig(format = ConfigFormat.TOML)
    public static final Config CONFIG = new Config();
    
    @ConfigValue(hidden = true, enableAdvanced = true)
    private boolean EnableAdvanced = false;
    
    public static class Teleporeter {
        @ConfigValue(range = "[1,)")
        public int MAX_BLOCKS_PER_TICK = 1000;
        @ConfigValue(range = "[1,)", advanced = true)
        public final double BASE_ENERGY_AMOUNT = 1000;
        @ConfigValue(range = "(0,)", advanced = true)
        public final double ENERGY_EXPONENT = 1.01;
    }
    
    public final Teleporeter Teleporeter = new Teleporeter();
}
