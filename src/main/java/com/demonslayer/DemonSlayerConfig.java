package com.demonslayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("demon-slayer-display")
public interface DemonSlayerConfig extends Config
{
	enum CounterStyle
	{
		AUTO, CLASSIC, CORPS
	}

	@ConfigItem(keyName = "localTitle", name = "Local Corps title",
		description = "Show your unlocked Corps title beside your own character")
	default boolean localTitle()
	{
		return false;
	}

	@ConfigItem(keyName = "crowSound", name = "Crow message sound",
		description = "Play a short sound for important Corps messages")
	default boolean crowSound()
	{
		return false;
	}

	@ConfigItem(keyName = "counterStyle", name = "XP counter style",
		description = "Choose the classic counter or the Corps counter unlocked at level 50")
	default CounterStyle counterStyle()
	{
		return CounterStyle.AUTO;
	}
	@ConfigItem(keyName = "resetProgression", name = "Reset progression…",
		description = "Open the confirmation dialog to erase this profile's Demon Slayer progress",
		position = 100)
	default boolean resetProgression()
	{
		return false;
	}
}
