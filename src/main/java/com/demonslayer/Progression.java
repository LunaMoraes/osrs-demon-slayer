package com.demonslayer;

import java.util.LinkedHashMap;
import java.util.Map;
import net.runelite.api.Experience;

final class Progression
{
	static final int MAX_XP = Experience.MAX_SKILL_XP;
	static final int HASHIRA_XP = Experience.getXpForLevel(99);
	static final long[] MASTERY_MILESTONES = {20_000_000L, 50_000_000L, 100_000_000L, 200_000_000L};
	static final long[] BOSS_MILESTONES = {100, 500, 1_000, 2_500, 5_000, 10_000};
	static final long[] TOTAL_MILESTONES = {100, 500, 1_000, 5_000, 10_000};
	static final String[] RANKS = {"Mizunoto", "Mizunoe", "Kanoto", "Kanoe", "Tsuchinoto",
		"Tsuchinoe", "Hinoto", "Hinoe", "Kinoto", "Kinoe", "Hashira"};
	static final int[] RANK_LEVELS = {1, 10, 20, 30, 40, 50, 60, 70, 80, 90, 99};

	static final class Record
	{
		String name;
		boolean demon;
		boolean undead;
		int level;
		long kills;
		long xp;

		Record(String name, boolean demon, boolean undead, int level)
		{
			this.name = name;
			this.demon = demon;
			this.undead = undead;
			this.level = level;
		}
	}

	static final class Profile
	{
		int version = 1;
		Map<String, Record> normalRecords = new LinkedHashMap<>();
		Map<String, Record> bossRecords = new LinkedHashMap<>();
		long lastBossSync;
	}

	private Progression()
	{
	}

	static long xp(Profile profile)
	{
		return xp(profile.normalRecords) + xp(profile.bossRecords);
	}

	static long xp(Map<String, Record> records)
	{
		long result = 0;
		for (Record record : records.values())
		{
			result += record.xp;
		}
		return result;
	}

	static long kills(Map<String, Record> records)
	{
		long result = 0;
		for (Record record : records.values())
		{
			result += record.kills;
		}
		return result;
	}

	static long kills(Profile profile)
	{
		return kills(profile.normalRecords) + kills(profile.bossRecords);
	}

	static long categoryKills(Profile profile, boolean demon)
	{
		return categoryKills(profile.normalRecords, demon) + categoryKills(profile.bossRecords, demon);
	}

	private static long categoryKills(Map<String, Record> records, boolean demon)
	{
		long result = 0;
		for (Record record : records.values())
		{
			if (demon ? record.demon : record.undead)
			{
				result += record.kills;
			}
		}
		return result;
	}

	static int level(Profile profile)
	{
		return Math.min(99, Experience.getLevelForXp((int) Math.min(xp(profile), MAX_XP)));
	}

	static int rankIndex(int level)
	{
		for (int i = RANK_LEVELS.length - 1; i >= 0; i--)
		{
			if (level >= RANK_LEVELS[i])
			{
				return i;
			}
		}
		return 0;
	}

	static long mastery(Profile profile)
	{
		return Math.max(0, xp(profile) - HASHIRA_XP);
	}

	static int masteryTier(Profile profile)
	{
		return tier(xp(profile), MASTERY_MILESTONES);
	}

	static int bossTier(Profile profile)
	{
		return tier(kills(profile.bossRecords), BOSS_MILESTONES);
	}

	private static int tier(long progress, long[] milestones)
	{
		int result = 0;
		for (long milestone : milestones)
		{
			if (progress >= milestone)
			{
				result++;
			}
		}
		return result;
	}

	static long award(Profile profile, Record record, long kills, int xpPerKill)
	{
		if (kills <= 0 || xpPerKill <= 0)
		{
			return 0;
		}
		long remaining = Math.max(0, MAX_XP - xp(profile));
		long earned = Math.min(remaining, kills * (long) xpPerKill);
		record.kills += kills;
		record.xp += earned;
		return earned;
	}

	static Record getOrCreate(Map<String, Record> records, String key, MonsterCatalog.Monster monster)
	{
		return records.computeIfAbsent(key,
			ignored -> new Record(monster.name, monster.demon(), monster.undead(), monster.level));
	}

	/** Keep Swing rendering on a stable copy while the client thread records kills. */
	static Profile snapshot(Profile source)
	{
		if (source == null)
		{
			return null;
		}
		Profile copy = new Profile();
		copy.lastBossSync = source.lastBossSync;
		copyRecords(source.normalRecords, copy.normalRecords);
		copyRecords(source.bossRecords, copy.bossRecords);
		return copy;
	}

	private static void copyRecords(Map<String, Record> from, Map<String, Record> to)
	{
		for (Map.Entry<String, Record> entry : from.entrySet())
		{
			Record original = entry.getValue();
			Record record = new Record(original.name, original.demon, original.undead, original.level);
			record.kills = original.kills;
			record.xp = original.xp;
			to.put(entry.getKey(), record);
		}
	}
}
