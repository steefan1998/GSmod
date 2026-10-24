package nl.gsmod.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.gsmod.daily.DailyClock;
import nl.gsmod.daily.DailyStatusPayload;

/**
 * Shows the daily reward and missions the server sent in a {@link DailyStatusPayload}.
 * The claim button only sends {@code /daily claim}; the server checks and answers with a fresh payload.
 */
public class DailyScreen extends Screen {
    private static final int WIDTH = 236;
    private static final int ROW = 24;
    private static final int PANEL = 0xE0101420;
    private static final int BORDER = 0x50FFFFFF;
    private static final int BAR_BG = 0xFF2A2F3A;
    private static final int BAR = 0xFF3FA9F5;
    private static final int BAR_DONE = 0xFF5BE35B;

    private DailyStatusPayload status;
    private long receivedAt;
    private Button claimButton;

    public DailyScreen(DailyStatusPayload status) {
        super(Component.translatable("screen.gsmod.daily"));
        setStatus(status);
    }

    public void setStatus(DailyStatusPayload status) {
        this.status = status;
        this.receivedAt = System.currentTimeMillis();
        if (claimButton != null) {
            updateButton();
        }
    }

    private int panelHeight() {
        return 92 + Math.max(1, status.missions().size()) * ROW;
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(4, (height - panelHeight()) / 2);
    }

    @Override
    protected void init() {
        claimButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.connection.sendCommand("daily claim");
                b.active = false;
            }
        }).bounds(left() + WIDTH - 108, top() + 22, 100, 20).build());
        updateButton();
    }

    private void updateButton() {
        claimButton.active = status.rewardsEnabled() && status.canClaim();
        claimButton.setMessage(Component.translatable(status.canClaim() ? "screen.gsmod.daily.claim" : "screen.gsmod.daily.claimed"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int x = left();
        int y = top();
        int h = panelHeight();
        graphics.fill(x, y, x + WIDTH, y + h, PANEL);
        graphics.outline(x, y, WIDTH, h, BORDER);
        graphics.centeredText(font, title, width / 2, y + 7, 0xFFFFD83A);

        if (status.rewardsEnabled()) {
            graphics.text(font, Component.translatable("screen.gsmod.daily.streak", status.streak()), x + 8, y + 24, 0xFFFFFFFF);
            graphics.text(font, Component.translatable("screen.gsmod.daily.next", status.nextStreak()), x + 8, y + 35, 0xFFAAAAAA);
        } else {
            graphics.text(font, Component.translatable("screen.gsmod.daily.disabled"), x + 8, y + 28, 0xFFFF6060);
        }
        long elapsed = (System.currentTimeMillis() - receivedAt) / 1000;
        long left = Math.max(0, status.secondsUntilReset() - elapsed);
        graphics.text(font, Component.translatable("screen.gsmod.daily.reset", DailyClock.formatDuration(left)), x + 8, y + 50, 0xFF8899AA);

        int my = y + 68;
        graphics.horizontalLine(x + 8, x + WIDTH - 8, my - 4, BORDER);
        graphics.text(font, Component.translatable("screen.gsmod.daily.missions").withStyle(ChatFormatting.BOLD), x + 8, my, 0xFFFFFFFF);
        my += 14;
        if (status.missions().isEmpty()) {
            graphics.text(font, Component.translatable("message.gsmod.mission.none"), x + 8, my + 4, 0xFFAAAAAA);
        }
        for (DailyStatusPayload.Line line : status.missions()) {
            int color = line.completed() ? 0xFF5BE35B : 0xFFE0E0E0;
            graphics.text(font, Component.literal(line.completed() ? "✔ " : "").append(line.title()), x + 8, my, color);
            String count = line.progress() + "/" + line.amount();
            graphics.text(font, count, x + WIDTH - 8 - font.width(count), my, 0xFFAAAAAA);
            int barW = WIDTH - 16;
            int filled = (int) (barW * Math.min(1.0, line.progress() / (double) Math.max(1, line.amount())));
            graphics.fill(x + 8, my + 11, x + 8 + barW, my + 14, BAR_BG);
            graphics.fill(x + 8, my + 11, x + 8 + filled, my + 14, line.completed() ? BAR_DONE : BAR);
            my += ROW;
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Client handler for the payload: update an open screen, or open a new one. */
    public static void handle(DailyStatusPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof DailyScreen screen) {
            screen.setStatus(payload);
            screen.rebuildWidgets();
        } else {
            minecraft.setScreen(new DailyScreen(payload));
        }
    }
}
