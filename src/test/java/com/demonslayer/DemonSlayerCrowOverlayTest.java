package com.demonslayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.client.ui.overlay.OverlayPosition;
import org.junit.Test;

public class DemonSlayerCrowOverlayTest
{
	@Test
	public void refreshNoticeRendersAwayFromMinimapAndExpires()
	{
		AtomicInteger tick = new AtomicInteger(100);
		AtomicBoolean loggedIn = new AtomicBoolean(true);
		DemonSlayerCrowOverlay overlay = new DemonSlayerCrowOverlay(tick::get, loggedIn::get);
		BufferedImage frame = new BufferedImage(234, 58, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = frame.createGraphics();
		try
		{
			assertEquals(OverlayPosition.TOP_CENTER, overlay.getPosition());
			assertNull(overlay.render(graphics));
			overlay.show("Boss records refreshed.");
			assertNotNull(overlay.render(graphics));
			assertTrue((frame.getRGB(15, 15) >>> 24) != 0);
			loggedIn.set(false);
			assertNull(overlay.render(graphics));
			loggedIn.set(true);
			tick.set(112);
			assertNotNull(overlay.render(graphics));
			tick.set(113);
			assertNull(overlay.render(graphics));
		}
		finally
		{
			graphics.dispose();
		}
	}
}
