package com.afkcrabhelper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.events.GameTick;
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
    }

    private void checkCrabInteraction()
    {
        Actor target = client.getLocalPlayer().getInteracting();
        boolean currentlyInteractingWithCrab = false;

        if (target instanceof NPC)
        {
            NPC npc = (NPC) target;
            String npcName = npc.getName();
            
            if (npcName != null && isCrabNpc(npcName))
            {
                currentlyInteractingWithCrab = true;
                lastCrabInteraction = System.currentTimeMillis();
            }
        }

        // Check if we recently interacted with a crab (within delay period)
        long timeSinceLastInteraction = System.currentTimeMillis() - lastCrabInteraction;
        if (timeSinceLastInteraction <= config.activationDelay() * 1000)
        {
            currentlyInteractingWithCrab = true;
        }

        isInteractingWithCrab = currentlyInteractingWithCrab;
    }

    private boolean isCrabNpc(String npcName)
    {
        String lowerName = npcName.toLowerCase();
        return lowerName.contains("sand crab") ||
               lowerName.contains("rock crab") ||
               lowerName.contains("ammonite crab") ||
               lowerName.contains("gemstone crab");
    }

    public boolean isShowingOverlay()
    {
        return isInteractingWithCrab && config.enableOverlay();
    }

    @Provides
    AfkCrabHelperConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AfkCrabHelperConfig.class);
    }
}