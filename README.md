# Demon Slayer

Demon Slayer is a plugin inspired by the anime, obviously, with progression derived from it. Eligible kills earn Demon Slayer XP equal to the NPC's combat level. The RuneScape skill curve carries the player from level 1 through the Corps ranks to **Hashira at level 99**. XP caps at 200 million; kills and boss milestones continue after the cap. The plugin changes no game mechanics.

## What the plugin records

- **Normal exterminations:** eligible NPC deaths observed after installing the plugin. A death needs recent damage attributed to your client; a nearby death or unconfirmed boss phase does not count.
- **Boss exterminations:** one shared record per boss for live kills and imported RuneLite KC. Live credit needs local damage and a KC advance or loot confirmation. Kills that cannot be confirmed live may be recovered by a later boss sync.
- **Bestiary:** counts and actual awarded XP by NPC or boss, sorted by contribution. Demons, undead, and vampires have their own sections. A monster with more than one tag appears in each matching section, but counts once toward total kills and XP.
- **Cosmetic rewards:** the sidebar frame and named rank medallion change automatically with your Corps rank; boss crest tiers appear in its header. Level 50 unlocks the Corps XP counter and an optional local title below the overhead player name. Hashira adds mastery border treatments. Reward status is on Profile and Breathing. These render only in your RuneLite client.
- **Kasugai Crow:** a short on-screen notice in RuneLite's managed top-center overlay area for levels, promotions, refreshes, and milestones. The latest message also appears on Profile. Its optional sound is off by default.

The Profile, Missions, Breathing, and Records tabs are in the crow sidebar panel. The local title and crow sound can be changed in the RuneLite plugin configuration.

## Kasugai Crow missions

Every 1,000 raw Demon Slayer XP from confirmed live kills funds a mission. XP keeps banking while a mission is active and after the normal 200 million XP cap. Imported boss KC never funds missions. Each assignment asks for a small number of kills based on the target's combat level. The mission pool has 25 normal target families, including feral vampyres, Vyrewatch, Vyrewatch Sentinels, and zombie pirates, plus six eligible bosses. The 50 normal location options cover early-game areas such as Stronghold of Security, Draynor Manor, and Ice Mountain; demon areas such as the Chasm of Fire and Catacombs of Kourend; the Slayer Tower, Morytania, islands, and optional Wilderness sites. Assignments use the listed NPC variants and, where IDs are shared across areas, a region check or a named choice of the areas that share those IDs. Quest-locked sites are offered only after their access quest is complete. Existing active missions keep their saved target, location, and progress.

Completing a mission rolls for a Breathing Point: 20%, 35%, 50%, 65%, 80%, then guaranteed after five failed rolls. The Missions tab shows the active assignment, bank, completed count, and next chance. Quest and Slayer level requirements are checked before an assignment is generated; some encounters still require their usual in-game items or matching Slayer task.

## Breathing Styles

Spend points in the square Breathing Tree opened from the Breathing tab. Start from any outer style, then unlock connected styles toward the center. Sun Breathing requires Water, Flame, Wind, Thunder, and Stone first. Moon is outside this progression. Unlocking and equipping are separate; one unlocked style can be active at a time. A style adds a brief local visual effect **after** a confirmed kill. It gives no combat benefit or mechanic hints.

## Reset progression

The Profile tab has a **Reset** action with two confirmations, including typing `RESET`. It clears Demon Slayer XP, kills, missions, and Breathing progress for the active RuneScape profile. Your OSRS account and RuneLite boss KC stay as they are. Known KC becomes a historical baseline so old kills do not return on refresh. If RuneLite learns a boss KC only after the reset, its first observation establishes that boss's baseline instead of importing old kills.

## Boss record sync

Progress is saved automatically for the active RuneScape profile after each credited kill and import, and reloaded when that profile logs in. Known RuneLite KC is imported automatically on profile load. **Refresh RuneLite KC** is an optional manual check if RuneLite learns more KC during the same session; it is not a save button. The plugin matches eligible bosses against the bundled Wiki dataset and adds only KC above its already-accounted record. Repeating refresh awards nothing; a lower RuneLite KC never reduces the record. Normal monster history cannot be imported. Unresolved KC names are available from a details button rather than occupying the Profile tab.

Some RuneLite KC values become known only after viewing an in-game boss log or earning another kill. [RuneLite's Chat Commands documentation](https://github.com/runelite/runelite/wiki/Chat-Commands) explains this limitation. If multiple eligible Wiki variants share a boss name, historical XP uses the **lowest eligible combat level**. For example, Wiki Vorkath variants include level 392 and level 732, so its historical KC uses 392 XP per kill; a confirmed live level 732 kill uses 732 XP. Ineligible Wiki boss names and known non-eligible RuneLite KC labels (Gauntlet, Guardians of the Rift, Lunar Chest, Mimic, and Royal Titans) are ignored. Other names that cannot be resolved appear in the sync result. Sync needs no Wiki connection at runtime.
