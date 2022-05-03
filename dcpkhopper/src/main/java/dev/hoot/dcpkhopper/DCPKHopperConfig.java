package dev.hoot.dcpkhopper;

import net.runelite.client.config.*;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@ConfigGroup("dcpkhopper")
public interface DCPKHopperConfig extends Config
{
//	@ConfigSection(
//			name = "Section 1",
//			description = "placeholder",
//			closedByDefault = true,
//			position = 0
//	)
//	String section1 = "section1";


	@ConfigItem(
			keyName = "hopKey",
			name = "Quick-hop",
			description = "When you press this key you'll hop to a random world",
			position = 0
	)
	default Keybind hopKey()
	{
		return new Keybind(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "teleKey",
			name = "Quick-teleport",
			description = "When you press this key you'll teleport if available",
			position = 1
	)
	default Keybind teleKey()
	{
		return new Keybind(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
			keyName = "maxPlayerCount",
			name = "Max world size",
			description = "Max size of world to hop to",
			position = 2
	)
	default int maxPlayerCount() { return 1950; }

}

