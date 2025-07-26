package com.afkcrabhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class AfkCrabHelperOverlay extends Overlay
{
    private final Client client;
    private final AfkCrabHelperPlugin plugin;
    private final AfkCrabHelperConfig config;
    private final PanelComponent panelComponent = new PanelComponent();
    private Instant trainingStartTime;

    @Inject
    public AfkCrabHelperOverlay(Client client, AfkCrabHelperPlugin plugin, AfkCrabHelperConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
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

        // Create full-screen overlay
        Dimension canvasSize = client.getCanvas().getSize();
        
        // Create overlay color with configured opacity
        Color overlayColor = config.overlayColor();
        Color transparentColor = new Color(
            overlayColor.getRed(),
            overlayColor.getGreen(), 
            overlayColor.getBlue(),
            config.overlayOpacity()
        );
        
        // Fill the entire screen with overlay
        graphics.setColor(transparentColor);
        graphics.fillRect(0, 0, canvasSize.width, canvasSize.height);

        // Render info panel if any info options are enabled
        if (config.showTimer() || config.showCrabName())
        {
            renderInfoPanel(graphics);
        }

        return canvasSize;
    }

    private void renderInfoPanel(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();
        panelComponent.setBackgroundColor(new Color(0, 0, 0, 100));

        // Show crab name if enabled
        if (config.showCrabName())
        {
            String crabName = getCurrentCrabName();
            if (crabName != null)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Training on:")
                    .right(crabName)
                    .build());
            }
        }

        // Show timer if enabled
        if (config.showTimer() && trainingStartTime != null)
        {
            Duration trainingDuration = Duration.between(trainingStartTime, Instant.now());
            String timeString = formatDuration(trainingDuration);
            
            panelComponent.getChildren().add(LineComponent.builder()
                .left("Training time:")
                .right(timeString)
                .build());
        }

        // Position panel in top-left corner
        panelComponent.setPreferredLocation(new java.awt.Point(10, 10));
        panelComponent.render(graphics);
    }

    private String getCurrentCrabName()
    {
        if (client.getLocalPlayer() == null)
        {
            return null;
        }

        Actor target = client.getLocalPlayer().getInteracting();
        if (target instanceof NPC)
        {
            return ((NPC) target).getName();
        }

        return null;
    }

    private String formatDuration(Duration duration)
    {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (hours > 0)
        {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        else
        {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
}