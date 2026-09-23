package com.demonslayer;

import static org.junit.Assert.assertEquals;
import com.google.gson.Gson;
import java.io.StringReader;
import java.util.Collections;
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
}
