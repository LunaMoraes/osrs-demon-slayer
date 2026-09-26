package com.demonslayer;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.text.DateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.api.Experience;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

final class DemonSlayerPanel extends PluginPanel
{
	private static final Color INK = new Color(23, 20, 22);
	private static final Color PARCHMENT = new Color(37, 31, 32);
	private static final Color RAISED = new Color(46, 37, 38);
	private static final Color CRIMSON = new Color(152, 42, 54);
	private static final Color GOLD = new Color(214, 174, 100);
	private static final Color TEXT = new Color(238, 226, 208);
	private static final Color MUTED = new Color(171, 154, 143);
	private static final BufferedImage CROW = ImageUtil.loadImageResource(DemonSlayerPanel.class, "crow.png");
	private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance(Locale.US);
	private static final String[] RANK_NUMERALS = {"I", "II", "III", "IV", "V", "VI", "VII",
		"VIII", "IX", "X", "XI"};

	private final JPanel body = new JPanel();
	private final JPanel tabs = new JPanel(new GridLayout(1, 3, 3, 0));
	private final Header header = new Header();
	private final Runnable onSync;
	private final DemonSlayerConfig config;
	private final Consumer<String> onUnlock;
	private final Consumer<String> onEquip;
	private final Runnable onReset;
	private final Supplier<JPanel> developerSection;
	private int tab;
	private Progression.Profile profile;
	private String playerName = "Slayer";
	private String syncMessage;
	private String crowMessage;
	private List<String> unresolved = new ArrayList<>();

	DemonSlayerPanel(Runnable onSync, DemonSlayerConfig config,
		Consumer<String> onUnlock, Consumer<String> onEquip, Runnable onReset,
		Supplier<JPanel> developerSection)
	{
		this.onSync = onSync;
		this.config = config;
		this.onUnlock = onUnlock;
		this.onEquip = onEquip;
		this.onReset = onReset;
		this.developerSection = developerSection;
		setLayout(new BorderLayout());
		setBackground(INK);
		JPanel top = new JPanel(new BorderLayout());
		top.setBackground(INK);
		top.add(header, BorderLayout.NORTH);
		top.add(tabs, BorderLayout.SOUTH);
		add(top, BorderLayout.NORTH);
		body.setBackground(INK);
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		JScrollPane scroll = new JScrollPane(body);
		scroll.setBorder(null);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getViewport().setBackground(INK);
		scroll.getVerticalScrollBar().setUnitIncrement(12);
		add(scroll, BorderLayout.CENTER);
		rebuild();
	}

	void update(Progression.Profile value, String name, String sync, String crow, List<String> unresolvedNames)
	{
		Progression.Profile display = Progression.snapshot(value);
		List<String> unresolvedCopy = new ArrayList<>(unresolvedNames);
		SwingUtilities.invokeLater(() ->
		{
			profile = display;
			playerName = name == null || name.trim().isEmpty() ? "Slayer" : name;
			syncMessage = sync;
			crowMessage = crow;
			unresolved = unresolvedCopy;
			rebuild();
		});
	}

	private void rebuild()
	{
		header.repaint();
		tabs.removeAll();
		String[] names = {"PROFILE", "REWARDS", "RECORD"};
		for (int i = 0; i < names.length; i++)
		{
			final int index = i;
			JButton button = button(names[i], i == tab);
			button.addActionListener(event ->
			{
				tab = index;
				rebuild();
			});
			tabs.add(button);
		}
		body.removeAll();
		body.add(Box.createVerticalStrut(8));
		if (profile == null)
		{
			JPanel waiting = card();
			waiting.add(label("AWAITING RUNESCAPE PROFILE", GOLD, 11, Font.BOLD));
			waiting.add(label("Log in to begin your Corps record.", MUTED, 11, Font.PLAIN));
			if (crowMessage != null)
			{
				waiting.add(label("Saved data could not be read.", CRIMSON, 10, Font.BOLD));
				waiting.add(label("Your data was not overwritten.", MUTED, 10, Font.PLAIN));
			}
			append(waiting);
		}
		else if (tab == 0)
		{
			showProfile();
		}
		else if (tab == 1)
		{
			showBreathing();
		}
		else
		{
			showRecords();
		}
		body.add(Box.createVerticalGlue());
		body.revalidate();
		body.repaint();
		tabs.revalidate();
		tabs.repaint();
	}

