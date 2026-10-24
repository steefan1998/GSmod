package nl.gsmod.daily;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

final class Rewards {
    /** Puts items in the inventory, dropping what doesn't fit at the player's feet. */
    static void give(ServerPlayer player, List<ItemStack> items, int experience) {
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                player.drop(copy, false, false);
            }
        }
        if (experience > 0) {
            player.giveExperiencePoints(experience);
        }
        player.containerMenu.broadcastChanges();
    }

    private Rewards() {}
}
