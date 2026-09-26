# Demon Slayer

Demon Slayer is a plugin inspired by the anime, obviously, with progression derived from it. Eligible kills earn Demon Slayer XP equal to the NPC's combat level. The RuneScape skill curve carries the player from level 1 through the Corps ranks to **Hashira at level 99**. XP caps at 200 million; kills and boss milestones continue after the cap. The plugin changes no game mechanics.

## What the plugin records

- **Normal exterminations:** eligible NPC deaths observed after installing the plugin. A death needs recent damage attributed to your client; a nearby death or unconfirmed boss phase does not count.
- **Boss exterminations:** one shared record per boss for live kills and imported RuneLite KC. Live credit needs local damage and a KC advance or loot confirmation. Kills that cannot be confirmed live may be recovered by a later boss sync.
- **Bestiary:** counts and actual awarded XP by NPC or boss, sorted by contribution. A monster tagged both demon and undead appears in both category sections, but counts once toward total kills and XP.
- **Cosmetic rewards:** the sidebar frame and named rank medallion change automatically with your Corps rank; boss crest tiers appear in its header. Level 50 unlocks the Corps XP counter and an optional local title below the overhead player name. Hashira adds mastery border treatments. The Rewards tab shows what is equipped and what is next. These render only in your RuneLite client.
- **Kasugai Crow:** a short on-screen notice in RuneLite's managed top-center overlay area for levels, promotions, refreshes, and milestones. The latest message also appears on Profile. Its optional sound is off by default.

The Profile, Records, and Rewards tabs are in the crow sidebar panel. The local title and crow sound can be changed in the RuneLite plugin configuration.

## Boss record sync

Progress is saved automatically for the active RuneScape profile after each credited kill and import, and reloaded when that profile logs in. Known RuneLite KC is imported automatically on profile load. **Refresh RuneLite KC** is an optional manual check if RuneLite learns more KC during the same session; it is not a save button. The plugin matches eligible bosses against the bundled Wiki dataset and adds only KC above its already-accounted record. Repeating refresh awards nothing; a lower RuneLite KC never reduces the record. Normal monster history cannot be imported. Unresolved KC names are available from a details button rather than occupying the Profile tab.

Some RuneLite KC values become known only after viewing an in-game boss log or earning another kill. [RuneLite's Chat Commands documentation](https://github.com/runelite/runelite/wiki/Chat-Commands) explains this limitation. If multiple eligible Wiki variants share a boss name, historical XP uses the **lowest eligible combat level**. For example, Wiki Vorkath variants include level 392 and level 732, so its historical KC uses 392 XP per kill; a confirmed live level 732 kill uses 732 XP. Ineligible Wiki boss names and known non-eligible RuneLite KC labels (Gauntlet, Guardians of the Rift, Lunar Chest, Mimic, and Royal Titans) are ignored. Other names that cannot be resolved appear in the sync result. Sync needs no Wiki connection at runtime.
