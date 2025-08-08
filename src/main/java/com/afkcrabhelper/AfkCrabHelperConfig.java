package com.afkcrabhelper;

import java.awt.Color;
import java.awt.Font;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("afkcrabhelper")
public interface AfkCrabHelperConfig extends Config
{
    @ConfigSection(
        name = "Display",
        description = "Configure what information to display",
        position = 0
    )
    String displaySection = "display";

    @ConfigSection(
        name = "Appearance",
        description = "Configure overlay appearance",
        position = 1
    )
    String appearanceSection = "appearance";

    @ConfigSection(
        name = "Font",
        description = "Configure text font settings",
        position = 2
    )
    String fontSection = "font";

    @ConfigSection(
        name = "Flash Alert",
        description = "Configure low HP flash alert",
        position = 3
    )
    String flashSection = "flash";

    @ConfigSection(
        name = "Timing",
        description = "Configure overlay timing",
        position = 4
    )
    String timingSection = "timing";

    @ConfigItem(
        keyName = "displayMode",
        name = "Display Mode",
        description = "What information to display in the overlay",
        section = displaySection,
        position = 0
    )
    default DisplayMode displayMode()
    {
        return DisplayMode.TIME_REMAINING;
    }

    @ConfigItem(
        keyName = "overlayColor",
        name = "Overlay Color",
        description = "Color of the distraction overlay",
        section = appearanceSection,
        position = 0
    )
    default Color overlayColor()
    {
        return Color.BLACK;
    }

    @ConfigItem(
        keyName = "overlayOpacity",
        name = "Overlay Opacity",
        description = "Opacity of the distraction overlay (0-255)",
        section = appearanceSection,
        position = 1
    )
    @Range(min = 0, max = 255)
    default int overlayOpacity()
    {
        return 200;
    }


    @ConfigItem(
        keyName = "fontFamily",
        name = "Font Family",
        description = "Font family for the overlay text",
        section = fontSection,
        position = 0
    )
    default FontFamily fontFamily()
    {
        return FontFamily.SANS_SERIF;
    }

    @ConfigItem(
        keyName = "fontSize",
        name = "Font Size",
        description = "Size of the overlay text",
        section = fontSection,
        position = 1
    )
    @Range(min = 12, max = 200)
    default int fontSize()
    {
        return 36;
    }

    @ConfigItem(
        keyName = "fontColor",
        name = "Font Color",
        description = "Color of the overlay text",
        section = fontSection,
        position = 2
    )
    default Color fontColor()
    {
        return Color.WHITE;
    }

    @ConfigItem(
        keyName = "fontStyle",
        name = "Font Style",
        description = "Style of the overlay text",
        section = fontSection,
        position = 3
    )
    default FontStyle fontStyle()
    {
        return FontStyle.BOLD;
    }

    @ConfigItem(
        keyName = "enableFlash",
        name = "Enable Low HP Flash",
        description = "Flash the text when crab HP is below threshold",
        section = flashSection,
        position = 0
    )
    default boolean enableFlash()
    {
        return true;
    }

    @ConfigItem(
        keyName = "flashThreshold",
        name = "Flash Threshold %",
        description = "Start flashing when crab HP% falls below this value",
        section = flashSection,
        position = 1
    )
    @Range(min = 1, max = 50)
    default int flashThreshold()
    {
        return 2;
    }

    @ConfigItem(
        keyName = "flashColor",
        name = "Flash Color",
        description = "Color to flash when HP is low",
        section = flashSection,
        position = 2
    )
    default Color flashColor()
    {
        return Color.YELLOW;
    }

    @ConfigItem(
        keyName = "activationDelay",
        name = "Activation Delay",
        description = "Seconds to wait before showing overlay after crab interaction starts",
        section = timingSection,
        position = 0
    )
    @Range(min = 0, max = 30)
    default int activationDelay()
    {
        return 0;
    }

    @ConfigItem(
        keyName = "hideDelay",
        name = "Hide Delay",
        description = "Seconds to wait before hiding overlay after crab interaction stops",
        section = timingSection,
        position = 1
    )
    @Range(min = 0, max = 30)
    default int hideDelay()
    {
        return 0;
    }

    enum DisplayMode
    {
        TIME_REMAINING("Time Remaining"),
        HP_PERCENTAGE("HP Percentage"),
        BOTH("Both");

        private final String name;

        DisplayMode(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    enum FontFamily
    {
        SANS_SERIF("Sans Serif"),
        SERIF("Serif"),
        MONOSPACED("Monospaced"),
        DIALOG("Dialog"),
        DIALOG_INPUT("Dialog Input");

        private final String name;

        FontFamily(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }

        public String getJavaFontName()
        {
            switch (this)
            {
                case SANS_SERIF:
                    return Font.SANS_SERIF;
                case SERIF:
                    return Font.SERIF;
                case MONOSPACED:
                    return Font.MONOSPACED;
                case DIALOG:
                    return Font.DIALOG;
                case DIALOG_INPUT:
                    return Font.DIALOG_INPUT;
                default:
                    return Font.SANS_SERIF;
            }
        }
    }

    enum FontStyle
    {
        PLAIN("Plain"),
        BOLD("Bold"),
        ITALIC("Italic"),
        BOLD_ITALIC("Bold Italic");

        private final String name;

        FontStyle(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }

        public int getJavaFontStyle()
        {
            switch (this)
            {
                case PLAIN:
                    return Font.PLAIN;
                case BOLD:
                    return Font.BOLD;
                case ITALIC:
                    return Font.ITALIC;
                case BOLD_ITALIC:
                    return Font.BOLD | Font.ITALIC;
                default:
                    return Font.PLAIN;
            }
        }
    }

}