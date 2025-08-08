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
import net.runelite.api.Client;
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
        
        // Get screen dimensions
        int screenWidth = client.getCanvasWidth();
        int screenHeight = client.getCanvasHeight();
        
        // Fill the entire screen with overlay
        graphics.setColor(transparentColor);
        graphics.fillRect(0, 0, screenWidth, screenHeight);

        // Render display text in center
        renderDisplayText(graphics);

        return new Dimension(screenWidth, screenHeight);
    }

    private void renderDisplayText(Graphics2D graphics)
    {
        String displayText = plugin.getDisplayText();
        if (displayText == null)
        {
            return;
        }

        // Enable anti-aliasing for smooth text
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Use configurable font settings
        String fontFamily = config.fontFamily().getJavaFontName();
        int fontStyle = config.fontStyle().getJavaFontStyle();
        int fontSize = config.fontSize();
        Font font = new Font(fontFamily, fontStyle, fontSize);
        graphics.setFont(font);
        FontMetrics metrics = graphics.getFontMetrics(font);

        // Get the game canvas dimensions for proper centering
        int canvasWidth = client.getCanvasWidth();
        int canvasHeight = client.getCanvasHeight();
        
        // Calculate center position based on game canvas
        int textWidth = metrics.stringWidth(displayText);
        int x = (canvasWidth - textWidth) / 2;
        int y = canvasHeight / 2;

        // Draw shadow (slightly offset)
        graphics.setColor(Color.BLACK);
        graphics.drawString(displayText, x + 2, y + 2);

        // Determine text color - flash if needed
        Color textColor = config.fontColor();
        if (plugin.shouldFlash())
        {
            // Flash between normal color and flash color based on time
            long currentTime = System.currentTimeMillis();
            boolean flashState = (currentTime / 500) % 2 == 0; // Flash every 500ms
            textColor = flashState ? config.flashColor() : config.fontColor();
        }
        
        // Draw main text
        graphics.setColor(textColor);
        graphics.drawString(displayText, x, y);
    }
}