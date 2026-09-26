package com.demonslayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.AlphaComposite;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Short, local, post-confirmation effects with no combat information. */
final class BreathingEffectOverlay extends Overlay
{
	private static final long DURATION_MS = 1_500;
	private static final int MAX_EFFECTS = 12;
	private static final class Effect
	{
		String style;
		LocalPoint location;
		int plane;
		int size;
		long started;
	}

	private final Client client;
	private final List<Effect> effects = new ArrayList<>();

	BreathingEffectOverlay(Client client)
	{
		this.client = client;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	void play(String style, LocalPoint location, int plane, int size)
	{
		if (style == null || location == null)
		{
			return;
		}
		if (effects.size() == MAX_EFFECTS)
		{
			effects.remove(0);
		}
		Effect effect = new Effect();
		effect.style = style;
		effect.location = location;
		effect.plane = plane;
		effect.size = Math.max(1, Math.min(size, 5));
		effect.started = System.currentTimeMillis();
		effects.add(effect);
	}

	void clear()
	{
		effects.clear();
	}

	@Override public Dimension render(Graphics2D graphics)
	{
		long now = System.currentTimeMillis();
		Iterator<Effect> iterator = effects.iterator();
		while (iterator.hasNext())
		{
			Effect effect = iterator.next();
			float progress = (float) (now - effect.started) / DURATION_MS;
			if (progress >= 1)
			{
				iterator.remove();
				continue;
			}
			Point point = Perspective.localToCanvas(client, effect.location, effect.plane);
			if (point == null)
			{
				continue;
			}
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.translate(point.getX(), point.getY() - 24 - effect.size * 7);
			int radius = 28 + effect.size * 12 + (int) (Math.sin(progress * Math.PI / 2) * 40);
			Color color = color(effect.style);
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
				Math.max(0, Math.min(255, (int) (210 * (1 - progress))))));
			g.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, (1 - progress) * 1.6f)));
			g.setStroke(new BasicStroke(2.5f + effect.size / 2f));
			draw(g, effect.style, radius, progress);
			g.dispose();
		}
		return null;
	}

	static Color color(String style)
	{
		switch (style)
		{
			case "water": return new Color(70, 170, 230);
			case "flame": return new Color(255, 107, 49);
			case "wind": return new Color(104, 212, 155);
			case "thunder": return new Color(250, 220, 78);
			case "stone": return new Color(166, 152, 123);
			case "flower": return new Color(245, 136, 187);
			case "insect": return new Color(164, 123, 220);
			case "serpent": return new Color(106, 188, 113);
			case "love": return new Color(252, 102, 163);
			case "mist": return new Color(180, 198, 211);
			case "beast": return new Color(144, 196, 220);
			case "sound": return new Color(240, 190, 97);
			default: return new Color(255, 185, 73);
		}
	}

	static void draw(Graphics2D g, String style, int radius, float progress)
	{
		Color tint = color(style);
		AffineTransform origin = g.getTransform();
		// Filled, tapered trails retain a readable silhouette at different zoom levels.
		if ("thunder".equals(style))
		{
			for (int i = 0; i < 3; i++)
			{
				g.setTransform(origin);
				g.rotate(i * 2.1 + progress * .4);
				Path2D bolt = new Path2D.Double();
				bolt.moveTo(-radius, radius * .45);
				bolt.lineTo(-radius * .15, -radius * .25);
				bolt.lineTo(-radius * .3, radius * .1);
				bolt.lineTo(radius, -radius * .5);
				glow(g, bolt, tint, radius * .08f);
			}
		}
		else if ("stone".equals(style))
		{
			for (int i = 0; i < 8; i++)
			{
				g.setTransform(origin);
				g.rotate(i * Math.PI / 4 + .2);
				g.translate(radius * (.25 + progress * .6), 0);
				Path2D shard = new Path2D.Double();
				shard.moveTo(0,-radius*.12); shard.lineTo(radius*.3,0);
				shard.lineTo(radius*.08,radius*.12); shard.lineTo(-radius*.06,0); shard.closePath();
				g.setColor(tint); g.fill(shard);
				g.setColor(tint.brighter()); g.draw(shard);
			}
		}
		else if ("flower".equals(style) || "insect".equals(style) || "love".equals(style))
		{
			for (int i = 0; i < 10; i++)
			{
				g.setTransform(origin);
				g.rotate(i * Math.PI / 5 + progress * 1.8);
				g.translate(radius * (.25 + progress * .65), 0);
				g.rotate(i * .7 + progress * 2);
				Path2D petal = new Path2D.Double();
				petal.moveTo(0,0);
				petal.curveTo(-radius*.3,-radius*.28,radius*.3,-radius*.38,radius*.18,0);
				petal.curveTo(radius*.1,radius*.13,0,radius*.2,0,0);
				g.setColor(i % 2 == 0 ? tint : tint.brighter()); g.fill(petal);
				if ("insect".equals(style))
				{
					g.scale(-1,1); g.fill(petal);
				}
			}
		}
		else
		{
			int count = "beast".equals(style) ? 4 : "mist".equals(style) ? 5 : 3;
			for (int i = 0; i < count; i++)
			{
				g.setTransform(origin);
				g.rotate("beast".equals(style) ? -.65 : i * Math.PI * 2 / count + progress * 2.5);
				if ("beast".equals(style)) g.translate((i-1.5)*radius*.3,0);
				Path2D trail = new Path2D.Double();
				trail.moveTo(-radius, radius * .25);
				trail.curveTo(-radius*.15,-radius*1.15,radius*1.2,-radius*.7,radius*.9,radius*.35);
				trail.curveTo(radius*.6,-radius*.4,-radius*.1,-radius*.65,-radius,radius*.25);
				g.setColor(new Color(tint.getRed(),tint.getGreen(),tint.getBlue(),160));
				g.fill(trail);
				g.setStroke(new BasicStroke(Math.max(1,radius*.045f),BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				g.setColor(tint.brighter()); g.draw(trail);
				g.scale(.8,.8); g.setColor(new Color(255,250,235,190)); g.draw(trail);
			}
		}
		// Embers, spray and sparks travel away from the finishing stroke.
		for (int i = 0; i < 16; i++)
		{
			g.setTransform(origin);
			double angle = i * 2.39996;
			double spread = radius * (.35 + progress * .85) * (.5 + (i % 4) * .2);
			double x = Math.cos(angle) * spread, y = Math.sin(angle) * spread;
			if ("flame".equals(style) || "sun".equals(style)) y -= progress * radius * .7;
			g.translate(x,y); g.rotate(angle);
			g.setColor(i % 3 == 0 ? new Color(255,248,221,220) : tint);
			int size = Math.max(2, radius / 15);
			g.fillOval(-size/2,-size/2,size*2,size);
		}
		g.setTransform(origin);
	}

	private static void glow(Graphics2D g, Path2D path, Color tint, float width)
	{
		g.setColor(new Color(tint.getRed(),tint.getGreen(),tint.getBlue(),70));
		g.setStroke(new BasicStroke(width*3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		g.draw(path);
		g.setColor(tint);
		g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		g.draw(path);
		g.setColor(new Color(255,253,229));
		g.setStroke(new BasicStroke(Math.max(1,width*.3f)));
		g.draw(path);
	}
}
