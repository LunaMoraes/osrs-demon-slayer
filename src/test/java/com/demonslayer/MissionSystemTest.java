package com.demonslayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import java.util.Random;
import org.junit.Test;

public class MissionSystemTest
{
	private final MonsterCatalog catalog = MonsterCatalog.load(new Gson());
	private final MissionSystem missions = new MissionSystem(catalog, new Gson(), new Random()
	{
		@Override public int nextInt(int bound) { return 0; }
	}, (target, location) -> true);

	@Test
	public void pyrefiendAssignmentsSelectExactlyOneLocationAndRepairOldMission()
	{
		String[] names = {"Fremennik Slayer Dungeon", "Isle of Souls", "Smoke Dungeon", "Sisterhood Sanctuary"};
		int[] regions = {11164, 9006, 12946, 15512};
		for (int i = 0; i < names.length; i++)
		{
			final String destination = names[i];
			MissionSystem system = new MissionSystem(catalog, new Gson(), new Random(1),
				(target, location) -> destination.equals(location));
			MissionSystem.Mission mission = system.generateEligibleMission((target, boss) -> !boss && "Pyrefiend".equals(target));
			assertEquals(destination, mission.location);
			Progression.Profile profile = new Progression.Profile();
			profile.activeMission = mission;
			system.liveKill(profile, catalog.byId(433), 433, -1, 43, (target, boss) -> false);
			assertEquals(0, mission.progress);
			system.liveKill(profile, catalog.byId(433), 433, regions[i], 43, (target, boss) -> false);
			assertEquals(1, mission.progress);
			mission.location = "Fremennik, Isle of Souls, Smoke Dungeon or Sisterhood Sanctuary";
			mission.regionIds = null;
			int required = mission.required;
			assertTrue(system.repairLegacyLocation(mission));
			assertEquals(destination, mission.location);
			assertEquals(1, mission.progress);
			assertEquals(required, mission.required);
			assertFalse(system.repairLegacyLocation(mission));
		}
	}

	@Test
	public void wildernessAssignmentCountsOnlyItsNpcVariant()
	{
		Progression.Profile profile = new Progression.Profile();
		MissionSystem.Mission mission = new MissionSystem.Mission();
		mission.target = "Ankou";
		mission.location = "Wilderness Slayer Cave";
		mission.npcIds = new int[]{7864};
		mission.required = 2;
		profile.activeMission = mission;
		MonsterCatalog.Monster elsewhere = catalog.byId(2514);
		MonsterCatalog.Monster cave = catalog.byId(7864);
		missions.liveKill(profile, elsewhere, 2514, -1, 75, (name, boss) -> true);
		assertEquals(0, mission.progress);
		missions.liveKill(profile, cave, 7864, -1, 98, (name, boss) -> true);
		assertEquals(1, mission.progress);
		MissionSystem.Result completed = missions.liveKill(profile, cave, 7864, -1, 98, (name, boss) -> true);
		assertTrue(completed.completed);
		assertTrue(completed.point);
		assertEquals(1, profile.breathingPointsAvailable);
		assertEquals(1, profile.missionsCompleted);
		assertEquals(271, profile.missionXpBank);
		assertFalse(completed.assigned);
	}

	@Test
	public void rawLiveXpAssignsAfterOneThousand()
	{
		Progression.Profile profile = new Progression.Profile();
		MonsterCatalog.Monster imp = catalog.byName("Imp", false);
		for (int i = 0; i < 999; i++)
		{
			missions.liveKill(profile, imp, 555, -1, 1, (name, boss) -> false);
		}
		assertEquals(999, profile.missionXpBank);
		assertEquals(null, profile.activeMission);
		missions.liveKill(profile, imp, 555, -1, 1, (name, boss) -> !boss);
		assertEquals(0, profile.missionXpBank);
		assertTrue(profile.activeMission != null);
	}

	@Test
	public void fifthDryFailureGuaranteesNextPoint()
	{
		assertEquals(20, MissionSystem.nextChance(0));
		assertEquals(80, MissionSystem.nextChance(4));
		assertEquals(100, MissionSystem.nextChance(5));
	}

	@Test
	public void slayerTowerAssignmentRequiresNpcAndRegion()
	{
		Progression.Profile profile = new Progression.Profile();
		MissionSystem.Mission mission = new MissionSystem.Mission();
		mission.target = "Bloodveld";
		mission.location = "Slayer Tower";
		mission.npcIds = new int[]{484, 485, 486, 487};
		mission.regionIds = new int[]{13623};
		mission.required = 2;
		profile.activeMission = mission;
		MonsterCatalog.Monster bloodveld = catalog.byId(484);
		missions.liveKill(profile, bloodveld, 484, 9880, 76, (name, boss) -> true);
		assertEquals(0, mission.progress);
		missions.liveKill(profile, bloodveld, 484, 13623, 76, (name, boss) -> true);
		assertEquals(1, mission.progress);
		assertTrue(MissionSystem.valid(mission));
	}

	@Test
	public void assignsNonWildernessLocations()
	{
		MissionSystem.Mission catacombs = missions.generateEligibleMission(
			(name, boss) -> !boss && name.equals("Ankou"));
		assertEquals("Stronghold of Security", catacombs.location);
		assertEquals(2514, catacombs.npcIds[0]);
		MissionSystem.Mission tower = missions.generateEligibleMission(
			(name, boss) -> !boss && name.equals("Bloodveld"));
		assertEquals("Slayer Tower (main floors)", tower.location);
		assertEquals(13623, tower.regionIds[0]);
	}

	@Test
	public void inaccessibleLocationIsSkipped()
	{
		MissionSystem gated = new MissionSystem(catalog, new Gson(), new Random()
		{
			@Override public int nextInt(int bound) { return 0; }
		}, (target, location) -> !"Braindeath Island".equals(location));
		MissionSystem.Mission pirate = gated.generateEligibleMission(
			(name, boss) -> !boss && name.equals("Zombie pirate"));
		assertFalse("Braindeath Island".equals(pirate.location));
	}
}
