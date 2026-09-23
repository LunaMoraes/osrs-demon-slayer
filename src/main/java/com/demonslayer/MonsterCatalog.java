package com.demonslayer;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MonsterCatalog
{
	static final String RESOURCE = "/com/demonslayer/monsters.json";

	static final class Monster
	{
		String name;
		String page;
		int level;
		String[] attributes;
		boolean boss;

		boolean demon()
		{
			return hasAttribute("demon");
		}

		boolean undead()
		{
			return hasAttribute("undead");
		}

		private boolean hasAttribute(String expected)
		{
			if (attributes != null)
			{
				for (String attribute : attributes)
				{
					if (expected.equals(attribute))
					{
						return true;
					}
				}
			}
			return false;
		}
	}

	private static final class Document
	{
		String source;
		Map<String, Monster> npcs;
		List<String> excludedBossNames;
	}

	private final Map<Integer, Monster> byId;
	private final Map<String, Monster> bossesByName;
	private final Set<String> excludedBossNames;

	private MonsterCatalog(Map<Integer, Monster> byId, Map<String, Monster> bossesByName,
		Set<String> excludedBossNames)
	{
		this.byId = Collections.unmodifiableMap(byId);
		this.bossesByName = Collections.unmodifiableMap(bossesByName);
		this.excludedBossNames = Collections.unmodifiableSet(excludedBossNames);
	}

	static MonsterCatalog load(Gson gson)
	{
		InputStream stream = MonsterCatalog.class.getResourceAsStream(RESOURCE);
		if (stream == null)
		{
			throw new IllegalStateException("Missing bundled monster catalog");
		}
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
		{
			return parse(gson, reader);
		}
		catch (java.io.IOException e)
		{
			throw new IllegalStateException("Unable to read bundled monster catalog", e);
		}
	}

	static MonsterCatalog parse(Gson gson, java.io.Reader reader)
	{
		Document document = gson.fromJson(reader, Document.class);
		if (document == null || document.npcs == null || document.npcs.isEmpty())
		{
			throw new JsonParseException("Monster catalog is empty");
		}
		Map<Integer, Monster> ids = new HashMap<>();
		Map<String, Monster> bosses = new HashMap<>();
		for (Map.Entry<String, Monster> row : document.npcs.entrySet())
		{
			Monster monster = row.getValue();
			int id;
			try
			{
				id = Integer.parseInt(row.getKey());
			}
			catch (NumberFormatException e)
			{
				throw new JsonParseException("Invalid NPC ID " + row.getKey(), e);
			}
			if (id <= 0 || monster == null || monster.name == null || monster.page == null
				|| monster.level <= 0 || (!monster.demon() && !monster.undead()))
			{
				throw new JsonParseException("Invalid monster catalog row " + row.getKey());
			}
			ids.put(id, monster);
			if (monster.boss)
			{
				addBoss(bosses, monster.name, monster);
				addBoss(bosses, monster.page, monster);
			}
		}
		Set<String> excluded = new HashSet<>();
		if (document.excludedBossNames != null)
		{
			for (String name : document.excludedBossNames)
			{
				if (name != null)
				{
					excluded.add(normalize(name));
				}
			}
		}
		return new MonsterCatalog(ids, bosses, excluded);
	}

	private static void addBoss(Map<String, Monster> bosses, String name, Monster monster)
	{
		String key = normalize(name);
		Monster previous = bosses.get(key);
		if (previous == null || monster.level < previous.level
			|| (monster.level == previous.level && monster.page.compareTo(previous.page) < 0))
		{
			bosses.put(key, monster);
		}
	}

	static String normalize(String value)
	{
		return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	Monster byId(int id)
	{
		return byId.get(id);
	}

	Monster bossByName(String name)
	{
		return bossesByName.get(normalize(name));
	}

	boolean isExcludedBoss(String name)
	{
		return excludedBossNames.contains(normalize(name));
	}

	int size()
	{
		return byId.size();
	}
}
