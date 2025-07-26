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
    description = "Provides a distraction-reducing overlay when training on crabs",
    tags = {"afk", "crab", "training", "overlay", "distraction"}
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
    
    // AFK time calculation variables
    private NPC currentCrab = null;
    private int lastCrabHp = -1;
    private long lastHpCheckTime = 0;
    private double calculatedDps = 0.0;
    private long dpsCalculationStartTime = 0;
    private int initialCrabHp = -1;
    private boolean isCalculatingDps = false;
    private long afkTimeCalculatedAt = 0;
    private double baseSecondsRemaining = 0.0;

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
                
                // If this is a new crab or we weren't tracking before, reset tracking
                if (currentCrab != npc)
                {
                    resetAfkCalculation();
                    currentCrab = npc;
                    if (isGemstoneCrab(npcName))
                    {
                        startAfkCalculation(npc);
                    }
                }
                
                // Update HP tracking for AFK calculation
                if (currentCrab != null && isGemstoneCrab(npcName))
                {
                    updateAfkCalculation(npc);
                }
            }
        }

        // Check if we recently interacted with a crab (within hide delay period)
        long timeSinceLastInteraction = System.currentTimeMillis() - lastCrabInteraction;
        if (timeSinceLastInteraction <= config.hideDelay() * 1000)
        {
            currentlyInteractingWithCrab = true;
        }
        
        // Also check if we have a valid crab target even without recent interaction
        // This helps ensure overlay shows when attacking starts
        if (currentCrab != null && client.getNpcs().contains(currentCrab))
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
            resetAfkCalculation();
            currentCrab = null;
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
                // Crab is no longer in the world - likely burrowed
                if (config.notifyOnCrabBurrow() && isGemstoneCrab(currentCrab.getName()))
                {
                    // Send burrow notification
                    client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
                        "The Gemstone Crab burrows away!", null);
                }
                
                // Reset tracking
                isInteractingWithCrab = false;
                currentCrab = null;
                resetAfkCalculation();
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
        // Reset AFK calculation when player starts interacting with something new
        if (event.getSource() == client.getLocalPlayer())
        {
            Actor newTarget = event.getTarget();
            if (newTarget instanceof NPC)
            {
                NPC npc = (NPC) newTarget;
                if (isCrabNpc(npc.getName()))
                {
                    // Starting to interact with a crab - reset calculation
                    resetAfkCalculation();
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
            resetAfkCalculation();
        }
    }
    
    @Subscribe
    public void onChatMessage(ChatMessage chatMessage)
    {
        // Listen for the actual game message about crab burrowing
        if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE)
        {
            String message = chatMessage.getMessage();
            if (message != null && message.contains("burrows") && currentCrab != null && 
                isGemstoneCrab(currentCrab.getName()) && config.notifyOnCrabBurrow())
            {
                // Game already sent the burrow message, but we can add our custom notification
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", 
                    "AFK Crab Helper: Gemstone Crab has burrowed!", null);
            }
        }
    }
    
    private void resetAfkCalculation()
    {
        lastCrabHp = -1;
        lastHpCheckTime = 0;
        calculatedDps = 0.0;
        dpsCalculationStartTime = 0;
        initialCrabHp = -1;
        isCalculatingDps = false;
        afkTimeCalculatedAt = 0;
        baseSecondsRemaining = 0.0;
    }
    
    private void startAfkCalculation(NPC crab)
    {
        if (crab.getHealthRatio() != -1 && crab.getHealthScale() > 0)
        {
            // Better HP calculation - ensure we don't get negative or zero values
            int healthRatio = Math.max(1, crab.getHealthRatio());
            int healthScale = Math.max(1, crab.getHealthScale());
            initialCrabHp = Math.max(1, (healthRatio * healthScale) / 30);
            lastCrabHp = initialCrabHp;
            lastHpCheckTime = System.currentTimeMillis();
            dpsCalculationStartTime = System.currentTimeMillis();
            isCalculatingDps = true;
            log.debug("Starting AFK calculation - Initial HP: {}, Ratio: {}, Scale: {}", initialCrabHp, healthRatio, healthScale);
        }
    }
    
    private void updateAfkCalculation(NPC crab)
    {
        if (!isCalculatingDps || crab.getHealthRatio() == -1)
        {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        // Better HP calculation with bounds checking
        int healthRatio = Math.max(0, crab.getHealthRatio());
        int healthScale = Math.max(1, crab.getHealthScale());
        int currentHp = Math.max(0, (healthRatio * healthScale) / 30);
        
        // Check if crab is dead
        if (currentHp <= 0 || crab.isDead())
        {
            // Crab is dead - stop showing overlay
            isInteractingWithCrab = false;
            currentCrab = null;
            resetAfkCalculation();
            return;
        }
        
        // Only calculate DPS after we have some time elapsed and HP has changed
        if (currentTime - dpsCalculationStartTime >= 3000 && currentHp < lastCrabHp && lastCrabHp > 0)
        {
            long timeElapsed = currentTime - lastHpCheckTime;
            if (timeElapsed >= 2000) // At least 2 seconds between HP checks for more stable readings
            {
                int hpLost = lastCrabHp - currentHp;
                if (hpLost > 0 && hpLost <= 50) // Sanity check - reject unrealistic damage values
                {
                    double dpsThisInterval = (double) hpLost / (timeElapsed / 1000.0);
                    
                    // More conservative moving average and bounds checking
                    if (dpsThisInterval > 0.1 && dpsThisInterval < 20.0) // Reasonable DPS bounds
                    {
                        if (calculatedDps == 0.0)
                        {
                            calculatedDps = dpsThisInterval;
                        }
                        else
                        {
                            // More conservative smoothing
                            calculatedDps = (calculatedDps * 0.8) + (dpsThisInterval * 0.2);
                        }
                        log.debug("DPS updated: {} (interval: {}, hp lost: {}, time: {}ms)", calculatedDps, dpsThisInterval, hpLost, timeElapsed);
                    }
                }
                
                lastCrabHp = currentHp;
                lastHpCheckTime = currentTime;
            }
        }
    }

    public boolean isShowingOverlay()
    {
        if (!isInteractingWithCrab || !config.enableOverlay())
        {
            return false;
        }
        
        // Check activation delay
        long timeSinceOverlayStart = System.currentTimeMillis() - overlayStartTime;
        return timeSinceOverlayStart >= config.activationDelay() * 1000;
    }
    
    public String getAfkTimeRemaining()
    {
        if (currentCrab == null || !config.showAfkTime() || calculatedDps <= 0.0)
        {
            return null;
        }
        
        // Check if we're still in calculation period (6 seconds)
        long timeSinceStart = System.currentTimeMillis() - dpsCalculationStartTime;
        if (timeSinceStart < 6000) // 6 seconds fixed delay
        {
            return "Calculating...";
        }
        
        // Calculate time remaining based on current HP and DPS
        int healthRatio = Math.max(0, currentCrab.getHealthRatio());
        int healthScale = Math.max(1, currentCrab.getHealthScale());
        int currentHp = Math.max(0, (healthRatio * healthScale) / 30);
        
        if (currentHp <= 0)
        {
            return "Crab dead";
        }
        
        // Recalculate more frequently but with bounds checking
        long timeSinceLastCalc = System.currentTimeMillis() - afkTimeCalculatedAt;
        if (afkTimeCalculatedAt == 0 || timeSinceLastCalc > 5000) // Recalculate every 5 seconds
        {
            baseSecondsRemaining = Math.min(600, Math.max(5, currentHp / calculatedDps)); // Bound between 5s and 10min
            afkTimeCalculatedAt = System.currentTimeMillis();
            log.debug("Recalculated AFK time: {}s (HP: {}, DPS: {})", baseSecondsRemaining, currentHp, calculatedDps);
        }
        
        // Calculate countdown based on elapsed time
        long elapsedSinceCalculation = System.currentTimeMillis() - afkTimeCalculatedAt;
        double elapsedSeconds = elapsedSinceCalculation / 1000.0;
        double secondsRemaining = Math.max(0, baseSecondsRemaining - elapsedSeconds);
        
        // If countdown reaches 0 but crab still has HP, recalculate immediately
        if (secondsRemaining <= 0 && currentHp > 0)
        {
            baseSecondsRemaining = Math.min(600, Math.max(5, currentHp / calculatedDps));
            afkTimeCalculatedAt = System.currentTimeMillis();
            secondsRemaining = baseSecondsRemaining;
        }
        
        if (secondsRemaining < 60)
        {
            return String.format("%.0f seconds", secondsRemaining);
        }
        else
        {
            int minutes = (int) (secondsRemaining / 60);
            int seconds = (int) (secondsRemaining % 60);
            return String.format("%d:%02d remaining", minutes, seconds);
        }
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