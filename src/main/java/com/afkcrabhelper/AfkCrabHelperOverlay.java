package com.afkcrabhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class AfkCrabHelperOverlay extends Overlay
{
    private final Client client;
    private final AfkCrabHelperPlugin plugin;
    private final AfkCrabHelperConfig config;
    private Instant trainingStartTime;

    @Inject
    public AfkCrabHelperOverlay(Client client, AfkCrabHelperPlugin plugin, AfkCrabHelperConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DETACHED);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!plugin.isShowingOverlay())
        {
            trainingStartTime = null;
            return null;
        }

        // Initialize training start time
        if (trainingStartTime == null)
        {
            trainingStartTime = Instant.now();
        }

        // Create overlay color with configured opacity
        Color overlayColor = config.overlayColor();
        Color transparentColor = new Color(
            overlayColor.getRed(),
            overlayColor.getGreen(), 
            overlayColor.getBlue(),
            config.overlayOpacity()
        );
        
        // Fill the entire screen with overlay (using a large size to ensure full coverage)
        graphics.setColor(transparentColor);
        graphics.fillRect(0, 0, 2560, 1440);

        // Render AFK time in center if enabled and available
        if (config.showAfkTime())
        {
            renderAfkTimeCenter(graphics);
        }

        return new Dimension(2560, 1440);
    }

    private void renderAfkTimeCenter(Graphics2D graphics)
    {
        String afkTime = plugin.getAfkTimeRemaining();
        if (afkTime == null)
        {
            return;
        }

        // Enable anti-aliasing for smooth text
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Set large font
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 36);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        // Get the game canvas dimensions for proper centering
        int canvasWidth = client.getCanvasWidth();
        int canvasHeight = client.getCanvasHeight();
        
        // Calculate center position based on game canvas
        int textWidth = metrics.stringWidth(afkTime);
        int x = (canvasWidth - textWidth) / 2;
        int y = canvasHeight / 2;

        // Draw shadow (slightly offset)
        graphics.setColor(Color.BLACK);
        graphics.drawString(afkTime, x + 2, y + 2);

        // Draw main text
        Color textColor = afkTime.equals("Calculating...") ? Color.YELLOW : Color.WHITE;
        graphics.setColor(textColor);
        graphics.drawString(afkTime, x, y);
    }

}