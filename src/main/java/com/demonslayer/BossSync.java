package com.demonslayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class BossSync
{
	static final class Result
	{
		long importedKills;
		long awardedXp;
		final List<String> unresolved = new ArrayList<>();
	}

	private final MonsterCatalog catalog;

	BossSync(MonsterCatalog catalog)
	{
		this.catalog = catalog;
	}

	Result importKnown(Progression.Profile profile, Map<String, Integer> known)
	{
		Result result = new Result();
		for (Map.Entry<String, Integer> entry : new TreeMap<>(known).entrySet())
		{
			if (entry.getValue() == null || entry.getValue() < 0)
			{
				continue;
			}
			MonsterCatalog.Monster monster = catalog.bossByName(entry.getKey());
			if (monster == null)
			{
				if (!catalog.isExcludedBoss(entry.getKey()))
				{
					result.unresolved.add(entry.getKey());
				}
				continue;
			}
			String key = key(monster);
			Progression.Record record = Progression.getOrCreate(profile.bossRecords, key, monster);
			long delta = Math.max(0, (long) entry.getValue() - record.kills);
			result.importedKills += delta;
			result.awardedXp += Progression.award(profile, record, delta, monster.level);
		}
		Collections.sort(result.unresolved);
		return result;
	}

	/** The before/after KC pair prevents a sync that arrived first from double-crediting this death. */
	long liveKill(Progression.Profile profile, MonsterCatalog.Monster monster, int beforeKc, int afterKc,
		int liveCombatLevel)
	{
		String key = key(monster);
		Progression.Record record = Progression.getOrCreate(profile.bossRecords, key, monster);
		if (afterKc > beforeKc && record.kills >= afterKc)
		{
			return 0;
		}
		return Progression.award(profile, record, 1, liveCombatLevel);
	}

	static String key(MonsterCatalog.Monster monster)
	{
		return MonsterCatalog.normalize(monster.page);
	}
}
