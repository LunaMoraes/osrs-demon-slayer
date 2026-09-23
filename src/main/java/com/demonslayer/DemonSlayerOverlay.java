package com.demonslayer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class DemonSlayerOverlay extends Overlay
{
	private static final Color GOLD = new Color(214, 174, 100);
	private static final int OVERHEAD_NAME_OFFSET = 40;
	private static final int TITLE_NAME_GAP = 5;

	private final Client client;
	private final DemonSlayerConfig config;
	private String cachedTitle;

	DemonSlayerOverlay(Client client, DemonSlayerConfig config)
	{
		this.client = client;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void setProfile(Progression.Profile profile)
	{
		int level = profile == null ? 0 : Progression.level(profile);
		cachedTitle = level < 50 ? null : level == 99 ? "柱  HASHIRA"
			: level >= 90 ? "◆  KINOE  ◆"
			: Progression.RANKS[Progression.rankIndex(level)].toUpperCase();
	}

	void clear()
	{
		cachedTitle = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}
		if (cachedTitle != null && config.localTitle())
		{
			drawTitle(graphics);
		}
		return null;
	}

	private void drawTitle(Graphics2D graphics)
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		String title = cachedTitle;
		graphics.setFont(new Font("Dialog", Font.BOLD, 12));
		Point position = player.getCanvasTextLocation(graphics, title,
			player.getLogicalHeight() + OVERHEAD_NAME_OFFSET);
		if (position == null)
		{
			return;
		}
		// Player Indicators uses this same anchor for overhead names. Draw one text line below it.
		int y = position.getY() + graphics.getFontMetrics().getHeight() + TITLE_NAME_GAP;
		graphics.setColor(Color.BLACK);
		graphics.drawString(title, position.getX() + 1, y + 1);
		graphics.setColor(GOLD);
		graphics.drawString(title, position.getX(), y);
	}

}