	private void showProfile()
	{
		int level = Progression.level(profile);
		long xp = Progression.xp(profile);
		JPanel skill = card();
		skill.add(label("DEMON SLAYER  /  " + playerName, GOLD, 10, Font.BOLD));
		skill.add(Box.createVerticalStrut(10));
		skill.add(label(Progression.RANKS[Progression.rankIndex(level)].toUpperCase(Locale.ROOT), TEXT, 18, Font.BOLD));
		skill.add(label("LEVEL " + level, MUTED, 12, Font.BOLD));
		skill.add(Box.createVerticalStrut(10));
		if (level < 99)
		{
			int start = Experience.getXpForLevel(level);
			int end = Experience.getXpForLevel(level + 1);
			boolean corpsCounter = level >= 50 && config.counterStyle() != DemonSlayerConfig.CounterStyle.CLASSIC;
			if (corpsCounter)
			{
				skill.add(label("CORPS XP  •  " + NUMBER.format(xp), GOLD, 12, Font.BOLD));
				skill.add(Box.createVerticalStrut(5));
			}
			skill.add(new ProgressBar((double) (xp - start) / (end - start), accent()));
			skill.add(Box.createVerticalStrut(5));
			skill.add(label(NUMBER.format(xp) + " / " + NUMBER.format(end) + " XP", TEXT, 11, Font.PLAIN));
			skill.add(label(NUMBER.format(end - xp) + " XP to next level", MUTED, 10, Font.PLAIN));
		}
		else
		{
			skill.add(new ProgressBar(1, accent()));
			skill.add(label("HASHIRA MASTERY", GOLD, 10, Font.BOLD));
			skill.add(label(NUMBER.format(Progression.mastery(profile)) + " XP", TEXT, 11, Font.BOLD));
			skill.add(label("Total XP  " + NUMBER.format(xp), MUTED, 10, Font.PLAIN));
		}
		BreathingProgression.Style active = BreathingProgression.style(profile.activeBreathingStyle);
		skill.add(Box.createVerticalStrut(7));
		skill.add(label(active == null ? "No breathing style equipped" : active.name + " Breathing",
			active == null ? MUTED : BreathingEffectOverlay.color(active.id), 11, Font.BOLD));
		if (crowMessage != null)
		{
			skill.add(Box.createVerticalStrut(8));
			skill.add(label("CROW  •  " + crowMessage, GOLD, 9, Font.PLAIN));
		}
		append(skill);
		showMissions();

		JPanel debug = developerSection.get();
		if (debug != null)
		{
			append(debug);
		}
	}

	private void showMissions()
	{
		JPanel mission = card();
		mission.add(label("KASUGAI CROW MISSIONS", GOLD, 11, Font.BOLD));
		MissionSystem.Mission active = profile.activeMission;
		if (active == null)
		{
			mission.add(label("No active mission", TEXT, 11, Font.PLAIN));
			mission.add(label(NUMBER.format(Math.min(profile.missionXpBank, MissionSystem.XP_THRESHOLD))
				+ " / " + MissionSystem.XP_THRESHOLD + " raw live XP to assignment", MUTED, 10, Font.PLAIN));
		}
		else
		{
			mission.add(label("Eliminate " + active.required + " " + active.target, TEXT, 11, Font.BOLD));
			if (active.location != null)
			{
				mission.add(label("Location: " + active.location, GOLD, 10, Font.BOLD));
			}
			mission.add(label(active.progress + " / " + active.required + " confirmed kills", MUTED, 10, Font.PLAIN));
			mission.add(label("Banked for next mission: " + NUMBER.format(profile.missionXpBank) + " XP",
				MUTED, 10, Font.PLAIN));
		}
		mission.add(row("Missions completed", profile.missionsCompleted));
		mission.add(label("Next Breathing Point chance: "
			+ MissionSystem.nextChance(profile.breathingDryStreak) + "%", GOLD, 10, Font.BOLD));
		mission.add(label("Dry streak: " + profile.breathingDryStreak, MUTED, 10, Font.PLAIN));
		mission.add(Box.createVerticalStrut(7));
		mission.add(row("Breathing points", profile.breathingPointsAvailable));
		append(mission);
	}

