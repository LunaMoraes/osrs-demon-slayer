package com.demonslayer;

import java.util.HashMap;
import java.util.Map;

/** Transient evidence for one NPC instance, keyed by the caller's spawn token. */
final class KillAttribution
{
	private static final int MAX_AGE_TICKS = 10;

	private static final class Evidence
	{
		int lastLocalHit = -1;
		boolean dead;
	}

	private final Map<Long, Evidence> active = new HashMap<>();

	void spawn(long token)
	{
		active.put(token, new Evidence());
	}

	void localHit(long token, int tick)
	{
		Evidence evidence = active.get(token);
		if (evidence != null)
		{
			evidence.lastLocalHit = tick;
		}
	}

	boolean death(long token, int tick)
	{
		Evidence evidence = active.get(token);
		if (evidence == null || evidence.dead)
		{
			return false;
		}
		evidence.dead = true;
		return evidence.lastLocalHit >= 0 && tick >= evidence.lastLocalHit
			&& tick - evidence.lastLocalHit <= MAX_AGE_TICKS;
	}

	void despawn(long token)
	{
		active.remove(token);
	}

	void clear()
	{
		active.clear();
	}
}
