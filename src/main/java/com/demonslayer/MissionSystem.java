package com.demonslayer;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiPredicate;

/** Mission bookkeeping consumes only kills already accepted by the plugin's attribution path. */
final class MissionSystem
{
	static final long XP_THRESHOLD = 1_000;
	static final int TARGET_XP_BUDGET = 800;
	private static final List<String> NORMAL = Arrays.asList("Imp", "Icefiend", "Zombie", "Skeleton",
		"Ghost", "Ankou", "Lesser demon", "Greater demon", "Black demon", "Hellhound",
		"Pyrefiend", "Demonic gorilla", "Tormented demon", "Crawling Hand", "Banshee",
		"Bloodveld", "Aberrant spectre", "Nechryael", "Abyssal demon", "Loar Shade",
		"Greater Nechryael", "Feral vampyre", "Vyrewatch", "Vyrewatch Sentinel", "Zombie pirate");
	private static final List<String> BOSSES = Arrays.asList("Vorkath", "Duke Sucellus",
		"Abyssal Sire", "Cerberus", "Skotizo", "Yama");

	static List<String> targets(boolean boss)
	{
		return java.util.Collections.unmodifiableList(boss ? BOSSES : NORMAL);
	}

	static final class Mission
	{
		String target;
		boolean boss;
		String location;
		int[] npcIds;
		int[] regionIds;
		int required;
		int progress;

		Mission copy()
		{
			Mission copy = new Mission();
			copy.target = target;
			copy.boss = boss;
			copy.location = location;
			copy.npcIds = npcIds == null ? null : npcIds.clone();
			copy.regionIds = regionIds == null ? null : regionIds.clone();
			copy.required = required;
			copy.progress = progress;
			return copy;
		}
	}

	static final class Result
	{
		String message;
		boolean assigned;
		boolean completed;
		boolean point;
	}

	private static final class Location
	{
		String target;
		String name;
		int[] npcIds;
		int[] regionIds;
		int weight;
	}

	private static final class Locations
	{
		List<Location> locations;
	}

	private final MonsterCatalog catalog;
	private final Random random;
	private final BiPredicate<String, String> eligibleLocation;
	private final Map<String, List<Location>> locations = new HashMap<>();

	MissionSystem(MonsterCatalog catalog, Gson gson, Random random,
		BiPredicate<String, String> eligibleLocation)
	{
		this.catalog = catalog;
		this.random = random;
		this.eligibleLocation = eligibleLocation;
		try (InputStreamReader reader = new InputStreamReader(
			MissionSystem.class.getResourceAsStream("/com/demonslayer/mission_locations.json"),
			StandardCharsets.UTF_8))
		{
			Locations parsed = gson.fromJson(reader, Locations.class);
			for (Location location : parsed.locations)
			{
				if (location.target == null || location.name == null || location.npcIds == null
					|| location.npcIds.length == 0 || location.weight <= 0
					|| (location.regionIds != null && location.regionIds.length == 0))
				{
					throw new IllegalStateException("Invalid mission location");
				}
				if (location.regionIds != null)
				{
					for (int regionId : location.regionIds)
					{
						if (regionId <= 0)
						{
							throw new IllegalStateException("Invalid mission region ID");
						}
					}
				}
				for (int id : location.npcIds)
				{
					MonsterCatalog.Monster monster = catalog.byId(id);
					if (monster == null || monster.boss || !MonsterCatalog.normalize(monster.name)
						.equals(MonsterCatalog.normalize(location.target)))
					{
						throw new IllegalStateException("Mission location has an invalid NPC ID: " + id);
					}
				}
				locations.computeIfAbsent(MonsterCatalog.normalize(location.target), ignored -> new ArrayList<>())
					.add(location);
			}
		}
		catch (java.io.IOException e)
		{
			throw new IllegalStateException("Unable to load mission locations", e);
		}
	}

	Result liveKill(Progression.Profile profile, MonsterCatalog.Monster monster, int npcId, int regionId,
		int combatLevel, BiPredicate<String, Boolean> eligible)
	{
		Result result = new Result();
		profile.missionXpBank += combatLevel;
		Mission active = profile.activeMission;
		if (active != null && active.boss == monster.boss
			&& MonsterCatalog.normalize(active.target).equals(MonsterCatalog.normalize(monster.name))
			&& matchesLocation(active, npcId, regionId))
		{
			active.progress++;
			if (active.progress >= active.required)
			{
				profile.activeMission = null;
				profile.missionsCompleted++;
				result.completed = true;
				result.point = random.nextInt(100) < nextChance(profile.breathingDryStreak);
				if (result.point)
				{
					profile.breathingPointsAvailable++;
					profile.breathingDryStreak = 0;
				}
				else
				{
					profile.breathingDryStreak++;
				}
				result.message = result.point ? "Mission complete. Breathing Point earned!"
					: "Mission complete. Breathing Insight eluded you.";
			}
		}
		if (profile.activeMission == null && profile.missionXpBank >= XP_THRESHOLD)
		{
			Mission next = assign(eligible);
			if (next != null)
			{
				profile.missionXpBank -= XP_THRESHOLD;
				profile.activeMission = next;
				result.assigned = true;
				if (!result.completed)
				{
					result.message = "New mission: " + next.target + ".";
				}
			}
		}
		return result;
	}

