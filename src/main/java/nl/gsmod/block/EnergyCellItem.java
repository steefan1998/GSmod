package nl.gsmod.block;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import nl.gsmod.Config;
import nl.gsmod.registry.ModDataComponents;

public class EnergyCellItem extends BlockItem {
    public EnergyCellItem(Block block, Properties properties) {
        super(block, properties);
    }

    private static int stored(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ENERGY.get(), 0);
    }

    private static int capacity() {
        return Config.SPEC.isLoaded() ? Config.ENERGY_CELL_CAPACITY.get() : 1_000_000;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("message.gsmod.energy",
                String.format("%,d", stored(stack)), String.format("%,d", capacity())).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stored(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * Mth.clamp(stored(stack) / (float) capacity(), 0, 1));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE0302A;
    }
}
