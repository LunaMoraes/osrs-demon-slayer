# Demon Slayer Corps

Demon Slayer Corps is an unofficial RuneLite progression plugin for demons and undead. Eligible kills earn Demon Slayer XP equal to the NPC's combat level. The RuneScape skill curve carries the player from level 1 through the Corps ranks to **Hashira at level 99** (13,034,431 XP). XP caps at 200 million; kills and boss milestones continue after the cap. The plugin changes no game mechanics.

## What the plugin records

- **Normal exterminations:** eligible NPC deaths observed after installing the plugin. A death needs recent damage attributed to your client; a nearby death or unconfirmed boss phase does not count.
- **Boss exterminations:** one shared record per boss for live kills and imported RuneLite KC. Live credit needs local damage and a KC advance or loot confirmation. Kills that cannot be confirmed live may be recovered by a later boss sync.
- **Bestiary:** counts and actual awarded XP by NPC or boss, sorted by contribution. A monster tagged both demon and undead appears in both category sections, but counts once toward total kills and XP.
- **Cosmetic rewards:** the sidebar frame and named rank medallion change automatically with your Corps rank; boss crest tiers appear in its header. Level 50 unlocks the Corps XP counter and an optional local title below the overhead player name. Hashira adds mastery border treatments. The Rewards tab shows what is equipped and what is next. These render only in your RuneLite client.
- **Kasugai Crow:** a short on-screen notice in RuneLite's managed top-center overlay area for levels, promotions, refreshes, and milestones. The latest message also appears on Profile. Its optional sound is off by default.

The Profile, Records, and Rewards tabs are in the crow sidebar panel. The local title and crow sound can be changed in the RuneLite plugin configuration.

## Boss record sync

Progress is saved automatically for the active RuneScape profile after each credited kill and import, and reloaded when that profile logs in. Known RuneLite KC is imported automatically on profile load. **Refresh RuneLite KC** is an optional manual check if RuneLite learns more KC during the same session; it is not a save button. The plugin matches eligible bosses against the bundled Wiki dataset and adds only KC above its already-accounted record. Repeating refresh awards nothing; a lower RuneLite KC never reduces the record. Normal monster history cannot be imported. Unresolved KC names are available from a details button rather than occupying the Profile tab.

Some RuneLite KC values become known only after viewing an in-game boss log or earning another kill. [RuneLite's Chat Commands documentation](https://github.com/runelite/runelite/wiki/Chat-Commands) explains this limitation. If multiple eligible Wiki variants share a boss name, historical XP uses the **lowest eligible combat level**. For example, Wiki Vorkath variants include level 392 and level 732, so its historical KC uses 392 XP per kill; a confirmed live level 732 kill uses 732 XP. Ineligible Wiki boss names are ignored, and names that cannot be resolved appear in the sync result. Sync needs no Wiki connection at runtime.

## Monster data

The bundled `monsters.json` is generated from the [OSRS Wiki Infobox Monster](https://oldschool.runescape.wiki/w/Template:Infobox_Monster) Bucket data. Only entries with the `demon` or `undead` attribute and a valid combat level are included. The generator also derives boss identities from the Wiki's boss category. NPC IDs are data, not a maintained Java allowlist.

To refresh it, run `python tools/generate_monsters.py`, review the generated diff, then run `python tools/test_generate_monsters.py` and the Gradle tests. The script sends a descriptive User-Agent. Wiki-derived data is attributed to OSRS Wiki contributors under [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/); the plugin source remains BSD-2-Clause.

## Build and test

Use JDK 11 for Gradle. From the project root:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-11.0.32.101-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build
.\gradlew.bat run
```

In IntelliJ IDEA, open this directory as a Gradle project, set the **Gradle JVM to JDK 11**, run the `test` or `build` Gradle task, then run the `run` Gradle task to launch the development client. The development runner registers `DemonSlayerPlugin`. Follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) instructions to log in to that client.

Only the account owner can verify behavior in-game. Check the sidebar while logged in; kill an eligible normal monster and a non-eligible monster; test another player's nearby kill; and confirm a boss live kill. Close and relaunch the development client without pressing Refresh, then check that progress returned and known RuneLite KC was imported automatically. Refresh twice to confirm the second attempt adds no XP and the crow appears at the top center, and verify a second RuneScape profile starts with separate progress. Check the rank frame, named medallion beside the crow icon, boss crest, optional local title below the Player Indicators name, level 99, mastery milestones, and crow sound. The Java build and a clean client launch do not prove these in-game behaviors.
