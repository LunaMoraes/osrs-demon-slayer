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
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
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
	private ProfileStore store;
	private Progression.Profile profile;
	private DemonSlayerPanel panel;
	private DemonSlayerOverlay overlay;
	private DemonSlayerCrowOverlay crowOverlay;
	private NavigationButton navigation;
	private String syncMessage;
	private String crowMessage;
	private List<String> unresolved = Collections.emptyList();

	@Override
	protected void startUp()
	{
		catalog = MonsterCatalog.load(gson);
		bossSync = new BossSync(catalog);
		store = new ProfileStore(gson, ProfileStore.runelite(configManager));
		panel = new DemonSlayerPanel(() -> clientThread.invoke((Runnable) this::syncBossRecords), config);
		overlay = new DemonSlayerOverlay(client, config);
		crowOverlay = new DemonSlayerCrowOverlay(client::getTickCount,
			() -> client.getGameState() == GameState.LOGGED_IN);
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "crow.png");
		navigation = NavigationButton.builder().tooltip("Demon Slayer Corps").icon(icon)
			.priority(8).panel(panel).build();
		clientToolbar.addNavigation(navigation);
		overlayManager.add(overlay);
		overlayManager.add(crowOverlay);
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
		if (state.monster.boss)
		{
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
			afterAward(beforeXp, beforeLevel, beforeKills, false);
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
			afterAward(beforeXp, beforeLevel, beforeKills, true);
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
		Map<String, Integer> known = new HashMap<>();
		for (String key : configManager.getRSProfileConfigurationKeys("killcount",
			configManager.getRSProfileKey(), ""))
		{
			Integer value = configManager.getRSProfileConfiguration("killcount", key, int.class);
			if (value != null && value >= 0)
			{
				known.put(key, value);
			}
		}
		BossSync.Result result = bossSync.importKnown(profile, known);
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
