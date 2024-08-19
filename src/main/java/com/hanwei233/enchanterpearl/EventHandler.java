package com.hanwei233.enchanterpearl;

import com.hanwei233.enchanterpearl.mixin.ExperienceOrbAccessor;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

import javax.annotation.Nullable;
import java.util.Optional;

import static com.hanwei233.enchanterpearl.EnchanterPearl.ENCHANTER_PEARL;

@Mod.EventBusSubscriber(modid = EnchanterPearl.MODID)
public class EventHandler {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pickUpXPEvent(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        ItemStack stack = null;

        if (player.level().isClientSide()) return;

        if (getPearlInCurios(player) != null) {
            stack = getPearlInCurios(player);
        }
        else if (getPearlInInventory(player) != null) {
            stack = getPearlInInventory(player);
        }

        if (stack == null) return;
        ExperienceOrb xpOrb = event.getOrb();
        int xpOrbCount = ((ExperienceOrbAccessor) xpOrb).getCount();
        int remainderXP = addAndReturnRemainingXP(stack, xpOrb.value * xpOrbCount);

        if (remainderXP <= 0) {
            player.takeXpDelay = 2;
            player.take(xpOrb, 1);
            xpOrb.discard();
            event.setCanceled(true);
        }
        else {
            xpOrb.value = remainderXP;
        }
    }

    @Nullable
    public static ItemStack getPearlInInventory(Player player) {
        Inventory inventory = player.getInventory();

        if (!inventory.isEmpty()) {
            for (ItemStack stack : inventory.items) {
                if (stack.is(ENCHANTER_PEARL.get())) {
                    int storedXP = EnchanterPearlItem.getStoredXP(stack);

                    if (EnchanterPearlItem.isBroken(stack) && storedXP < EnchanterPearlConfig.CONFIG.maxStoredXP.get()) {
                        return stack;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static ItemStack getPearlInCurios(Player player) {
        if (ModList.get().isLoaded("curios")) {
            Optional<ICuriosItemHandler> curiosInventory = CuriosApi.getCuriosInventory(player).resolve();

            if (curiosInventory.isPresent()) {
                Optional<SlotResult> slotResult = curiosInventory.get().findFirstCurio(ENCHANTER_PEARL.get());

                if (slotResult.isPresent()) {
                    ItemStack slotStack = slotResult.get().stack();
                    int storedXP = EnchanterPearlItem.getStoredXP(slotStack);

                    if (EnchanterPearlItem.isBroken(slotStack) && storedXP < EnchanterPearlConfig.CONFIG.maxStoredXP.get()) {
                        return slotStack;
                    }
                }
            }
        }
        return null;
    }

    public static int addAndReturnRemainingXP(ItemStack stack, int xpOrbValue) {
        int maxStoredXP = EnchanterPearlConfig.CONFIG.maxStoredXP.get();
        int storedXP = EnchanterPearlItem.getStoredXP(stack);
        int sum = storedXP + xpOrbValue;

        if(sum <= maxStoredXP) {
            EnchanterPearlItem.addStoredXP(stack, xpOrbValue);
            return 0;
        }
        else {
            EnchanterPearlItem.addStoredXP(stack, maxStoredXP - storedXP);
            return sum - maxStoredXP;
        }
    }
}
