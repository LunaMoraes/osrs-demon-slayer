package com.demonslayer;

import net.runelite.api.Client;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;

final class MissionRequirements
{
	private final Client client;

	MissionRequirements(Client client)
	{
		this.client = client;
	}

	boolean eligible(String target, boolean boss)
	{
		int slayer = client.getRealSkillLevel(Skill.SLAYER);
		switch (target)
		{
			case "Vorkath": return finished(Quest.DRAGON_SLAYER_II);
			case "Duke Sucellus": return finished(Quest.DESERT_TREASURE_II__THE_FALLEN_EMPIRE);
			case "Abyssal Sire": return slayer >= 85;
			case "Cerberus": return slayer >= 91;
			case "Yama": return finished(Quest.A_KINGDOM_DIVIDED);
			case "Demonic gorilla": return finished(Quest.MONKEY_MADNESS_II);
			case "Tormented demon": return finished(Quest.WHILE_GUTHIX_SLEEPS);
			case "Vyrewatch Sentinel": return finished(Quest.SINS_OF_THE_FATHER);
			case "Vyrewatch": return finished(Quest.DARKNESS_OF_HALLOWVALE);
			case "Loar Shade": return finished(Quest.PRIEST_IN_PERIL);
			case "Crawling Hand": return slayer >= 5 && finished(Quest.PRIEST_IN_PERIL);
			case "Banshee": return slayer >= 15 && finished(Quest.PRIEST_IN_PERIL);
			case "Pyrefiend": return slayer >= 30;
			case "Bloodveld": return slayer >= 50 && finished(Quest.PRIEST_IN_PERIL);
			case "Aberrant spectre": return slayer >= 60 && finished(Quest.PRIEST_IN_PERIL);
			case "Nechryael": return slayer >= 80 && finished(Quest.PRIEST_IN_PERIL);
			case "Greater Nechryael": return slayer >= 80;
			case "Feral vampyre": return finished(Quest.PRIEST_IN_PERIL);
			case "Abyssal demon": return slayer >= 85;
			default: return true;
		}
	}

	boolean eligibleLocation(String target, String location)
	{
		if ("Sisterhood Sanctuary".equals(location))
		{
			return finished(Quest.PRIEST_IN_PERIL);
		}
		if ("Slayer Tower (main floors)".equals(location))
		{
			return finished(Quest.PRIEST_IN_PERIL);
		}
		if ("Braindeath Island".equals(location))
		{
			return finished(Quest.RUM_DEAL);
		}
		if ("Harmony Island".equals(location))
		{
			return finished(Quest.THE_GREAT_BRAIN_ROBBERY);
		}
		return true;
	}

	private boolean finished(Quest quest)
	{
		return quest.getState(client) == QuestState.FINISHED;
	}
}
