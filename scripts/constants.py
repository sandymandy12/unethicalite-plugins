# must start with plugin base name and end with base name
INNER_PATH = "src/main/java/dev/unethicalite"
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

CLASSIC_TEMPLATE = """\
package dev.unethicalite.LOWERCASED;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
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

LOOPED_TEMPLATE = """\
package dev.unethicalite.LOWERCASED;

import com.google.inject.Provides;
import dev.unethicalite.api.plugins.LoopedPlugin;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;

import java.util.concurrent.Executors;

@PluginDescriptor(
		name = "FULLNAME",
		description = "DESCRIPTION",
		enabledByDefault = false
)
@Slf4j
@Extension
public class UPPERCASEDPlugin extends LoopedPlugin
{
	
	@Inject
	private UPPERCASEDConfig config;

	@Inject
	private ItemManager itemManager;
	
	@Override
	public void startUp() throws Exception
	{
		super.startUp();
		
	}

	@Provides
	public HootFighterConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(UPPERCASEDConfig.class);
	}

	@Override
	public void shutDown() throws Exception
	{
		super.shutDown();
		if (executor != null)
		{
			executor.shutdown();
		}
	}

	@Override
	protected int loop()
	{

		return -3;
	}
}


"""