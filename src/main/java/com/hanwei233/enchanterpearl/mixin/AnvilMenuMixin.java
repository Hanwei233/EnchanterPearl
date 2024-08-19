package com.hanwei233.enchanterpearl.mixin;

import com.hanwei233.enchanterpearl.EnchanterPearlItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.hanwei233.enchanterpearl.EnchanterPearl.ENCHANTER_PEARL;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin extends ItemCombinerMenu {

    @Shadow @Final private DataSlot cost;
    @Shadow public int repairItemCountCost;
    @Shadow @Final public static int INPUT_SLOT;
    @Shadow @Final public static int ADDITIONAL_SLOT;

    public AnvilMenuMixin(int id, Inventory inventory, ContainerLevelAccess access) {
        super(MenuType.ANVIL, id, inventory, access);
        this.addDataSlot(this.cost);
    }

    @Inject(method = "createResult", at = @At("RETURN"))
    public void changePearlBrokenState(CallbackInfo ci) {
        ItemStack inputStack1 = this.inputSlots.getItem(INPUT_SLOT);
        ItemStack inputStack2 = this.inputSlots.getItem(ADDITIONAL_SLOT);
        ItemStack resultStack = ItemStack.EMPTY;

        if (inputStack1.isEmpty() || inputStack2.isEmpty()) return;

        if (inputStack1.is(ENCHANTER_PEARL.get())) {

            if (EnchanterPearlItem.isBroken(inputStack1) && inputStack2.is(Items.ENDER_PEARL)) {
                resultStack = enchanterpearl$setBrokenState(inputStack1, false);
            }
            if (!EnchanterPearlItem.isBroken(inputStack1) && inputStack2.is(Items.OBSIDIAN)) {
                resultStack = enchanterpearl$setBrokenState(inputStack1, true);
            }

            if (resultStack.isEmpty()) return;

            this.resultSlots.setItem(0, resultStack);
            this.repairItemCountCost = 1;
            this.cost.set(3);
        }
    }

    @Unique
    public ItemStack enchanterpearl$setBrokenState(ItemStack stack, boolean state) {
        ItemStack resultStack = stack.copy();

        resultStack.getOrCreateTag().putBoolean("broken", state);
        return resultStack;
    }
}
