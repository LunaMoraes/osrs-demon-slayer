package com.demonslayer;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
	private int tab;
	private Progression.Profile profile;
	private String playerName = "Slayer";
	private String syncMessage;
	private String crowMessage;
	private List<String> unresolved = new ArrayList<>();

	DemonSlayerPanel(Runnable onSync, DemonSlayerConfig config)
	{
		this.onSync = onSync;
		this.config = config;
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
		String[] names = {"PROFILE", "RECORDS", "REWARDS"};
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
			showRecords();
		}
		else
		{
			showRewards();
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
		if (crowMessage != null)
		{
			skill.add(Box.createVerticalStrut(8));
			skill.add(label("CROW  •  " + crowMessage, GOLD, 9, Font.PLAIN));
		}
		append(skill);

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
			kills.add(label(syncMessage, MUTED, 9, Font.PLAIN));
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
	}

	private void showRecords()
	{
		JPanel summary = card();
		summary.add(label("BESTIARY  /  EXTERMINATIONS", GOLD, 10, Font.BOLD));
		summary.add(Box.createVerticalStrut(8));
		summary.add(row("Demon", Progression.categoryKills(profile, true)));
		summary.add(row("Undead", Progression.categoryKills(profile, false)));
		summary.add(row("Total unique", Progression.kills(profile)));
		append(summary);
		showRecordSection("DEMONS", true);
		showRecordSection("UNDEAD", false);
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
			section.add(label(record.name, TEXT, 11, Font.BOLD));
			section.add(row("Kills", record.kills));
			section.add(label("LV " + record.level + "  •  " + type(record), MUTED, 9, Font.PLAIN));
			section.add(label(NUMBER.format(record.xp) + " XP earned", GOLD, 9, Font.PLAIN));
			section.add(Box.createVerticalStrut(6));
		}
		append(section);
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
		return record.demon && record.undead ? "Demon / Undead" : record.demon ? "Demon" : "Undead";
	}

	private void showRewards()
	{
		int level = Progression.level(profile);
		long xp = Progression.xp(profile);
		long bosses = Progression.kills(profile.bossRecords);
		JPanel rank = card();
		rank.add(label("EQUIPPED CORPS REGALIA", GOLD, 11, Font.BOLD));
		rank.add(Box.createVerticalStrut(8));
		rank.add(label(Progression.RANKS[Progression.rankIndex(level)].toUpperCase(Locale.ROOT)
			+ "  •  LEVEL " + level, TEXT, 14, Font.BOLD));
		rank.add(Box.createVerticalStrut(7));
		rank.add(reward("Rank frame applied to the sidebar", true));
		rank.add(reward(Progression.RANKS[Progression.rankIndex(level)] + " badge ("
			+ RANK_NUMERALS[Progression.rankIndex(level)] + ") beside the crow", true));
		rank.add(reward("Corps XP counter on Profile", level >= 50
			&& config.counterStyle() != DemonSlayerConfig.CounterStyle.CLASSIC));
		rank.add(reward("Local title: " + (level < 50 ? "locked" : config.localTitle() ? "on" : "off in settings"),
			level >= 50 && config.localTitle()));
		rank.add(reward("Boss crest: tier " + Progression.bossTier(profile) + " / 6",
			Progression.bossTier(profile) > 0));
		rank.add(Box.createVerticalStrut(7));
		int nextRank = Progression.rankIndex(level) + 1;
		if (nextRank < Progression.RANKS.length)
		{
			rank.add(label("NEXT  " + Progression.RANKS[nextRank] + "  •  LEVEL "
				+ Progression.RANK_LEVELS[nextRank], MUTED, 9, Font.BOLD));
		}
		else
		{
			rank.add(label("ALL CORPS RANKS UNLOCKED", GOLD, 9, Font.BOLD));
		}
		append(rank);

		JPanel mastery = card();
		mastery.add(label("HASHIRA MASTERY", GOLD, 11, Font.BOLD));
		mastery.add(Box.createVerticalStrut(8));
		for (long milestone : Progression.MASTERY_MILESTONES)
		{
			mastery.add(reward(NUMBER.format(milestone) + " XP  •  prestige border", xp >= milestone));
		}
		for (long milestone : Progression.BOSS_MILESTONES)
		{
			mastery.add(reward(NUMBER.format(milestone) + " bosses  •  crest", bosses >= milestone));
		}
		append(mastery);
	}

	private static JLabel reward(String text, boolean unlocked)
	{
		return label((unlocked ? "◆  " : "◇  ") + text, unlocked ? TEXT : MUTED, 10, Font.PLAIN);
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
		JLabel result = new JLabel(text);
		result.setForeground(color);
		result.setFont(new Font("Dialog", style, size));
		result.setAlignmentX(LEFT_ALIGNMENT);
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
			new EmptyBorder(10, 12, 10, 12)));
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
