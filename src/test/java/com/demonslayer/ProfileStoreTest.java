package com.demonslayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class ProfileStoreTest
{
	private static final class MemoryBackend implements ProfileStore.Backend
	{
		String active;
		final Map<String, String> values = new HashMap<>();

		@Override public String profileKey() { return active; }
		@Override public String read(String group, String key) { return values.get(active + group + key); }
		@Override public void write(String group, String key, String value) { values.put(active + group + key, value); }
	}

	@Test
	public void isolatesAndReloadsProfiles()
	{
		MemoryBackend backend = new MemoryBackend();
		ProfileStore store = new ProfileStore(new Gson(), backend);
		assertNull(store.loadActive());
		backend.active = "account-a";
		Progression.Profile first = store.loadActive();
		Progression.Record record = new Progression.Record("Demon", true, false, 12);
		first.normalRecords.put("1", record);
		Progression.award(first, record, 1, 12);
		store.saveActive();
		backend.active = "account-b";
		assertEquals(0, Progression.kills(store.loadActive()));
		store.clear();
		backend.active = "account-a";
		assertEquals(1, Progression.kills(store.loadActive()));
		assertEquals(12, Progression.xp(store.loadActive()));
	}

	@Test(expected = JsonParseException.class)
	public void refusesCorruptProfileWithoutReplacingIt()
	{
		MemoryBackend backend = new MemoryBackend();
		backend.active = "account-a";
		backend.values.put("account-a" + ProfileStore.GROUP + "profile-v1", "{\"version\":99}");
		ProfileStore store = new ProfileStore(new Gson(), backend);
		try
		{
			store.loadActive();
		}
		finally
		{
			assertEquals("{\"version\":99}", backend.values.get("account-a"
				+ ProfileStore.GROUP + "profile-v1"));
		}
	}
}
