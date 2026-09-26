package com.demonslayer;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.runelite.client.config.ConfigManager;

final class ProfileStore
{
	static final String GROUP = "demon-slayer-progression";
	private static final String V1_KEY = "profile-v1";
	private static final String V2_KEY = "profile-v2";
	private static final String RESET_KEY = "reset-v2";

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
		String json = backend.read(GROUP, V2_KEY);
		Progression.Profile parsed;
		if (json != null)
		{
			parsed = gson.fromJson(json, Progression.Profile.class);
			validate(parsed, 2);
		}
		else
		{
			String legacy = backend.read(GROUP, RESET_KEY) == null ? backend.read(GROUP, V1_KEY) : null;
			parsed = legacy == null ? new Progression.Profile() : gson.fromJson(legacy, Progression.Profile.class);
			if (backend.read(GROUP, RESET_KEY) != null)
			{
				parsed.resetBossKc = true;
			}
			if (legacy != null)
			{
				validate(parsed, 1);
				parsed.version = 2;
			}
			// Leave v1 untouched; write the migration before accepting new awards.
			backend.write(GROUP, V2_KEY, gson.toJson(parsed));
		}
		activeKey = key;
		active = parsed;
		return active;
	}

	private static void validate(Progression.Profile parsed, int version)
	{
		if (parsed == null || parsed.version != version || parsed.normalRecords == null
			|| parsed.bossRecords == null || parsed.bossKcBaselines == null
			|| parsed.unlockedBreathingStyles == null || parsed.missionXpBank < 0
			|| parsed.missionsCompleted < 0 || parsed.breathingDryStreak < 0
			|| parsed.breathingPointsAvailable < 0)
		{
			throw new JsonParseException("Unsupported or malformed Demon Slayer profile");
		}
		validate(parsed.normalRecords);
		validate(parsed.bossRecords);
		for (java.util.Map.Entry<String, Integer> entry : parsed.bossKcBaselines.entrySet())
		{
			if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0)
			{
				throw new JsonParseException("Invalid boss KC baseline");
			}
		}
		if (version == 2 && !BreathingProgression.valid(parsed))
		{
			throw new JsonParseException("Invalid Breathing state");
		}
		if (version == 2 && parsed.activeMission != null && !MissionSystem.valid(parsed.activeMission))
		{
			throw new JsonParseException("Invalid mission state");
		}
		if (Progression.xp(parsed) > Progression.MAX_XP)
		{
			throw new JsonParseException("Demon Slayer profile exceeds XP cap");
		}
	}

	private static void validate(java.util.Map<String, Progression.Record> records)
	{
		for (java.util.Map.Entry<String, Progression.Record> entry : records.entrySet())
		{
			Progression.Record record = entry.getValue();
			if (entry.getKey() == null || record == null || record.name == null || record.level <= 0
				|| record.kills < 0 || record.xp < 0 || (!record.demon && !record.undead && !record.vampire))
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
		backend.write(GROUP, V2_KEY, gson.toJson(active));
	}

	void resetActive(Progression.Profile replacement)
	{
		if (active == null || activeKey == null || !activeKey.equals(backend.profileKey()))
		{
			throw new IllegalStateException("No active RuneScape profile to reset");
		}
		backend.write(GROUP, V2_KEY, gson.toJson(replacement));
		backend.write(GROUP, RESET_KEY, "true");
		active = replacement;
	}

	void clear()
	{
		activeKey = null;
		active = null;
	}
}
