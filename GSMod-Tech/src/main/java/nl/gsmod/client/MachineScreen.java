package nl.gsmod.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import nl.gsmod.machine.MachineType;
import nl.gsmod.menu.MachineMenu;

/**
 * Machine GUI drawn entirely from rectangles in the vanilla container palette, so no GUI textures are needed.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int BG = 0xFFC6C6C6;
    private static final int LIGHT = 0xFFFFFFFF;
    private static final int DARK = 0xFF555555;
    private static final int SLOT_BG = 0xFF8B8B8B;
    private static final int BLACK = 0xFF000000;
    private static final int ENERGY_EMPTY = 0xFF3A1010;
    private static final int ENERGY_FULL = 0xFFE0302A;
    private static final int ENERGY_TOP = 0xFFFF8A60;
    private static final int PROGRESS = 0xFFFFFFFF;
    private static final int FLAME = 0xFFFF9A1F;

    private static final int ENERGY_X = 10, ENERGY_Y = 17, ENERGY_W = 12, ENERGY_H = 52;
    private static final int ARROW_X = 80, ARROW_Y = 36, ARROW_W = 24, ARROW_H = 14;
    private static final int FLAME_X = 81, FLAME_Y = 27, FLAME_S = 14;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = leftPos;
        int y = topPos;

        bevel(graphics, x, y, imageWidth, imageHeight, BG, LIGHT, DARK);
        graphics.outline(x, y, imageWidth, imageHeight, BLACK);

        for (Slot slot : menu.slots) {
            bevel(graphics, x + slot.x - 1, y + slot.y - 1, 18, 18, SLOT_BG, DARK, LIGHT);
        }

        // Energy bar
        bevel(graphics, x + ENERGY_X - 1, y + ENERGY_Y - 1, ENERGY_W + 2, ENERGY_H + 2, ENERGY_EMPTY, DARK, LIGHT);
        int capacity = menu.getCapacity();
        int filled = capacity > 0 ? (int) ((long) menu.getEnergy() * ENERGY_H / capacity) : 0;
        if (filled > 0) {
            int top = y + ENERGY_Y + ENERGY_H - filled;
            graphics.fillGradient(x + ENERGY_X, top, x + ENERGY_X + ENERGY_W, y + ENERGY_Y + ENERGY_H, ENERGY_TOP, ENERGY_FULL);
        }

        if (menu.getMachineType() == MachineType.GENERATOR) {
            graphics.fill(x + FLAME_X, y + FLAME_Y, x + FLAME_X + FLAME_S, y + FLAME_Y + FLAME_S, SLOT_BG);
            int maxBurn = menu.getMaxBurn();
            if (maxBurn > 0 && menu.getBurn() > 0) {
                int h = Math.max(1, menu.getBurn() * FLAME_S / maxBurn);
                graphics.fill(x + FLAME_X, y + FLAME_Y + FLAME_S - h, x + FLAME_X + FLAME_S, y + FLAME_Y + FLAME_S, FLAME);
            }
        } else {
            graphics.fill(x + ARROW_X, y + ARROW_Y + 4, x + ARROW_X + ARROW_W - 6, y + ARROW_Y + ARROW_H - 4, SLOT_BG);
            for (int i = 0; i < 7; i++) {
                graphics.fill(x + ARROW_X + ARROW_W - 7 + i, y + ARROW_Y + i, x + ARROW_X + ARROW_W - 6 + i, y + ARROW_Y + ARROW_H - i, SLOT_BG);
            }
            int max = menu.getMaxProgress();
            if (max > 0 && menu.getProgress() > 0) {
                int w = menu.getProgress() * ARROW_W / max;
                graphics.enableScissor(x + ARROW_X, y + ARROW_Y, x + ARROW_X + w, y + ARROW_Y + ARROW_H);
                graphics.fill(x + ARROW_X, y + ARROW_Y + 4, x + ARROW_X + ARROW_W - 6, y + ARROW_Y + ARROW_H - 4, PROGRESS);
                for (int i = 0; i < 7; i++) {
                    graphics.fill(x + ARROW_X + ARROW_W - 7 + i, y + ARROW_Y + i, x + ARROW_X + ARROW_W - 6 + i, y + ARROW_Y + ARROW_H - i, PROGRESS);
                }
                graphics.disableScissor();
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (isHovering(ENERGY_X, ENERGY_Y, ENERGY_W, ENERGY_H, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("message.gsmod.energy",
                    String.format("%,d", menu.getEnergy()), String.format("%,d", menu.getCapacity())), mouseX, mouseY);
        } else if (hoveredSlot == null && isHovering(MachineMenu.UPGRADE_X, 17, 16, 52, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(Component.translatable("gui.gsmod.upgrades"), mouseX, mouseY);
        }
    }

    /** Filled rectangle with a 1px light top-left and dark bottom-right edge. */
    private static void bevel(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int topLeft, int bottomRight) {
        g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w - 1, y + 1, topLeft);
        g.fill(x, y, x + 1, y + h - 1, topLeft);
        g.fill(x + 1, y + h - 1, x + w, y + h, bottomRight);
        g.fill(x + w - 1, y + 1, x + w, y + h, bottomRight);
    }
}
