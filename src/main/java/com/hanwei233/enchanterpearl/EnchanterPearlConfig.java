package com.hanwei233.enchanterpearl;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = EnchanterPearl.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EnchanterPearlConfig {
    public static final ForgeConfigSpec CONFIG_SPEC;
    public static final EnchanterPearlConfig CONFIG;

    public final ForgeConfigSpec.IntValue maxStoredXP;
    public final ForgeConfigSpec.BooleanValue retrieveUntilNextLevel;
    public final ForgeConfigSpec.BooleanValue storeUntilPreviousLevel;
    //public final ForgeConfigSpec.BooleanValue retrieveXPRepairItem;
    public final ForgeConfigSpec.BooleanValue identificationCostsXP;
    public final ForgeConfigSpec.IntValue costsXP;

    static {
        Pair<EnchanterPearlConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(EnchanterPearlConfig::new);
        CONFIG_SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    EnchanterPearlConfig(ForgeConfigSpec.Builder builder) {
        maxStoredXP = builder
                .comment("The maximum amount of experience that can be stored in the pearl")
                .defineInRange("MaxStoreXP", 2920, 1, Integer.MAX_VALUE);
        retrieveUntilNextLevel = builder
                .comment("Retrieving the experience of upgrading the next level")
                .define("RetrieveUntilNextLevel", true);
        storeUntilPreviousLevel = builder
                .comment("Stored to the previous level of experience")
                .define("StoreUntilPreviousLevel", true);
        /*retrieveXPRepairItem = builder
                .comment("Repair items when retrieving the experience from broken Enchanter Pearl, just like Vanilla Mending enchantment")
                .define("RetrieveXPRepairItem", true);*/
        identificationCostsXP = builder
                .comment("Identifying incompatible enchantments costs experience")
                .define("IdentificationCostsXP", false);
        costsXP = builder
                .comment("The experience cost to identify incompatible enchantments")
                .defineInRange("CostsXP", 100, 0, Integer.MAX_VALUE);
    }
}
