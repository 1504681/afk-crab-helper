package com.afkcrabhelper;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("afkcrabhelper")
public interface AfkCrabHelperConfig extends Config
{
    @ConfigItem(
        keyName = "enableOverlay",
        name = "Enable Overlay",
        description = "Show distraction-reducing overlay when training on crabs"
    )
    default boolean enableOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "overlayColor",
        name = "Overlay Color",
        description = "Color of the distraction overlay"
    )
    default Color overlayColor()
    {
        return Color.BLACK;
    }

    @ConfigItem(
        keyName = "overlayOpacity",
        name = "Overlay Opacity",
        description = "Opacity of the distraction overlay (0-255)"
    )
    @Range(min = 0, max = 255)
    default int overlayOpacity()
    {
        return 180;
    }

    @ConfigItem(
        keyName = "activationDelay",
        name = "Activation Delay",
        description = "Seconds to wait before showing overlay after crab interaction starts"
    )
    @Range(min = 0, max = 30)
    default int activationDelay()
    {
        return 3;
    }

    @ConfigItem(
        keyName = "hideDelay",
        name = "Hide Delay",
        description = "Seconds to wait before hiding overlay after crab interaction stops"
    )
    @Range(min = 0, max = 30)
    default int hideDelay()
    {
        return 5;
    }

    @ConfigItem(
        keyName = "showTimer",
        name = "Show Timer",
        description = "Display a timer showing how long you've been training"
    )
    default boolean showTimer()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showCrabName",
        name = "Show Crab Name",
        description = "Display the name of the crab you're currently fighting"
    )
    default boolean showCrabName()
    {
        return true;
    }
}