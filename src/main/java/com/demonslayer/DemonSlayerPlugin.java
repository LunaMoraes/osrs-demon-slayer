package com.demonslayer;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(name = "Demon Slayer", description = "A persistent Demon Slayer Corps skill and bestiary",
	internalName = "demon-slayer", tags = {"demon", "undead", "progression", "bestiary"})
public class DemonSlayerPlugin extends Plugin
{
	private static final int BOSS_EVIDENCE_TICKS = 10;

	private static final class TrackedNpc
	{
		long token;
		MonsterCatalog.Monster monster;
		int npcId;
		int combatLevel;
		int beforeKc = -1;
		int deathTick;
		boolean lootSeen;
		LocalPoint deathLocation;
		int deathPlane;
		int deathRegionId;
		int size;
		int height;
	}

	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private ClientToolbar clientToolbar;
	@Inject private OverlayManager overlayManager;
	@Inject private ConfigManager configManager;
	@Inject private DemonSlayerConfig config;
	@Inject private Gson gson;

	private final KillAttribution attribution = new KillAttribution();
	private final Map<NPC, TrackedNpc> tracked = new IdentityHashMap<>();
	private final Map<NPC, TrackedNpc> pendingBosses = new IdentityHashMap<>();
	private long nextToken;
	private MonsterCatalog catalog;
	private BossSync bossSync;
	private MissionSystem missions;
	private MissionRequirements missionRequirements;
	private ProfileStore store;
	private Progression.Profile profile;
	private DemonSlayerPanel panel;
	private DemonSlayerOverlay overlay;
	private DemonSlayerCrowOverlay crowOverlay;
	private BreathingEffectOverlay breathingOverlay;
	private NavigationButton navigation;
	private String syncMessage;
	private String crowMessage;
	private List<String> unresolved = Collections.emptyList();

