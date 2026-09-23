package com.demonslayer;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.runelite.client.config.ConfigManager;

final class ProfileStore
{
	static final String GROUP = "demon-slayer-progression";
	private static final String KEY = "profile-v1";

	interface Backend
	{
		String profileKey();
		String read(String group, String key);
		void write(String group, String key, String value);
	}

	static Backend runelite(ConfigManager configManager)
	{
		return new Backend()
		{
			@Override
			public String profileKey()
			{
				return configManager.getRSProfileKey();
			}

			@Override
			public String read(String group, String key)
			{
				return configManager.getRSProfileConfiguration(group, key);
			}

			@Override
			public void write(String group, String key, String value)
			{
				configManager.setRSProfileConfiguration(group, key, value);
			}
		};
	}

	private final Gson gson;
	private final Backend backend;
	private String activeKey;
	private Progression.Profile active;

	ProfileStore(Gson gson, Backend backend)
	{
		this.gson = gson;
		this.backend = backend;
	}

	Progression.Profile loadActive()
	{
		String key = backend.profileKey();
		if (key == null)
		{
			activeKey = null;
			active = null;
			return null;
		}
		if (key.equals(activeKey) && active != null)
		{
			return active;
		}
		String json = backend.read(GROUP, KEY);
		Progression.Profile parsed = json == null ? new Progression.Profile()
			: gson.fromJson(json, Progression.Profile.class);
		if (parsed == null || parsed.version != 1 || parsed.normalRecords == null || parsed.bossRecords == null)
		{
			throw new JsonParseException("Unsupported or malformed Demon Slayer profile");
		}
		validate(parsed.normalRecords);
		validate(parsed.bossRecords);
		if (Progression.xp(parsed) > Progression.MAX_XP)
		{
			throw new JsonParseException("Demon Slayer profile exceeds XP cap");
		}
		activeKey = key;
		active = parsed;
		return active;
	}

	private static void validate(java.util.Map<String, Progression.Record> records)
	{
		for (java.util.Map.Entry<String, Progression.Record> entry : records.entrySet())
		{
			Progression.Record record = entry.getValue();
			if (entry.getKey() == null || record == null || record.name == null || record.level <= 0
				|| record.kills < 0 || record.xp < 0 || (!record.demon && !record.undead))
			{
				throw new JsonParseException("Malformed Demon Slayer record " + entry.getKey());
			}
		}
	}

	void saveActive()
	{
		if (active == null || activeKey == null || !activeKey.equals(backend.profileKey()))
		{
			throw new IllegalStateException("No active RuneScape profile to save");
		}
		backend.write(GROUP, KEY, gson.toJson(active));
	}

	void clear()
	{
		activeKey = null;
		active = null;
	}
}
