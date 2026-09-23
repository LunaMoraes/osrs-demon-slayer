package com.demonslayer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.Test;

public class DemonSlayerPanelLayoutTest
{
	@Test
	public void tabsFitStandardSidebarAtKinotoAndHashira() throws Exception
	{
		DemonSlayerPanel[] panels = new DemonSlayerPanel[1];
		Progression.Profile profile = new Progression.Profile();
		Progression.Record record = new Progression.Record("Vorkath", false, true, 392);
		record.kills = 5_509;
		record.xp = 2_159_528;
		profile.bossRecords.put("vorkath", record);
		Progression.Record longName = new Progression.Record("Tormented demon (elite variant)", true, false, 450);
		longName.kills = 1;
		profile.normalRecords.put("123", longName);
		SwingUtilities.invokeAndWait(() ->
		{
			panels[0] = new DemonSlayerPanel(() -> { }, new DemonSlayerConfig() { });
			panels[0].setSize(225, 620);
			panels[0].update(profile, "Lara The Ant", "0 historical kills; 0 XP. 5 names unresolved.",
				"Boss records synchronized.", java.util.Arrays.asList("gauntlet", "guardians of the rift",
					"lunar chest", "mimic", "royal titans"));
		});
		SwingUtilities.invokeAndWait(() ->
		{
			DemonSlayerPanel panel = panels[0];
			panel.doLayout();
			JScrollPane scroll = (JScrollPane) panel.getComponent(1);
			scroll.doLayout();
			assertFits(scroll, "Profile");
			JPanel top = (JPanel) panel.getComponent(0);
			JPanel tabs = (JPanel) top.getComponent(1);
			((JButton) tabs.getComponent(1)).doClick();
			scroll.doLayout();
			assertFits(scroll, "Records");
			((JButton) tabs.getComponent(2)).doClick();
			scroll.doLayout();
			assertFits(scroll, "Rewards");
		});
		record.xp = 200_000_000;
		record.kills = 10_000;
		SwingUtilities.invokeAndWait(() -> panels[0].update(profile, "Lara The Ant", null,
			"Hashira mastery milestone.", java.util.Collections.emptyList()));
		SwingUtilities.invokeAndWait(() ->
		{
			DemonSlayerPanel panel = panels[0];
			JPanel tabs = (JPanel) ((JPanel) panel.getComponent(0)).getComponent(1);
			((JButton) tabs.getComponent(0)).doClick();
			JScrollPane scroll = (JScrollPane) panel.getComponent(1);
			scroll.doLayout();
			assertFits(scroll, "Hashira Profile");
			((JButton) tabs.getComponent(2)).doClick();
			scroll.doLayout();
			assertFits(scroll, "Hashira Rewards");
		});
	}

	private static void assertFits(JScrollPane scroll, String tab)
	{
		int contentWidth = scroll.getViewport().getView().getPreferredSize().width;
		int viewportWidth = scroll.getViewport().getExtentSize().width;
		assertTrue(tab + " content width " + contentWidth + " > viewport " + viewportWidth,
			contentWidth <= viewportWidth);
		assertFalse(tab + " horizontal scrollbar", scroll.getHorizontalScrollBar().isVisible());
		assertFalse(tab + " vertical scrollbar", scroll.getVerticalScrollBar().isVisible());
	}
}
