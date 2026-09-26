package com.demonslayer;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class BossSyncTest
{
	private final MonsterCatalog catalog = MonsterCatalog.parse(new Gson(), new StringReader(
		"{\"npcs\":{\"8061\":{\"name\":\"Vorkath\",\"page\":\"Vorkath\",\"level\":732,\"attributes\":[\"undead\"],\"boss\":true}}}"));
	private final BossSync sync = new BossSync(catalog);

	@Test
	public void repeatedLowerAndMissingSyncs()
	{
		Progression.Profile profile = new Progression.Profile();
		assertEquals(5, sync.importKnown(profile, Collections.singletonMap("Vorkath", 5)).importedKills);
		assertEquals(5 * 732, Progression.xp(profile));
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 5)).importedKills);
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 3)).importedKills);
		assertEquals(2, sync.importKnown(profile, Collections.singletonMap("Vorkath", 7)).importedKills);
		assertEquals(7, Progression.kills(profile.bossRecords));
	}

	@Test
	public void liveThenSyncAndSyncThenDeath()
	{
		Progression.Profile profile = new Progression.Profile();
		MonsterCatalog.Monster vorkath = catalog.byId(8061);
		sync.importKnown(profile, Collections.singletonMap("Vorkath", 5));
		assertEquals(732, sync.liveKill(profile, vorkath, 5, 6, 732));
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 6)).importedKills);
		assertEquals(1, sync.importKnown(profile, Collections.singletonMap("Vorkath", 7)).importedKills);
		assertEquals(0, sync.liveKill(profile, vorkath, 6, 7, 732));
		assertEquals(7, Progression.kills(profile.bossRecords));
	}

	@Test
	public void unresolvedNameDoesNotAward()
	{
		Progression.Profile profile = new Progression.Profile();
		BossSync.Result result = sync.importKnown(profile, Collections.singletonMap("Unknown", 100));
		assertEquals(0, result.importedKills);
		assertEquals(1, result.unresolved.size());
		assertEquals(0, Progression.xp(profile));
	}

	@Test
	public void knownIneligibleKcNamesAreIgnoredButUnknownNamesRemainVisible()
	{
		BossSync bundledSync = new BossSync(MonsterCatalog.load(new Gson()));
		Progression.Profile profile = new Progression.Profile();
		Map<String, Integer> known = new HashMap<>();
		for (String name : new String[]{"gauntlet", "guardians of the rift", "lunar chest",
			"mimic", "royal titans"})
		{
			known.put(name, 100);
		}
		known.put("Unknown future boss", 100);
		known.put("Vorkath", 2);
		BossSync.Result result = bundledSync.importKnown(profile, known);
		assertEquals(2, result.importedKills);
		assertEquals(784, result.awardedXp);
		assertEquals(Collections.singletonList("Unknown future boss"), result.unresolved);
		assertEquals(2, Progression.kills(profile.bossRecords));
	}

	@Test
	public void resetBaselineBlocksHistoryButRecoversLaterKills()
	{
		Progression.Profile profile = new Progression.Profile();
		profile.resetBossKc = true;
		profile.bossKcBaselines.put("vorkath", 500);
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 500)).importedKills);
		assertEquals(1, sync.importKnown(profile, Collections.singletonMap("Vorkath", 501)).importedKills);
		assertEquals(1, Progression.kills(profile.bossRecords));
	}

	@Test
	public void missingResetBaselineAccountsForLiveKill()
	{
		Progression.Profile profile = new Progression.Profile();
		profile.resetBossKc = true;
		MonsterCatalog.Monster vorkath = catalog.byId(8061);
		sync.liveKill(profile, vorkath, -1, 501, 732);
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 501)).importedKills);
		assertEquals(Integer.valueOf(500), profile.bossKcBaselines.get("vorkath"));
		assertEquals(1, sync.importKnown(profile, Collections.singletonMap("Vorkath", 502)).importedKills);
	}

	@Test
	public void firstKcSyncBetweenDeathAndLootDoesNotAbsorbTheLiveKill()
	{
		Progression.Profile profile = new Progression.Profile();
		profile.resetBossKc = true;
		MonsterCatalog.Monster vorkath = catalog.byId(8061);
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 501)).importedKills);
		assertEquals(732, sync.liveKill(profile, vorkath, 500, 501, 732));
		assertEquals(Integer.valueOf(500), profile.bossKcBaselines.get("vorkath"));
		assertEquals(0, sync.importKnown(profile, Collections.singletonMap("Vorkath", 501)).importedKills);
	}
}
