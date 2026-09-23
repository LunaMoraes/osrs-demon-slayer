package com.demonslayer;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(name = "Demon Slayer")
public class DemonSlayerPlugin extends Plugin
{
	@Override
	protected void startUp()
	{
		log.debug("Demon Slayer started");
	}

	@Override
	protected void shutDown()
	{
		log.debug("Demon Slayer stopped");
	}
}