	private void showBreathing()
	{
		JPanel card = card();
		card.add(label("BREATHING TREE", GOLD, 11, Font.BOLD));
		card.add(label("Points: " + profile.breathingPointsAvailable, TEXT, 11, Font.PLAIN));
		card.add(label("Start at an outer style.", MUTED, 9, Font.PLAIN));
		card.add(label("Follow its lineage inward.", MUTED, 9, Font.PLAIN));
		JButton open = button("OPEN BREATHING TREE", false);
		open.addActionListener(event -> openTree());
		card.add(open);
		append(card);
		showRewards();
	}

	void confirmReset()
	{
		int choice = JOptionPane.showConfirmDialog(this,
			"Erase Demon Slayer XP, kills, missions, and Breathing progress for this RuneScape profile?\n"
				+ "Your OSRS account and RuneLite boss KC will not change.",
			"Reset Demon Slayer progression", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		if (choice != JOptionPane.OK_OPTION)
		{
			return;
		}
		String typed = JOptionPane.showInputDialog(this, "Type RESET to confirm:",
			"Final confirmation", JOptionPane.WARNING_MESSAGE);
		if ("RESET".equals(typed))
		{
			onReset.run();
		}
	}

	private void openTree()
	{
		Window owner = SwingUtilities.getWindowAncestor(this);
		JDialog dialog = new JDialog(owner, "Breathing Tree", java.awt.Dialog.ModalityType.MODELESS);
		JPanel frame = new JPanel(new BorderLayout());
		frame.setBackground(INK);
		frame.add(new TreeCanvas(profile, id -> {onUnlock.accept(id); dialog.dispose();},
			id -> {onEquip.accept(id); dialog.dispose();}), BorderLayout.CENTER);
		JButton close = button("CLOSE", false);
		close.addActionListener(event -> dialog.dispose());
		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottom.setBackground(INK);
		bottom.add(close);
		frame.add(bottom, BorderLayout.SOUTH);
		dialog.setContentPane(frame);
		dialog.setSize(860, 720);
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}

	private static final class TreeCanvas extends JPanel
	{
		private final Map<String, Point> nodes = new HashMap<>();
		private final Progression.Profile value;

		TreeCanvas(Progression.Profile profile, Consumer<String> unlock, Consumer<String> equip)
		{
			value = profile;
			setLayout(null);
			setBackground(INK);
			String[] ids = {"sun", "water", "flame", "wind", "thunder", "stone", "flower",
				"serpent", "love", "mist", "beast", "sound", "insect"};
			int[][] positions = {{420,325},{270,250},{540,235},{535,415},{300,440},{420,505},
				{185,155},{125,290},{690,165},{710,375},{680,510},{175,535},{105,80}};
			for (int i = 0; i < ids.length; i++)
			{
				String id = ids[i];
				Point point = new Point(positions[i][0], positions[i][1]);
				nodes.put(id, point);
				BreathingProgression.Style style = BreathingProgression.style(id);
				boolean owned = profile.unlockedBreathingStyles.contains(id);
				boolean available = BreathingProgression.available(profile, id);
				boolean active = id.equals(profile.activeBreathingStyle);
				Color tint = BreathingEffectOverlay.color(id);
				JButton node = new JButton()
				{
					@Override protected void paintComponent(Graphics graphics)
					{
						Graphics2D g = (Graphics2D) graphics.create();
						g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						int cx = getWidth() / 2;
						g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), active ? 65 : 22));
						g.fillOval(cx - 31, 1, 62, 62);
						g.setColor(active || owned ? tint : available ? GOLD : new Color(85,77,80));
						g.setStroke(new BasicStroke(active ? 2.5f : 1.2f));
						g.drawOval(cx - 26, 6, 52, 52);
						g.translate(cx, 32);
						BreathingEffectOverlay.draw(g, id, 16, .35f);
						g.translate(-cx, -32);
						g.setColor(active || owned ? TEXT : available ? GOLD : MUTED);
						g.setFont(new Font("Serif", Font.BOLD, 14));
						g.drawString(style.name, cx - g.getFontMetrics().stringWidth(style.name) / 2, 78);
						String status = active ? "EQUIPPED" : owned ? "UNLOCKED" : available ? "1 POINT" : "LOCKED";
						g.setFont(new Font("Dialog", Font.PLAIN, 9));
						g.setColor(active ? tint : MUTED);
						g.drawString(status, cx - g.getFontMetrics().stringWidth(status) / 2, 92);
						g.dispose();
					}
				};
				node.setBounds(point.x - 55, point.y - 32, 110, 98);
				node.setContentAreaFilled(false);
				node.setBorderPainted(false);
				node.setFocusPainted(false);
				node.setToolTipText(owned ? "Equip " + style.name : available ? "Unlock for one Breathing Point"
					: "sun".equals(id) ? "Unlock Water, Flame, Wind, Thunder and Stone first" : "Unlock a connected outer style first");
				node.setEnabled(owned || available);
				node.addActionListener(event -> { if (owned) equip.accept(id); else unlock.accept(id); });
				add(node);
			}
		}

		@Override protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setPaint(new java.awt.RadialGradientPaint(420, 325, 360,
				new float[]{0, 1}, new Color[]{new Color(55,35,36), INK}));
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(GOLD);
			g.setFont(new Font("Serif", Font.BOLD, 21));
			g.drawString("BREATHING LINEAGES", 285, 38);
			g.setFont(new Font("Dialog", Font.PLAIN, 11));
			g.setColor(MUTED);
			g.drawString(value.breathingPointsAvailable + " points available  /  Follow a branch toward Sun", 278, 59);
			for (BreathingProgression.Style style : BreathingProgression.styles())
			{
				if (style.parent == null) continue;
				Point a = nodes.get(style.id), b = nodes.get(style.parent);
				double distance = a.distance(b);
				double ux = (b.x-a.x)/distance, uy = (b.y-a.y)/distance;
				boolean owned = value.unlockedBreathingStyles.contains(style.id);
				g.setColor(owned ? BreathingEffectOverlay.color(style.id).darker() : new Color(66,53,56));
				g.setStroke(new BasicStroke(owned ? 2f : 1f));
				g.draw(new java.awt.geom.Line2D.Double(a.x+ux*39,a.y+uy*39,b.x-ux*39,b.y-uy*39));
			}
			g.setColor(MUTED);
			g.drawString("Outer styles begin each lineage. Sun requires all five principal styles.", 215, 606);
			g.dispose();
		}
	}

	private void showRecords()
	{
		JPanel kills = card();
		kills.add(label("EXTERMINATIONS", GOLD, 11, Font.BOLD));
		kills.add(Box.createVerticalStrut(8));
		kills.add(row("Normal", Progression.kills(profile.normalRecords)));
		kills.add(row("Boss", Progression.kills(profile.bossRecords)));
		kills.add(Box.createVerticalStrut(5));
		kills.add(row("TOTAL", Progression.kills(profile)));
		kills.add(Box.createVerticalStrut(6));
		kills.add(label("Progress saves automatically", MUTED, 9, Font.PLAIN));
		kills.add(Box.createVerticalStrut(7));
		JButton sync = button("REFRESH RUNELITE KC", false);
		sync.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));
		sync.addActionListener(event -> onSync.run());
		kills.add(sync);
		if (syncMessage != null)
		{
			kills.add(Box.createVerticalStrut(5));
			JLabel summary = label(syncMessage.length() > 27 ? syncMessage.substring(0, 26) + "…"
				: syncMessage, MUTED, 9, Font.PLAIN);
			summary.setToolTipText(syncMessage);
			kills.add(summary);
		}
		if (!unresolved.isEmpty())
		{
			JButton details = button(unresolved.size() + " UNRESOLVED KC NAMES", false);
			details.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
			details.addActionListener(event -> JOptionPane.showMessageDialog(this,
				String.join("\n", unresolved), "Unresolved RuneLite KC names", JOptionPane.INFORMATION_MESSAGE));
			kills.add(Box.createVerticalStrut(5));
			kills.add(details);
		}
		if (profile.lastBossSync > 0)
		{
			kills.add(Box.createVerticalStrut(5));
			kills.add(label("Last sync  " + DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT).format(new Date(profile.lastBossSync)), MUTED, 9, Font.PLAIN));
		}
		append(kills);

		JPanel summary = card();
		summary.add(label("BESTIARY  /  EXTERMINATIONS", GOLD, 10, Font.BOLD));
		summary.add(Box.createVerticalStrut(8));
		summary.add(row("Demon", Progression.categoryKills(profile, true)));
		summary.add(row("Undead", Progression.categoryKills(profile, false)));
		summary.add(row("Vampire", Progression.vampireKills(profile)));
		summary.add(row("Total kills", Progression.kills(profile)));
		append(summary);
		showRecordSection("DEMONS", true);
		showRecordSection("UNDEAD", false);
		showVampireRecords();
	}

	private void showVampireRecords()
	{
		JPanel section = card();
		section.add(label("VAMPIRES", GOLD, 11, Font.BOLD));
		List<Progression.Record> records = new ArrayList<>();
		for (Progression.Record record : profile.normalRecords.values())
		{
			if (record.vampire) records.add(record);
		}
		for (Progression.Record record : profile.bossRecords.values())
		{
			if (record.vampire) records.add(record);
		}
		for (Progression.Record record : records)
		{
			recordEntry(section, record);
		}
		if (records.isEmpty()) section.add(label("No exterminations recorded yet.", MUTED, 10, Font.PLAIN));
		append(section);
	}

	private void showRecordSection(String title, boolean demon)
	{
		JPanel section = card();
		section.add(label(title, GOLD, 11, Font.BOLD));
		section.add(Box.createVerticalStrut(8));
		List<Progression.Record> records = new ArrayList<>();
		addMatchingRecords(records, profile.normalRecords, demon);
		addMatchingRecords(records, profile.bossRecords, demon);
		records.sort(Comparator.comparingLong((Progression.Record record) -> record.xp).reversed());
		if (records.isEmpty())
		{
			section.add(label("No exterminations recorded yet.", MUTED, 10, Font.PLAIN));
		}
		for (Progression.Record record : records)
		{
			recordEntry(section, record);
		}
		append(section);
	}

	private void recordEntry(JPanel section, Progression.Record record)
	{
		Color tint = record.vampire ? new Color(205, 120, 166) : record.demon ? new Color(231, 133, 91) : new Color(132, 186, 204);
		JPanel entry = new JPanel();
		entry.setLayout(new BoxLayout(entry, BoxLayout.Y_AXIS));
		entry.setBackground(INK);
		entry.setAlignmentX(LEFT_ALIGNMENT);
		entry.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 2, 0, 0, tint), new EmptyBorder(8, 5, 8, 5)));
		JLabel name = label(record.name, tint, 11, Font.BOLD);
		if (record.name.length() > 27) name.setText("<html><div style=" + '"' + "width:112px" + '"' + ">" + record.name.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
		entry.add(name);
		entry.add(label("LV " + record.level + "  •  " + type(record), MUTED, 9, Font.PLAIN));
		entry.add(Box.createVerticalStrut(5));
		entry.add(row("Kills", record.kills));
		entry.add(label(NUMBER.format(record.xp) + " XP earned", TEXT, 10, Font.PLAIN));
		section.add(entry);
		section.add(Box.createVerticalStrut(8));
	}

	private static void addMatchingRecords(List<Progression.Record> result,
		Map<String, Progression.Record> source, boolean demon)
	{
		for (Progression.Record record : source.values())
		{
			if (demon ? record.demon : record.undead)
			{
				result.add(record);
			}
		}
	}

	private static String type(Progression.Record record)
	{
		return record.vampire ? "Vampire" : record.demon && record.undead ? "Demon / Undead"
			: record.demon ? "Demon" : "Undead";
	}

	private void showRewards()
	{
		int level = Progression.level(profile);
		int rank = Progression.rankIndex(level);
		rewardCard(RANK_NUMERALS[rank], "CORPS BADGE", Progression.RANKS[rank],
			"Your badge and profile frame", GOLD);
		if (rank + 1 < Progression.RANKS.length)
		{
			JPanel next = card();
			next.add(label("NEXT RANK", MUTED, 10, Font.BOLD));
			next.add(label(Progression.RANKS[rank + 1], TEXT, 14, Font.BOLD));
			next.add(label("Unlocks at level " + Progression.RANK_LEVELS[rank + 1], GOLD, 10, Font.PLAIN));
			append(next);
		}
		rewardCard("T", "CHARACTER TITLE", level < 50 ? "Unlocks at level 50"
			: config.localTitle() ? "Enabled" : "Disabled in settings", "Visible beside your character", GOLD);
		int tier = Progression.bossTier(profile);
		rewardCard("◆", "BOSS CREST", "Tier " + tier + " / 6", "Earned through boss exterminations", CRIMSON.brighter());
		milestones("CREST PROGRESS", Progression.kills(profile.bossRecords), Progression.BOSS_MILESTONES, "kills");
		if (level >= 99)
		{
			rewardCard("✦", "HASHIRA MASTERY", "Border tier " + Progression.masteryTier(profile) + " / 4",
				"Prestige earned beyond level 99", GOLD);
			milestones("NEXT PRESTIGE", Progression.xp(profile), Progression.MASTERY_MILESTONES, "XP");
		}
	}

	private void milestones(String title, long current, long[] thresholds, String unit)
	{
		JPanel panel = card();
		panel.add(label(title, GOLD, 10, Font.BOLD));
		long previous = 0;
		for (long threshold : thresholds)
		{
			if (current < threshold)
			{
				panel.add(Box.createVerticalStrut(8));
				panel.add(new ProgressBar((double) (current - previous) / (threshold - previous), GOLD));
				panel.add(Box.createVerticalStrut(6));
				panel.add(label(NUMBER.format(current) + " / " + NUMBER.format(threshold) + " " + unit, TEXT, 11, Font.PLAIN));
				panel.add(label(NUMBER.format(threshold - current) + " " + unit + " remaining", MUTED, 10, Font.PLAIN));
				append(panel);
				return;
			}
			previous = threshold;
		}
		panel.add(label("All milestones unlocked", TEXT, 11, Font.BOLD));
		append(panel);
	}

	private void rewardCard(String glyph, String title, String value, String description, Color tint)
	{
		JPanel panel = card();
		JPanel heading = new JPanel(new BorderLayout(8, 0));
		heading.setOpaque(false);
		heading.setAlignmentX(LEFT_ALIGNMENT);
		JPanel emblem = new JPanel()
		{
			@Override protected void paintComponent(Graphics graphics)
			{
				Graphics2D g = (Graphics2D) graphics.create();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), 25));
				g.fillOval(1, 1, 38, 38);
				g.setColor(tint);
				g.drawOval(1, 1, 38, 38);
				g.drawOval(5, 5, 30, 30);
				g.setFont(new Font("Serif", Font.BOLD, 15));
				g.drawString(glyph, 20 - g.getFontMetrics().stringWidth(glyph) / 2, 26);
				g.dispose();
			}
		};
		emblem.setOpaque(false);
		emblem.setPreferredSize(new Dimension(41, 41));
		heading.add(emblem, BorderLayout.WEST);
		heading.add(label(title.replace(" ", "<br>"), tint, 10, Font.BOLD), BorderLayout.CENTER);
		// Explicit HTML keeps the compact emblem heading within the sidebar.
		((JLabel) heading.getComponent(1)).setText("<html>" + title.replace(" ", "<br>") + "</html>");
		panel.add(heading);
		panel.add(Box.createVerticalStrut(9));
		panel.add(label(value, TEXT, 13, Font.BOLD));
		panel.add(Box.createVerticalStrut(4));
		panel.add(label(description, MUTED, 10, Font.PLAIN));
		append(panel);
	}

	private static JPanel row(String name, long number)
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 19));
		row.add(label(name, TEXT, 11, Font.PLAIN), BorderLayout.WEST);
		row.add(label(NUMBER.format(number), GOLD, 11, Font.BOLD), BorderLayout.EAST);
		return row;
	}

	private static JLabel label(String text, Color color, int size, int style)
	{
		String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		JLabel result = new JLabel(text.length() > 27
			? "<html><div style=" + '"' + "width:125px" + '"' + ">" + escaped + "</div></html>" : text);
		result.setForeground(color);
		result.setFont(new Font("Dialog", style, size));
		result.setAlignmentX(LEFT_ALIGNMENT);
		result.setMinimumSize(new Dimension(0, result.getPreferredSize().height));
		return result;
	}

	private JPanel card()
	{
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
		result.setBackground(PARCHMENT);
		int tier = masteryTier();
		result.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(tier == 0 ? accent().darker() : GOLD,
				tier == 0 ? 1 : tier + 1),
			new EmptyBorder(10, 12 - tier, 10, 12 - tier)));
		result.setAlignmentX(LEFT_ALIGNMENT);
		return result;
	}

	private void append(JPanel card)
	{
		card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
		body.add(card);
		body.add(Box.createVerticalStrut(7));
	}

	private JButton button(String text, boolean selected)
	{
		JButton result = new JButton(text);
		result.setAlignmentX(LEFT_ALIGNMENT);
		result.setFont(new Font("Dialog", Font.BOLD, 10));
		result.setForeground(selected ? accent() : TEXT);
		result.setBackground(selected ? RAISED : accent().darker());
		result.setFocusPainted(false);
		result.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(selected ? accent() : new Color(88, 42, 48)),
			new EmptyBorder(7, 4, 7, 4)));
		return result;
	}

	private Color accent()
	{
		if (profile == null)
		{
			return CRIMSON;
		}
		int rank = Progression.rankIndex(Progression.level(profile));
		if (rank == 10)
		{
			return GOLD;
		}
		if (rank >= 8)
		{
			return new Color(194 + (rank - 8) * 9, 148 + (rank - 8) * 12,
				80 + (rank - 8) * 8);
		}
		return new Color(Math.min(202, 145 + rank * 5), 42 + rank * 6, 54 + rank * 2);
	}

	private int masteryTier()
	{
		return profile == null ? 0 : Progression.masteryTier(profile);
	}

	private final class Header extends JPanel
	{
		Header()
		{
			setPreferredSize(new Dimension(225, 155));
			setBackground(INK);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int width = getWidth();
			Color accent = accent();
			int rank = profile == null ? 0 : Progression.rankIndex(Progression.level(profile));
			g.setColor(accent);
			g.fillRect(0, 0, width, 4);
			g.setStroke(new BasicStroke(rank == 10 ? 2f : 1f));
			g.drawRoundRect(8, 9, width - 17, 115, 13, 13);
			if (rank >= 5)
			{
				g.drawRoundRect(12, 13, width - 25, 107, 11, 11);
			}
			if (profile != null)
			{
				g.setColor(GOLD);
				for (int i = 0; i < Progression.bossTier(profile); i++)
				{
					g.fillOval(17 + i * 9, 18, 5, 5);
				}
				if (rank == 10)
				{
					for (int i = 0; i < masteryTier(); i++)
					{
						g.fillOval(width - 22 - i * 9, 18, 5, 5);
					}
				}
			}
			g.setColor(new Color(67, 43, 44));
			g.drawLine(12, 131, width - 12, 131);
			g.drawImage(CROW, width / 2 - 64, 24, 44, 44, null);
			if (profile != null)
			{
				int badgeX = width / 2 + 24;
				g.setColor(RAISED);
				g.fillRoundRect(badgeX, 22, 58, 48, 9, 9);
				g.setColor(accent);
				g.setStroke(new BasicStroke(2f));
				g.drawRoundRect(badgeX, 22, 58, 48, 9, 9);
				g.setFont(new Font("Serif", Font.BOLD, 19));
				centerAt(g, RANK_NUMERALS[rank], badgeX + 29, 44);
				g.setColor(TEXT);
				g.setFont(new Font("Dialog", Font.BOLD, 8));
				centerAt(g, Progression.RANKS[rank].toUpperCase(Locale.ROOT), badgeX + 29, 59);
			}
			g.setColor(GOLD);
			g.setFont(new Font("Serif", Font.BOLD, 15));
			center(g, "DEMON SLAYER", width, 87);
			g.setColor(TEXT);
			g.setFont(new Font("Dialog", Font.BOLD, 10));
			center(g, "C O R P S", width, 103);
			if (rank >= 9)
			{
				g.setColor(accent);
				center(g, rank == 10 ? "◆  HASHIRA EMBLEM  ◆" : "◆  KINOE CREST  ◆", width, 117);
			}
			else if (profile != null && Progression.bossTier(profile) > 0)
			{
				g.setColor(GOLD);
				center(g, "◆  BOSS CREST  " + Progression.bossTier(profile) + "  ◆", width, 117);
			}
			g.setColor(MUTED);
			g.setFont(new Font("Dialog", Font.PLAIN, 10));
			center(g, profile == null ? "Awaiting profile"
				: Progression.RANKS[Progression.rankIndex(Progression.level(profile))] + "  •  Level "
				+ Progression.level(profile), width, 149);
			g.dispose();
		}

		private void center(Graphics2D g, String text, int width, int y)
		{
			g.drawString(text, (width - g.getFontMetrics().stringWidth(text)) / 2, y);
		}

		private void centerAt(Graphics2D g, String text, int x, int y)
		{
			g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);
		}
	}

	private static final class ProgressBar extends JPanel
	{
		private final double fraction;
		private final Color accent;

		ProgressBar(double fraction, Color accent)
		{
			this.fraction = Math.max(0, Math.min(1, fraction));
			this.accent = accent;
			setPreferredSize(new Dimension(170, 14));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 14));
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setColor(INK);
			g.fillRoundRect(0, 1, getWidth(), 11, 6, 6);
			g.setColor(accent);
			g.fillRoundRect(1, 2, (int) ((getWidth() - 2) * fraction), 9, 5, 5);
			g.setColor(GOLD);
			g.drawRoundRect(0, 1, getWidth() - 1, 11, 6, 6);
			g.dispose();
		}
	}
}

