package com.demonslayer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

/** Brief Corps notices in RuneLite's managed top-center overlay area. */
final class DemonSlayerCrowOverlay extends Overlay
{
	private static final int WIDTH = 234;
	private static final int HEIGHT = 58;
	private static final int VISIBLE_TICKS = 12;
	private static final Color INK = new Color(23, 20, 22, 232);
	private static final Color GOLD = new Color(214, 174, 100);
	private static final Color TEXT = new Color(238, 226, 208);
	private static final BufferedImage CROW = ImageUtil.loadImageResource(DemonSlayerCrowOverlay.class,
		"crow.png");

	private final IntSupplier tick;
	private final BooleanSupplier loggedIn;
	private String message;
	private int expiresAtTick;

	DemonSlayerCrowOverlay(IntSupplier tick, BooleanSupplier loggedIn)
	{
		this.tick = tick;
		this.loggedIn = loggedIn;
		setPosition(OverlayPosition.TOP_CENTER);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_LOW);
		setMovable(true);
		setSnappable(true);
	}

	void show(String text)
	{
		message = text;
		expiresAtTick = tick.getAsInt() + VISIBLE_TICKS;
	}

	void clear()
	{
		message = null;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!loggedIn.getAsBoolean() || message == null || tick.getAsInt() > expiresAtTick)
		{
			return null;
		}
		Graphics2D g = (Graphics2D) graphics.create();
		g.setColor(INK);
		g.fillRoundRect(0, 0, WIDTH, HEIGHT, 10, 10);
		g.setColor(GOLD);
		g.drawRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, 10, 10);
		g.drawImage(CROW, 8, 7, 43, 43, null);
		g.setFont(new Font("Dialog", Font.BOLD, 10));
		g.drawString("KASUGAI CROW  •  CORPS", 58, 21);
		g.setFont(new Font("Dialog", Font.PLAIN, 11));
		g.setColor(TEXT);
		g.drawString(fit(message, g.getFontMetrics(), WIDTH - 67), 58, 42);
		g.dispose();
		return new Dimension(WIDTH, HEIGHT);
	}

	private static String fit(String text, FontMetrics metrics, int maxWidth)
	{
		if (metrics.stringWidth(text) <= maxWidth)
		{
			return text;
		}
		String ellipsis = "…";
		int end = text.length();
		while (end > 0 && metrics.stringWidth(text.substring(0, end) + ellipsis) > maxWidth)
		{
			end--;
		}
		return text.substring(0, end) + ellipsis;
	}
}
