package com.afkcrabhelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "AFK Crab Helper",
    description = "Gemstone Crab AFK overlay with a countdown until the crab dies, HP percentage and flash alerts",
    tags = {"afk", "crab", "training", "overlay", "distraction", "gemstone"}
)
public class AfkCrabHelperPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private AfkCrabHelperConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private AfkCrabHelperOverlay overlay;

    private boolean isInteractingWithCrab = false;
    private long lastCrabInteraction = 0;
    private long overlayStartTime = 0;
    
    // Crab tracking variables
    private NPC currentCrab = null;
    private int lastSeenHealthRatio = 0;
    private long timerStartTime = 0;
    private double initialTimeMinutes = 0.0;

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(overlay);
        log.info("AFK Crab Helper started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(overlay);
        isInteractingWithCrab = false;
        log.info("AFK Crab Helper stopped!");
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (client.getLocalPlayer() == null)
        {
            return;
        }

        checkCrabInteraction();
        checkCrabStatus();
    }

    private void checkCrabInteraction()
    {
        Actor target = client.getLocalPlayer().getInteracting();
        boolean currentlyInteractingWithCrab = false;
        NPC targetCrab = null;

        if (target instanceof NPC)
        {
            NPC npc = (NPC) target;
            String npcName = npc.getName();
            
            if (npcName != null && isCrabNpc(npcName))
            {
                currentlyInteractingWithCrab = true;
                lastCrabInteraction = System.currentTimeMillis();
                targetCrab = npc;

                int npcHealthRatio = npc.getHealthRatio();
                int npcHealthScale = npc.getHealthScale();
                // -1 means the client has no health bar for this NPC (freshly spawned, nobody has hit it yet)
                boolean healthKnown = npcHealthRatio >= 0 && npcHealthScale > 0;

                if (currentCrab != npc)
                {
                    // New crab: start the countdown. No health bar yet means it just spawned, so assume full.
                    currentCrab = npc;
                    lastSeenHealthRatio = npcHealthRatio;
                    timerStartTime = System.currentTimeMillis();
                    initialTimeMinutes = healthKnown ? minutesForHealth(npcHealthRatio, npcHealthScale) : 10.0;
                }
                else if (healthKnown && lastSeenHealthRatio != npcHealthRatio)
                {
                    // Health bar ticked: resync the countdown so drift stays within one bar unit
                    lastSeenHealthRatio = npcHealthRatio;
                    timerStartTime = System.currentTimeMillis();
                    initialTimeMinutes = minutesForHealth(npcHealthRatio, npcHealthScale);
                }
            }
        }

        // Check if we recently interacted with a crab (within hide delay period)
        long timeSinceLastInteraction = System.currentTimeMillis() - lastCrabInteraction;
        if (timeSinceLastInteraction <= config.hideDelay() * 1000)
        {
            currentlyInteractingWithCrab = true;
        }
        
        // Also check if we have a valid crab target and are still actively interacting
        // Only keep overlay up if we're actually still targeting the crab
        if (currentCrab != null && client.getNpcs().contains(currentCrab) && target == currentCrab)
        {
            currentlyInteractingWithCrab = true;
        }

        // Handle activation delay for overlay
        if (currentlyInteractingWithCrab && !isInteractingWithCrab)
        {
            // Starting interaction - set overlay start time
            overlayStartTime = System.currentTimeMillis();
        }
        else if (!currentlyInteractingWithCrab && isInteractingWithCrab)
        {
            // Stopping interaction - reset crab tracking
            currentCrab = null;
            timerStartTime = 0;
            initialTimeMinutes = 0.0;
        }
        
        // Update overlay start time if we don't have one but should be showing
        if (currentlyInteractingWithCrab && overlayStartTime == 0)
        {
            overlayStartTime = System.currentTimeMillis();
        }

        isInteractingWithCrab = currentlyInteractingWithCrab;
    }
    
    private void checkCrabStatus()
    {
        // Check if our tracked crab is still valid
        if (currentCrab != null)
        {
            // Check if crab is no longer valid (burrowed or moved away)
            if (!client.getNpcs().contains(currentCrab))
            {
                // Reset tracking
                isInteractingWithCrab = false;
                currentCrab = null;
                timerStartTime = 0;
                initialTimeMinutes = 0.0;
            }
        }
    }

    private boolean isCrabNpc(String npcName)
    {
        if (npcName == null) return false;
        String lowerName = npcName.toLowerCase();
        return lowerName.equals("gemstone crab");
    }
    
    // The Gemstone Crab's bar drains on a fixed ~10 minute schedule regardless of damage,
    // so health% maps straight to minutes left. Half a bar unit is subtracted because the
    // displayed ratio rounds up; testing showed that tracks the real burrow time better.
    private static double minutesForHealth(int healthRatio, int healthScale)
    {
        double healthPercent = (healthRatio - 0.5) / healthScale * 100.0;
        return Math.max(0, healthPercent / 10.0);
    }
    
    @Subscribe
    public void onInteractingChanged(InteractingChanged event)
    {
        // Track when player starts interacting with something new
        if (event.getSource() == client.getLocalPlayer())
        {
            Actor newTarget = event.getTarget();
            if (newTarget instanceof NPC)
            {
                NPC npc = (NPC) newTarget;
                if (isCrabNpc(npc.getName()))
                {
                    // Starting to interact with a crab
                    lastCrabInteraction = System.currentTimeMillis();
                }
            }
        }
    }
    
    @Subscribe
    public void onNpcDespawned(NpcDespawned event)
    {
        NPC npc = event.getNpc();
        
        // Check if it's our current crab that despawned
        if (npc == currentCrab)
        {
            // Crab despawned - stop overlay immediately
            isInteractingWithCrab = false;
            currentCrab = null;
            timerStartTime = 0;
            initialTimeMinutes = 0.0;
        }
    }
    
    

    public boolean isShowingOverlay()
    {
        if (!isInteractingWithCrab)
        {
            return false;
        }
        
        // Check activation delay
        long timeSinceOverlayStart = System.currentTimeMillis() - overlayStartTime;
        return timeSinceOverlayStart >= config.activationDelay() * 1000;
    }
    
    public String getDisplayText()
    {
        if (currentCrab == null)
        {
            return null;
        }

        int healthRatio = currentCrab.getHealthRatio();
        int healthScale = currentCrab.getHealthScale();
        boolean healthKnown = healthRatio >= 0 && healthScale > 0;
        double minutes = remainingMinutes(healthKnown ? (double) healthRatio / healthScale * 100.0 : 100.0);
        // No health bar yet (fresh spawn): estimate HP from the countdown instead of showing 0% / "Crab dead"
        double healthPercent = healthKnown ? (double) healthRatio / healthScale * 100.0 : Math.min(100.0, minutes * 10.0);

        if (healthKnown && healthRatio == 0)
        {
            return "Crab dead";
        }

        switch (config.displayMode())
        {
            case HP_PERCENTAGE:
                return String.format("%.1f%% HP", healthPercent);

            case TIME_REMAINING:
                if (minutes < 1.0)
                {
                    return String.format("%.0f seconds", minutes * 60);
                }
                return formatTime(minutes) + " remaining";

            case BOTH:
                String timeStr = minutes < 1.0 ? String.format("%.0fs", minutes * 60) : formatTime(minutes);
                return String.format("%.1f%% HP | %s", healthPercent, timeStr);

            default:
                return null;
        }
    }

    private double remainingMinutes(double fallbackHealthPercent)
    {
        if (timerStartTime > 0)
        {
            double elapsedMinutes = (System.currentTimeMillis() - timerStartTime) / 1000.0 / 60.0;
            return Math.max(0, initialTimeMinutes - elapsedMinutes);
        }
        return fallbackHealthPercent / 10.0;
    }

    private static String formatTime(double minutes)
    {
        int mins = (int) minutes;
        int secs = (int) ((minutes - mins) * 60);
        return String.format("%d:%02d", mins, secs);
    }
    
    public boolean shouldFlash()
    {
        if (!config.enableFlash() || currentCrab == null)
        {
            return false;
        }
        
        int healthRatio = currentCrab.getHealthRatio();
        int healthScale = currentCrab.getHealthScale();
        if (healthRatio < 0 || healthScale <= 0)
        {
            return false; // no health bar yet, nothing to alert on
        }
        double healthPercent = (double) healthRatio / healthScale * 100.0;
        
        return healthPercent <= config.flashThreshold();
    }
    
    public NPC getCurrentCrab()
    {
        return currentCrab;
    }

    @Provides
    AfkCrabHelperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AfkCrabHelperConfig.class);
    }
}