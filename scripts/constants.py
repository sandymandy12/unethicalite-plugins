
# must start with plugin base name and end with base name
INNER_PATH = "src/main/java/dev/hoot"
GRADLE_SETTINGS_PATH = "../settings.gradle.kts"


PLUGIN_GRADLE_TEMPLATE = """\
version = "0.0.1"

project.extra["PluginName"] = "FULLNAME"
project.extra["PluginDescription"] = "DESCRIPTION"

tasks {
    jar {
        manifest {
            attributes(mapOf(
                    "Plugin-Version" to project.version,
                    "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                    "Plugin-Provider" to project.extra["PluginProvider"],
                    "Plugin-Description" to project.extra["PluginDescription"],
                    "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}
"""

CONFIG_TEMPLATE = """\
package dev.hoot.LOWERCASED;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("LOWERCASED")
public interface CAPITALIZEDConfig extends Config
{
	@ConfigSection(
			name = "",
			description = "",
			closedByDefault = true,
			position = 0
	)
	String SECTION = "";


	@ConfigSection(
			name = "",
			description = "",
			closedByDefault = true,
			position = 1
	)
	String SECTION = "";

	@ConfigItem(
			keyName = "",
			name = "",
			description = "",
			section = ,
			position = 0
	)
	default String ITEM()
	{
		return "";
	}
}

    """

PLUGIN_TEMPLATE = """\
package dev.hoot.LOWERCASED;

import com.google.inject.Inject;
import com.google.inject.Provides;
import dev.hoot.api.EntityNameable;
import dev.hoot.api.Interactable;
import dev.hoot.api.entities.NPCs;
import dev.hoot.api.entities.Players;
import dev.hoot.api.entities.TileItems;
import dev.hoot.api.entities.TileObjects;
import dev.hoot.api.game.Game;
import dev.hoot.api.items.Inventory;
import dev.hoot.api.widgets.Widgets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.TileObject;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.pf4j.Extension;

@PluginDescriptor(
		name = "FULLNAME",
		description = "DESCRIPTION",
		enabledByDefault = false
)
@Slf4j
@Extension
public class CAPITALIZEDPlugin extends Plugin
{
	@Inject
	private CAPITALIZEDConfig config;

	@Inject
	private Client client;


	@Provides
	public CAPITALIZEDConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CAPITALIZEDConfig.class);
	}

}

"""
