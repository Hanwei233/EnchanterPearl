package com.hanwei233.enchanterpearl;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


@Mod(EnchanterPearl.MODID)
public class EnchanterPearl {
    public static final String MODID = "enchanterpearl";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> ENCHANTER_PEARL = ITEMS.register("enchanter_pearl", () -> new EnchanterPearlItem(new Item.Properties().stacksTo(1)));

    public EnchanterPearl() {
        IEventBus modbus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modbus);
        modbus.addListener(this::addToCreateModeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EnchanterPearlConfig.CONFIG_SPEC);
    }

    @Mod.EventBusSubscriber(modid = EnchanterPearl.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void setupEvent(FMLClientSetupEvent event) {
            event.enqueueWork(() -> ItemProperties.register(ENCHANTER_PEARL.get(), new ResourceLocation(MODID, "broken"), (stack, level, entity, seed) -> EnchanterPearlItem.isBroken(stack) ? 1.0F : 0.0F));
        }
    }


    public void addToCreateModeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES || event.getTabKey() == CreativeModeTabs.INGREDIENTS)
            event.getEntries().putAfter(new ItemStack(Items.ENDER_EYE), new ItemStack(ENCHANTER_PEARL.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }
}
