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

	/** Explicit user refresh restores historical KC even after a progression reset. */
	Result refreshKnown(Progression.Profile profile, Map<String, Integer> known)
	{
		profile.resetBossKc = false;
		profile.bossKcBaselines.clear();
		return importKnown(profile, known);
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
				if (!catalog.isExcludedKc(entry.getKey()))
				{
					result.unresolved.add(entry.getKey());
				}
				continue;
			}
			String key = key(monster);
			Progression.Record record = profile.bossRecords.get(key);
			long recordedKills = record == null ? 0 : record.kills;
			Integer baseline = profile.bossKcBaselines.get(key);
			if (profile.resetBossKc && baseline == null)
			{
				profile.bossKcBaselines.put(key, (int) Math.max(0, (long) entry.getValue() - recordedKills));
				continue;
			}
			long delta = Math.max(0, (long) entry.getValue() - (baseline == null ? 0 : baseline) - recordedKills);
			if (delta == 0)
			{
				continue;
			}
			record = Progression.getOrCreate(profile.bossRecords, key, monster);
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
		Integer baseline = profile.bossKcBaselines.get(key);
		if (profile.resetBossKc && baseline != null && beforeKc >= 0 && afterKc > beforeKc
			&& baseline == afterKc && record.kills == 0)
		{
			// A first KC observation can arrive between death and loot confirmation.
			baseline = Math.max(0, beforeKc);
			profile.bossKcBaselines.put(key, baseline);
		}
		if (afterKc > beforeKc && record.kills >= (long) afterKc - (baseline == null ? 0 : baseline))
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