	private Mission assign(BiPredicate<String, Boolean> eligible)
	{
		List<MonsterCatalog.Monster> candidates = new ArrayList<>();
		boolean boss = random.nextInt(100) < 15;
		for (String name : boss ? BOSSES : NORMAL)
		{
			MonsterCatalog.Monster monster = catalog.byName(name, boss);
			if (monster != null && eligible.test(name, boss))
			{
				candidates.add(monster);
			}
		}
		if (candidates.isEmpty() && boss)
		{
			for (String name : NORMAL)
			{
				MonsterCatalog.Monster monster = catalog.byName(name, false);
				if (monster != null && eligible.test(name, false))
				{
					candidates.add(monster);
				}
			}
		}
		if (candidates.isEmpty())
		{
			return null;
		}
		MonsterCatalog.Monster chosen = candidates.get(random.nextInt(candidates.size()));
		Mission mission = new Mission();
		mission.target = chosen.name;
		mission.boss = chosen.boss;
		mission.required = chosen.boss ? Math.max(1, Math.min(2, Math.round((float) TARGET_XP_BUDGET / chosen.level)))
			: Math.max(3, Math.min(30, Math.round((float) TARGET_XP_BUDGET / chosen.level)));
		if (!chosen.boss)
		{
			List<Location> choices = locations.get(MonsterCatalog.normalize(chosen.name));
			if (choices != null)
			{
				List<Location> available = new ArrayList<>();
				for (Location location : choices)
				{
					if (eligibleLocation.test(chosen.name, location.name))
					{
						available.add(location);
					}
				}
				if (!available.isEmpty())
				{
					Location location = chooseLocation(available);
					mission.location = location.name;
					mission.npcIds = location.npcIds.clone();
					mission.regionIds = location.regionIds == null ? null : location.regionIds.clone();
				}
			}
		}
		return mission;
	}

	private Location chooseLocation(List<Location> choices)
	{
		int total = 0;
		for (Location choice : choices)
		{
			total += choice.weight;
		}
		int roll = random.nextInt(total);
		for (Location choice : choices)
		{
			roll -= choice.weight;
			if (roll < 0)
			{
				return choice;
			}
		}
		throw new IllegalStateException("No mission location");
	}

	Mission generateEligibleMission(BiPredicate<String, Boolean> eligible)
	{
		return assign(eligible);
	}

	private static boolean matchesLocation(Mission mission, int npcId, int regionId)
	{
		if (mission.npcIds == null && mission.regionIds == null)
		{
			return true;
		}
		if (mission.npcIds != null)
		{
			boolean found = false;
			for (int id : mission.npcIds)
			{
				found |= id == npcId;
			}
			if (!found)
			{
				return false;
			}
		}
		if (mission.regionIds != null)
		{
			for (int id : mission.regionIds)
			{
				if (id == regionId)
				{
					return true;
				}
			}
			return false;
		}
		return true;
	}

	static int nextChance(int dryStreak)
	{
		return dryStreak >= 5 ? 100 : 20 + 15 * dryStreak;
	}

	static boolean valid(Mission mission)
	{
		if (mission.target == null || !(mission.boss ? BOSSES : NORMAL).contains(mission.target)
			|| mission.required <= 0 || mission.required > (mission.boss ? 2 : 30)
			|| mission.progress < 0 || mission.progress >= mission.required)
		{
			return false;
		}
		if (mission.location != null && (mission.location.isEmpty() || mission.npcIds == null
			|| mission.npcIds.length == 0))
		{
			return false;
		}
		if (mission.npcIds != null)
		{
			for (int id : mission.npcIds)
			{
				if (id <= 0)
				{
					return false;
				}
			}
		}
		if (mission.regionIds != null)
		{
			if (mission.regionIds.length == 0 || mission.location == null)
			{
				return false;
			}
			for (int id : mission.regionIds)
			{
				if (id <= 0)
				{
					return false;
				}
			}
		}
		return true;
	}
}
