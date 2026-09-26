package com.demonslayer;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BreathingProgression
{
	// Persisted IDs are deliberately independent of labels and enum ordering.
	static final class Style
	{
		final String id;
		final String name;
		final String parent;
		Style(String id, String name, String parent)
		{
			this.id = id;
			this.name = name;
			this.parent = parent;
		}
	}

	private static final Map<String, Style> STYLES;
	static
	{
		Map<String, Style> styles = new LinkedHashMap<>();
		for (Style style : Arrays.asList(
			new Style("insect", "Insect", "flower"),
			new Style("flower", "Flower", "water"),
			new Style("serpent", "Serpent", "water"),
			new Style("water", "Water", "sun"),
			new Style("love", "Love", "flame"),
			new Style("flame", "Flame", "sun"),
			new Style("mist", "Mist", "wind"),
			new Style("beast", "Beast", "wind"),
			new Style("wind", "Wind", "sun"),
			new Style("sound", "Sound", "thunder"),
			new Style("thunder", "Thunder", "sun"),
			new Style("stone", "Stone", "sun"),
			new Style("sun", "Sun", null)))
		{
			styles.put(style.id, style);
		}
		STYLES = Collections.unmodifiableMap(styles);
	}

	private BreathingProgression()
	{
	}

	static List<Style> styles()
	{
		return Collections.unmodifiableList(Arrays.asList(STYLES.values().toArray(new Style[0])));
	}

	static Style style(String id)
	{
		return STYLES.get(id);
	}

	static boolean available(Progression.Profile profile, String id)
	{
		Style style = style(id);
		if (style == null || profile.breathingPointsAvailable <= 0
			|| profile.unlockedBreathingStyles.contains(id))
		{
			return false;
		}
		if ("sun".equals(id))
		{
			return profile.unlockedBreathingStyles.containsAll(Arrays.asList(
				"water", "flame", "wind", "thunder", "stone"));
		}
		for (Style other : STYLES.values())
		{
			if (id.equals(other.parent) && profile.unlockedBreathingStyles.contains(other.id))
			{
				return true;
			}
		}
		// Any outer node may start a branch; Stone is itself a principal outer node.
		return "insect".equals(id) || "serpent".equals(id) || "love".equals(id)
			|| "mist".equals(id) || "beast".equals(id) || "sound".equals(id)
			|| "stone".equals(id);
	}

	static boolean unlock(Progression.Profile profile, String id)
	{
		if (!available(profile, id))
		{
			return false;
		}
		profile.breathingPointsAvailable--;
		profile.unlockedBreathingStyles.add(id);
		return true;
	}

	static boolean equip(Progression.Profile profile, String id)
	{
		if (id != null && !profile.unlockedBreathingStyles.contains(id))
		{
			return false;
		}
		profile.activeBreathingStyle = id;
		return true;
	}

	static boolean valid(Progression.Profile profile)
	{
		for (String id : profile.unlockedBreathingStyles)
		{
			if (style(id) == null)
			{
				return false;
			}
		}
		return profile.activeBreathingStyle == null
			|| profile.unlockedBreathingStyles.contains(profile.activeBreathingStyle);
	}
}
