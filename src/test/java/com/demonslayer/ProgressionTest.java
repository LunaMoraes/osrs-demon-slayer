package com.demonslayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.StringReader;
import org.junit.Test;

public class ProgressionTest
{
	@Test
	public void levelsAndCap()
	{
		Progression.Profile profile = new Progression.Profile();
		Progression.Record record = new Progression.Record("Demon", true, false, 100);
		profile.normalRecords.put("1", record);
		assertEquals(1, Progression.level(profile));
		Progression.award(profile, record, 1, 83);
		assertEquals(2, Progression.level(profile));
		Progression.award(profile, record, 2_000_000, 100);
		assertEquals(99, Progression.level(profile));
		assertEquals(200_000_000, Progression.xp(profile));
		assertEquals(2_000_001, record.kills);
		assertEquals(200_000_000 - Progression.HASHIRA_XP, Progression.mastery(profile));
	}

	@Test
	public void ranksAndDualAttributes()
	{
		assertEquals("Mizunoto", Progression.RANKS[Progression.rankIndex(1)]);
		assertEquals("Hinoe", Progression.RANKS[Progression.rankIndex(74)]);
		assertEquals("Hashira", Progression.RANKS[Progression.rankIndex(99)]);
		Progression.Profile profile = new Progression.Profile();
		Progression.Record record = new Progression.Record("Both", true, true, 20);
		profile.normalRecords.put("42", record);
		Progression.award(profile, record, 1, 20);
		assertEquals(1, Progression.kills(profile));
		assertEquals(1, Progression.categoryKills(profile, true));
		assertEquals(1, Progression.categoryKills(profile, false));
		assertEquals(20, Progression.xp(profile));
		Progression.Profile display = Progression.snapshot(profile);
		Progression.award(profile, record, 1, 20);
		assertEquals(1, Progression.kills(display));
		assertEquals(2, Progression.kills(profile));
	}

	@Test
	public void masteryAndBossRewardTiersFollowTheirThresholds()
	{
		Progression.Profile profile = new Progression.Profile();
		Progression.Record normal = new Progression.Record("Demon", true, false, 100);
		Progression.Record boss = new Progression.Record("Boss", true, false, 100);
		profile.normalRecords.put("1", normal);
		profile.bossRecords.put("boss", boss);
		assertEquals(0, Progression.masteryTier(profile));
		assertEquals(0, Progression.bossTier(profile));
		Progression.award(profile, normal, 200_000, 100);
		assertEquals(1, Progression.masteryTier(profile));
		Progression.award(profile, boss, 100, 100);
		assertEquals(1, Progression.bossTier(profile));
		Progression.award(profile, normal, 1_800_000, 100);
		assertEquals(4, Progression.masteryTier(profile));
	}

	@Test
	public void parsesAndResolvesLowestEligibleBoss()
	{
		String json = "{\"npcs\":{\"1\":{\"name\":\"Vorkath\",\"page\":\"Vorkath\",\"level\":732,\"attributes\":[\"undead\"],\"boss\":true},"
			+ "\"2\":{\"name\":\"Vorkath\",\"page\":\"Vorkath\",\"level\":600,\"attributes\":[\"undead\"],\"boss\":true}}}";
		MonsterCatalog catalog = MonsterCatalog.parse(new Gson(), new StringReader(json));
		assertEquals(2, catalog.size());
		assertEquals(600, catalog.bossByName("Vorkath").level);
		assertEquals(732, catalog.byId(1).level);
	}

	@Test
	public void bundledWikiCatalogLoadsAndResolvesEligibleVariants()
	{
		MonsterCatalog catalog = MonsterCatalog.load(new Gson());
		assertTrue(catalog.size() > 500);
		assertNotNull(catalog.byId(8061));
		assertEquals(392, catalog.bossByName("Vorkath").level);
		assertEquals(732, catalog.byId(8061).level);
	}

	@Test(expected = JsonParseException.class)
	public void rejectsInvalidCatalogRow()
	{
		MonsterCatalog.parse(new Gson(), new StringReader("{\"npcs\":{\"oops\":{\"name\":\"X\"}}}"));
	}
}
