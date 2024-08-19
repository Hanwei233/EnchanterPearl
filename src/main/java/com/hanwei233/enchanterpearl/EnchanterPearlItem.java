package com.hanwei233.enchanterpearl;

import com.hanwei233.enchanterpearl.openmods.utils.EnchantmentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class EnchanterPearlItem extends Item {
    public final Style TOOLTIP_STYLE = Style.EMPTY.applyFormat(ChatFormatting.GRAY);
    public final Style ENCHANTMENT_STYLE = Style.EMPTY.applyFormat(ChatFormatting.YELLOW);

    public EnchanterPearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isCrouching() && !isBroken(stack) && getStoredXP(stack) < EnchanterPearlConfig.CONFIG.maxStoredXP.get()) {
            int xpToPearl;

            if (EnchanterPearlConfig.CONFIG.storeUntilPreviousLevel.get()) {
                int xpFromLevel = EnchantmentUtils.getExperienceForLevel(player.experienceLevel);

                xpToPearl = EnchantmentUtils.getPlayerXP(player) - xpFromLevel;

                if (xpToPearl == 0 && player.experienceLevel > 0) //player has exactly x > 0 levels (xp bar looks empty)
                    xpToPearl = xpFromLevel - EnchantmentUtils.getExperienceForLevel(player.experienceLevel - 1);
            }
            else xpToPearl = EnchantmentUtils.getPlayerXP(player);

            if (xpToPearl <= 0) return InteractionResultHolder.pass(stack);
            int finalXP = this.calculateAddedXP(stack, xpToPearl);
            int oldXPLevel = player.experienceLevel;

            if (!MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.XpChange(player, -finalXP))) {
                addStoredXP(stack, finalXP);
                EnchantmentUtils.addPlayerXP(player, -finalXP);
                this.playXPOrbSound(level, player);
            }

            if (oldXPLevel != player.experienceLevel) MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.LevelChange(player, player.experienceLevel));

            return InteractionResultHolder.success(stack);
        }
        else if (player.isCrouching() && getStoredXP(stack) > 0) {
            int xpToPlayer;
            int repairDamage = 0;
            ItemStack repairStack = null;

            if (EnchanterPearlConfig.CONFIG.retrieveUntilNextLevel.get()) {
                xpToPlayer = EnchantmentUtils.getExperienceForLevel(player.experienceLevel + 1) - EnchantmentUtils.getPlayerXP(player);
            }
            else xpToPlayer = getStoredXP(stack);

            if (xpToPlayer <= 0) return InteractionResultHolder.pass(stack);
            int finalXP = this.calculateRemovedXP(stack, xpToPlayer);
            int oldXPLevel = player.experienceLevel;

            if (isBroken(stack)/* && EnchanterPearlConfig.CONFIG.retrieveXPRepairItem.get()*/){
                repairStack = getRepairedItems(player, ItemStack::isDamaged);

                if (repairStack != null) {
                    repairDamage = getRepairDamage(repairStack, finalXP);
                    finalXP = calculateRepairXP(repairStack, finalXP);
                }
            }

            if (finalXP <= 0 || !MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.XpChange(player, finalXP))) {
                addStoredXP(stack, -finalXP);
                EnchantmentUtils.addPlayerXP(player, finalXP);
                if (repairStack != null) repairStack.setDamageValue(repairStack.getDamageValue() - repairDamage);
                this.playXPOrbSound(level, player);
            }

            if (oldXPLevel != player.experienceLevel) MinecraftForge.EVENT_BUS.post(new PlayerXpEvent.LevelChange(player, player.experienceLevel));

            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack selectedStack, Slot slot, ClickAction action, Player player) {
        ItemStack overridedStack = slot.getItem();

        if (action == ClickAction.SECONDARY && !isBroken(selectedStack) && !overridedStack.isEmpty()) {
            Boolean identificationCostsXP = EnchanterPearlConfig.CONFIG.identificationCostsXP.get();
            int costsXP = EnchanterPearlConfig.CONFIG.costsXP.get();

            if (identificationCostsXP && getStoredXP(selectedStack) < costsXP) return false;
            HashSet<Enchantment> enchantments = this.getImCompatibilityEnchantments(overridedStack);

            if (!enchantments.isEmpty()) {
                if (identificationCostsXP) addStoredXP(selectedStack, -costsXP);

                if (player.level().isClientSide()) {
                    player.sendSystemMessage(Component.translatable("message.enchanterpearl.imcompatibility"));
                    this.playIdentifyingSound(player.level(), player);

                    for (Enchantment e : enchantments) {
                        player.sendSystemMessage(Component.translatable(e.getDescriptionId()).setStyle(ENCHANTMENT_STYLE));
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getStoredXP(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Mth.clamp(0, MAX_BAR_WIDTH * ((float) getStoredXP(stack) / (float) EnchanterPearlConfig.CONFIG.maxStoredXP.get()), MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float storedXP = getStoredXP(stack);

        return Mth.hsvToRgb(Math.max(0.0F, storedXP / EnchanterPearlConfig.CONFIG.maxStoredXP.get()) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (isBroken(stack)) {
            tooltip.add(Component.translatable("tooltip.enchanterpearl.broken").setStyle(TOOLTIP_STYLE));
            tooltip.add(Component.translatable("tooltip.enchanterpearl.2").setStyle(TOOLTIP_STYLE));
            tooltip.add(Component.translatable("tooltip.enchanterpearl.4").setStyle(TOOLTIP_STYLE));
        }
        else {
            tooltip.add(Component.translatable("tooltip.enchanterpearl.1").setStyle(TOOLTIP_STYLE));
            tooltip.add(Component.translatable("tooltip.enchanterpearl.2").setStyle(TOOLTIP_STYLE));
            tooltip.add(Component.translatable("tooltip.enchanterpearl.3").setStyle(TOOLTIP_STYLE));
        }
        tooltip.add(Component.translatable("tooltip.enchanterpearl.5", getStoredXP(stack), EnchanterPearlConfig.CONFIG.maxStoredXP.get()).setStyle(TOOLTIP_STYLE));
    }

    public static boolean isBroken(ItemStack stack) {
        return stack.getOrCreateTag().getBoolean("broken");
    }

    public static int getStoredXP(ItemStack stack) {
        return stack.getOrCreateTag().getInt("xp");
    }

    public static void addStoredXP(ItemStack stack, int amount) {
        int storedXP = getStoredXP(stack);
        stack.getOrCreateTag().putInt("xp", storedXP + amount);
    }

    public int calculateAddedXP(ItemStack stack, int amount) {
        int maxStoredXP = EnchanterPearlConfig.CONFIG.maxStoredXP.get();
        int storedXP = getStoredXP(stack);

        if(storedXP + amount <= maxStoredXP) {
            return amount;
        }
        else {
            return maxStoredXP - storedXP;
        }
    }

    public int calculateRemovedXP(ItemStack stack, int amount) {
        int storedXP = getStoredXP(stack);

        return Math.min(storedXP, amount);
    }

    public int calculateRepairXP(ItemStack repairStack, int amount) {
        int repairXP = amount - this.getRepairDamage(repairStack, amount) / 2;//durabilityToXp()

        return Math.max(repairXP, 0);
    }

    public int getRepairDamage(ItemStack repairStack, int amount) {
        return Math.min((int) (amount * repairStack.getXpRepairRatio()), repairStack.getDamageValue());
    }

    @Nullable
    public ItemStack getRepairedItems(Player player, Predicate<ItemStack> predicate) {
        Inventory inventory = player.getInventory();
        ItemStack result = null;
        double min = 1.0;

        if (!inventory.isEmpty()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);

                if (!stack.isEmpty() && EnchantmentHelper.getTagEnchantmentLevel(Enchantments.MENDING, stack) > 0 && predicate.test(stack)) {
                    int maxDamage = stack.getMaxDamage();
                    int damage = stack.getDamageValue();
                    double percentage = (double) (maxDamage - damage) / maxDamage;

                    if (percentage < min) {
                        min = percentage;
                        result = stack;
                    }
                }
            }
            return result;
        }
        return null;
    }

    public HashSet<Enchantment> getImCompatibilityEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> stackEnchantments = EnchantmentHelper.getEnchantments(stack);
        HashSet<Enchantment> imCompatibilityEnchantments = new HashSet<>();

        if (stackEnchantments.isEmpty()) return imCompatibilityEnchantments;
        for (Map.Entry<Enchantment, Integer> E : stackEnchantments.entrySet()) {
            Enchantment enchantment = E.getKey();

            for (Enchantment forgeEnchantment : ForgeRegistries.ENCHANTMENTS.getValues()) {
                if (forgeEnchantment.canEnchant(stack) || stack.is(Items.ENCHANTED_BOOK)) {
                    if (!enchantment.isCompatibleWith(forgeEnchantment) && enchantment != forgeEnchantment) {
                        imCompatibilityEnchantments.add(forgeEnchantment);
                    }
                }
            }
        }
        return imCompatibilityEnchantments;
    }

    public void playXPOrbSound(Level level, Player player) {
        if (level.isClientSide())
            level.playSound(player, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.3F, (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.35F + 0.9F);
    }

    public void playIdentifyingSound(Level level, Player player) {
        if (level.isClientSide())
            level.playSound(player, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.2F, player.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}