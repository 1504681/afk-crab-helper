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
    description = "Gemstone Crab AFK timer overlay shows time remaining until crab dies. Also supports Sand Crabs, Rock Crabs, and Ammonite Crabs with HP percentage display and flash alerts",
    tags = {"afk", "crab", "training", "overlay", "distraction", "gemstone", "sand", "rock", "ammonite"}
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
                
                // If this is a new crab or we weren't tracking before, update tracking
                if (currentCrab != npc)
                {
                    currentCrab = npc;
                    // Start countdown timer
                    timerStartTime = System.currentTimeMillis();
                    // Calculate initial time based on health%
                    int healthRatio = Math.max(0, npc.getHealthRatio());
                    int healthScale = Math.max(1, npc.getHealthScale());
                    double healthPercent = (double) healthRatio / healthScale * 100.0;
                    initialTimeMinutes = healthPercent / 10.0;
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
        return lowerName.equals("sand crab") ||
               lowerName.equals("rock crab") ||
               lowerName.equals("ammonite crab") ||
               lowerName.equals("gemstone crab");
    }
    
    private boolean isGemstoneCrab(String npcName)
    {
        return npcName != null && npcName.toLowerCase().equals("gemstone crab");
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
        
        // Get crab health info
        int healthRatio = Math.max(0, currentCrab.getHealthRatio());
        int healthScale = Math.max(1, currentCrab.getHealthScale());
        double healthPercent = (double) healthRatio / healthScale * 100.0;
        
        if (healthPercent <= 0)
        {
            return "Crab dead";
        }
        
        AfkCrabHelperConfig.DisplayMode mode = config.displayMode();
        
        switch (mode)
        {
            case HP_PERCENTAGE:
                return String.format("%.1f%% HP", healthPercent);
                
            case TIME_REMAINING:
                // Use countdown timer if we have one, otherwise calculate from health%
                double minutes;
                if (timerStartTime > 0) {
                    // Calculate remaining time from countdown
                    long elapsedMs = System.currentTimeMillis() - timerStartTime;
                    double elapsedMinutes = elapsedMs / 1000.0 / 60.0;
                    minutes = Math.max(0, initialTimeMinutes - elapsedMinutes);
                } else {
                    // Fallback to health% calculation
                    minutes = healthPercent / 10.0;
                }
                
                if (minutes < 1.0) {
                    return String.format("%.0f seconds", minutes * 60);
                } else {
                    int mins = (int) minutes;
                    int secs = (int) ((minutes - mins) * 60);
                    return String.format("%d:%02d remaining", mins, secs);
                }
                
            case BOTH:
                // Use countdown timer if we have one, otherwise calculate from health%
                double mins;
                if (timerStartTime > 0) {
                    // Calculate remaining time from countdown
                    long elapsedMs = System.currentTimeMillis() - timerStartTime;
                    double elapsedMinutes = elapsedMs / 1000.0 / 60.0;
                    mins = Math.max(0, initialTimeMinutes - elapsedMinutes);
                } else {
                    // Fallback to health% calculation
                    mins = healthPercent / 10.0;
                }
                
                String timeStr;
                if (mins < 1.0) {
                    timeStr = String.format("%.0fs", mins * 60);
                } else {
                    int m = (int) mins;
                    int s = (int) ((mins - m) * 60);
                    timeStr = String.format("%d:%02d", m, s);
                }
                return String.format("%.1f%% HP | %s", healthPercent, timeStr);
                
            default:
                return null;
        }
    }
    
    public boolean shouldFlash()
    {
        if (!config.enableFlash() || currentCrab == null)
        {
            return false;
        }
        
        int healthRatio = Math.max(0, currentCrab.getHealthRatio());
        int healthScale = Math.max(1, currentCrab.getHealthScale());
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