	@Override
	protected void startUp()
	{
		catalog = MonsterCatalog.load(gson);
		bossSync = new BossSync(catalog);
		missionRequirements = new MissionRequirements(client);
		missions = new MissionSystem(catalog, gson, new Random(), missionRequirements::eligibleLocation);
		store = new ProfileStore(gson, ProfileStore.runelite(configManager));
		panel = new DemonSlayerPanel(() -> clientThread.invoke((Runnable) this::syncBossRecords), config,
			id -> clientThread.invoke(() -> unlockStyle(id)),
			id -> clientThread.invoke(() -> equipStyle(id)),
			() -> clientThread.invoke((Runnable) this::resetProfile),
			() -> DeveloperDebugHooks.create(this));
		overlay = new DemonSlayerOverlay(client, config);
		crowOverlay = new DemonSlayerCrowOverlay(client::getTickCount,
			() -> client.getGameState() == GameState.LOGGED_IN);
		breathingOverlay = new BreathingEffectOverlay(client);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "crow.png");
		navigation = NavigationButton.builder().tooltip("Demon Slayer Corps").icon(icon)
			.priority(8).panel(panel).build();
		clientToolbar.addNavigation(navigation);
		overlayManager.add(overlay);
		overlayManager.add(crowOverlay);
		overlayManager.add(breathingOverlay);
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loadProfile();
		}
		refreshPanel();
		log.debug("Demon Slayer started with {} eligible NPC IDs", catalog.size());
	}

	@Override
	protected void shutDown()
	{
		if (navigation != null)
		{
			clientToolbar.removeNavigation(navigation);
		}
		if (overlay != null)
		{
			overlayManager.remove(overlay);
			overlay.clear();
		}
		if (crowOverlay != null)
		{
			overlayManager.remove(crowOverlay);
			crowOverlay.clear();
		}
		if (breathingOverlay != null)
		{
			overlayManager.remove(breathingOverlay);
			breathingOverlay.clear();
		}
		clearSession();
		log.debug("Demon Slayer stopped");
	}

	@com.google.inject.Provides
	DemonSlayerConfig provideConfig(ConfigManager manager)
	{
		return manager.getConfig(DemonSlayerConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loadProfile();
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			clearSession();
			refreshPanel();
		}
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged event)
	{
		clearSession();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			loadProfile();
		}
		refreshPanel();
	}

	private void loadProfile()
	{
		try
		{
			profile = store.loadActive();
			if (profile != null && missions.repairLegacyLocation(profile.activeMission))
			{
				store.saveActive();
			}
			overlay.setProfile(profile);
			if (profile != null)
			{
				syncBossRecords(true);
			}
		}
		catch (JsonParseException error)
		{
			profile = null;
			crowMessage = "Saved profile could not be read. Data was left untouched.";
			log.warn("Unable to load Demon Slayer profile", error);
		}
		refreshPanel();
	}

	private void clearSession()
	{
		tracked.clear();
		pendingBosses.clear();
		attribution.clear();
		profile = null;
		syncMessage = null;
		crowMessage = null;
		unresolved = Collections.emptyList();
		if (store != null)
		{
			store.clear();
		}
		if (overlay != null)
		{
			overlay.clear();
		}
		if (crowOverlay != null)
		{
			crowOverlay.clear();
		}
		if (breathingOverlay != null)
		{
			breathingOverlay.clear();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		register(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		TrackedNpc current = tracked.get(npc);
		MonsterCatalog.Monster monster = resolve(npc);
		if (monster == null)
		{
			if (current != null)
			{
				attribution.despawn(current.token);
				tracked.remove(npc);
			}
		}
		else if (current == null)
		{
			register(npc);
		}
		else
		{
			current.monster = monster;
			current.npcId = effectiveId(npc);
			current.combatLevel = combatLevel(npc);
			current.size = npcSize(npc);
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		TrackedNpc removed = tracked.remove(event.getNpc());
		if (removed != null)
		{
			attribution.despawn(removed.token);
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (profile == null || !(event.getActor() instanceof NPC) || !event.getHitsplat().isMine()
			|| event.getHitsplat().getAmount() <= 0)
		{
			return;
		}
		TrackedNpc state = register((NPC) event.getActor());
		if (state != null)
		{
			if (state.monster.boss && state.beforeKc < 0)
			{
				state.beforeKc = knownKc(state.monster);
			}
			attribution.localHit(state.token, client.getTickCount());
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (profile == null || !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		TrackedNpc state = tracked.get(npc);
		if (state == null || state.combatLevel <= 0 || !attribution.death(state.token, client.getTickCount()))
		{
			return;
		}
		WorldPoint worldLocation = npc.getWorldLocation();
		int deathRegionId = worldLocation == null ? -1 : worldLocation.getRegionID();
		if (state.monster.boss)
		{
			state.deathLocation = npc.getLocalLocation();
			state.deathPlane = client.getPlane();
			state.deathRegionId = deathRegionId;
			state.deathTick = client.getTickCount();
			pendingBosses.put(npc, state);
			if (state.lootSeen || knownKc(state.monster) > state.beforeKc)
			{
				confirmBoss(npc, state);
			}
		}
		else
		{
			long beforeXp = Progression.xp(profile);
			int beforeLevel = Progression.level(profile);
			long beforeKills = Progression.kills(profile);
			Progression.Record record = Progression.getOrCreate(profile.normalRecords,
				Integer.toString(state.npcId), state.monster);
			Progression.award(profile, record, 1, state.combatLevel);
			MissionSystem.Result mission = missions.liveKill(profile, state.monster, state.npcId,
				deathRegionId,
				state.combatLevel, missionRequirements::eligible);
			breathingOverlay.play(profile.activeBreathingStyle, npc.getLocalLocation(), client.getPlane(),
				state.size, state.height);
			afterAward(beforeXp, beforeLevel, beforeKills, false);
			if (mission.message != null)
			{
				crow(mission.message);
				refreshPanel();
			}
		}
	}

	@Subscribe
	public void onNpcLootReceived(NpcLootReceived event)
	{
		TrackedNpc state = tracked.get(event.getNpc());
		if (state == null)
		{
			state = pendingBosses.get(event.getNpc());
		}
		if (state != null && state.monster.boss)
		{
			state.lootSeen = true;
			if (pendingBosses.containsKey(event.getNpc()))
			{
				confirmBoss(event.getNpc(), state);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if ("demon-slayer-display".equals(event.getGroup()))
		{
			if ("resetProgression".equals(event.getKey()) && "true".equals(event.getNewValue()))
			{
				configManager.setConfiguration("demon-slayer-display", "resetProgression", false);
				javax.swing.SwingUtilities.invokeLater(() -> { if (panel != null) panel.confirmReset(); });
			}

			refreshPanel();
			return;
		}
		if (!"killcount".equals(event.getGroup()) || profile == null
			|| (event.getProfile() != null && !event.getProfile().equals(configManager.getRSProfileKey())))
		{
			return;
		}
		MonsterCatalog.Monster changed = catalog.bossByName(event.getKey());
		if (changed == null)
		{
			return;
		}
		for (Map.Entry<NPC, TrackedNpc> pending : new ArrayList<>(pendingBosses.entrySet()))
		{
			TrackedNpc state = pending.getValue();
			if (BossSync.key(changed).equals(BossSync.key(state.monster))
				&& knownKc(state.monster) > state.beforeKc)
			{
				confirmBoss(pending.getKey(), state);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		int tick = client.getTickCount();
		Iterator<Map.Entry<NPC, TrackedNpc>> iterator = pendingBosses.entrySet().iterator();
		while (iterator.hasNext())
		{
			if (tick - iterator.next().getValue().deathTick > BOSS_EVIDENCE_TICKS)
			{
				iterator.remove();
			}
		}
	}

	private TrackedNpc register(NPC npc)
	{
		TrackedNpc existing = tracked.get(npc);
		if (existing != null)
		{
			return existing;
		}
		MonsterCatalog.Monster monster = resolve(npc);
		if (monster == null)
		{
			return null;
		}
		TrackedNpc state = new TrackedNpc();
		state.token = ++nextToken;
		state.monster = monster;
		state.npcId = effectiveId(npc);
		state.combatLevel = combatLevel(npc);
		state.size = npcSize(npc);
		state.height = Math.max(80, npc.getLogicalHeight());
		tracked.put(npc, state);
		attribution.spawn(state.token);
		return state;
	}

	private MonsterCatalog.Monster resolve(NPC npc)
	{
		int id = effectiveId(npc);
		return id < 0 ? null : catalog.byId(id);
	}

	private static int effectiveId(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		return composition == null ? -1 : composition.getId();
	}

	private static int combatLevel(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		return composition == null ? -1 : composition.getCombatLevel();
	}

	private static int npcSize(NPC npc)
	{
		NPCComposition composition = npc.getTransformedComposition();
		return composition == null ? 1 : composition.getSize();
	}

	private void confirmBoss(NPC npc, TrackedNpc state)
	{
		if (pendingBosses.remove(npc) == null || profile == null)
		{
			return;
		}
		long beforeXp = Progression.xp(profile);
		int beforeLevel = Progression.level(profile);
		long beforeKills = Progression.kills(profile);
		bossSync.liveKill(profile, state.monster, state.beforeKc, knownKc(state.monster), state.combatLevel);
		if (Progression.kills(profile) != beforeKills)
		{
			MissionSystem.Result mission = missions.liveKill(profile, state.monster, state.npcId,
				state.deathRegionId,
				state.combatLevel, missionRequirements::eligible);
			breathingOverlay.play(profile.activeBreathingStyle, state.deathLocation, state.deathPlane, state.size, state.height);
			afterAward(beforeXp, beforeLevel, beforeKills, true);
			if (mission.message != null)
			{
				crow(mission.message);
				refreshPanel();
			}
		}
	}

	private void afterAward(long beforeXp, int beforeLevel, long beforeKills, boolean boss)
	{
		store.saveActive();
		int level = Progression.level(profile);
		long xp = Progression.xp(profile);
		long kills = Progression.kills(profile);
		String message = null;
		if (beforeLevel < 99 && level == 99)
		{
			message = "Hashira rank achieved.";
		}
		else if (Progression.rankIndex(level) > Progression.rankIndex(beforeLevel))
		{
			message = Progression.RANKS[Progression.rankIndex(level)] + " rank achieved.";
		}
		else if (level > beforeLevel)
		{
			message = "Demon Slayer level " + level + ".";
		}
		else if (crossed(beforeXp, xp, Progression.MASTERY_MILESTONES))
		{
			message = "Hashira mastery milestone.";
		}
		else if (boss && crossed(Progression.kills(profile.bossRecords) - 1,
			Progression.kills(profile.bossRecords), Progression.BOSS_MILESTONES))
		{
			message = "Boss extermination milestone.";
		}
		else if (crossed(beforeKills, kills, Progression.TOTAL_MILESTONES))
		{
			message = "Extermination milestone: " + kills + ".";
		}
		if (message != null)
		{
			crow(message);
		}
		refreshPanel();
	}

	private static boolean crossed(long before, long after, long[] thresholds)
	{
		for (long threshold : thresholds)
		{
			if (before < threshold && after >= threshold)
			{
				return true;
			}
		}
		return false;
	}

	private int knownKc(MonsterCatalog.Monster monster)
	{
		String rsProfile = configManager.getRSProfileKey();
		if (rsProfile == null)
		{
			return -1;
		}
		int best = -1;
		for (String key : configManager.getRSProfileConfigurationKeys("killcount", rsProfile, ""))
		{
			MonsterCatalog.Monster matched = catalog.bossByName(key);
			if (matched != null && BossSync.key(matched).equals(BossSync.key(monster)))
			{
				Integer value = configManager.getRSProfileConfiguration("killcount", key, int.class);
				if (value != null)
				{
					best = Math.max(best, value);
				}
			}
		}
		return best;
	}

	private void syncBossRecords()
	{
		syncBossRecords(false);
	}

	private void syncBossRecords(boolean automatic)
	{
		if (profile == null)
		{
			syncMessage = "Log in before synchronizing boss records.";
			refreshPanel();
			return;
		}
		Map<String, Integer> known = knownBossKc();
		BossSync.Result result = automatic ? bossSync.importKnown(profile, known)
			: bossSync.refreshKnown(profile, known);
		profile.lastBossSync = System.currentTimeMillis();
		store.saveActive();
		syncMessage = automatic && result.importedKills == 0 ? null
			: result.importedKills + " imported • +" + result.awardedXp + " XP";
		unresolved = result.unresolved;
		if (!automatic)
		{
			crow("Boss records refreshed.");
		}
		else if (result.importedKills > 0)
		{
			crow(result.importedKills + " boss records imported.");
		}
		refreshPanel();
	}

	private Map<String, Integer> knownBossKc()
	{
		Map<String, Integer> known = new HashMap<>();
		String key = configManager.getRSProfileKey();
		if (key == null)
		{
			return known;
		}
		for (String name : configManager.getRSProfileConfigurationKeys("killcount", key, ""))
		{
			Integer value = configManager.getRSProfileConfiguration("killcount", name, int.class);
			if (value != null && value >= 0)
			{
				known.put(name, value);
			}
		}
		return known;
	}

	private void resetProfile()
	{
		if (profile == null)
		{
			return;
		}
		Progression.Profile fresh = new Progression.Profile();
		fresh.resetBossKc = true;
		for (Map.Entry<String, Integer> entry : knownBossKc().entrySet())
		{
			MonsterCatalog.Monster monster = catalog.bossByName(entry.getKey());
			if (monster != null)
			{
				fresh.bossKcBaselines.merge(BossSync.key(monster), entry.getValue(), Math::max);
			}
		}
		store.resetActive(fresh);
		profile = fresh;
		tracked.clear();
		pendingBosses.clear();
		attribution.clear();
		breathingOverlay.clear();
		syncMessage = null;
		unresolved = Collections.emptyList();
		crow("Demon Slayer progression reset.");
		refreshPanel();
	}

	private void unlockStyle(String id)
	{
		if (profile != null && BreathingProgression.unlock(profile, id))
		{
			store.saveActive();
			BreathingProgression.Style style = BreathingProgression.style(id);
			crow(style.name + " Breathing unlocked.");
			refreshPanel();
		}
	}

	private void equipStyle(String id)
	{
		if (profile != null && BreathingProgression.equip(profile, id))
		{
			store.saveActive();
			refreshPanel();
		}
	}

	void developerApply(Consumer<Progression.Profile> change)
	{
		clientThread.invoke(() ->
		{
			if (profile != null)
			{
				change.accept(profile);
				store.saveActive();
				refreshPanel();
			}
		});
	}

	MissionSystem.Mission developerGenerateMission()
	{
		return missions.generateEligibleMission(missionRequirements::eligible);
	}

	private void crow(String message)
	{
		crowMessage = message;
		crowOverlay.show(message);
		if (config.crowSound())
		{
			client.playSoundEffect(SoundEffectID.UI_BOOP);
		}
	}

	private void refreshPanel()
	{
		if (overlay != null)
		{
			overlay.setProfile(profile);
		}
		if (panel != null)
		{
			String name = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();
			panel.update(profile, name, syncMessage, crowMessage, unresolved);
		}
	}
}
