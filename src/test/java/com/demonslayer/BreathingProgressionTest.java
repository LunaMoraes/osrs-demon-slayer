package com.demonslayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BreathingProgressionTest
{
	@Test
	public void branchesCanStartAtOuterNodesAndSunNeedsFivePrincipals()
	{
		Progression.Profile profile = new Progression.Profile();
		profile.breathingPointsAvailable = 13;
		assertFalse(BreathingProgression.available(profile, "water"));
		for (String id : new String[]{"insect", "flower", "water", "love", "flame", "mist",
			"wind", "sound", "thunder", "stone"})
		{
			assertTrue(BreathingProgression.unlock(profile, id));
		}
		assertTrue(BreathingProgression.available(profile, "sun"));
		assertTrue(BreathingProgression.unlock(profile, "sun"));
		assertTrue(BreathingProgression.equip(profile, "sun"));
		assertFalse(BreathingProgression.equip(profile, "serpent"));
	}
}
