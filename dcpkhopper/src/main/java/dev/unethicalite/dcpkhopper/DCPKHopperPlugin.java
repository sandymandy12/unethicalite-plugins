package dev.unethicalite.dcpkhopper;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.HotkeyListener;
import org.pf4j.Extension;

import dev.unethicalite.api.game.*;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

@PluginDescriptor(
		name = "DC PK Hopper",
		description = "Hop worlds or teleport when attackable players show up",
		enabledByDefault = false
)
@Slf4j
@Extension
public class DCPKHopperPlugin extends Plugin
{
	@Inject
	private DCPKHopperConfig config;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private KeyManager keyManager;

	private final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;
	private int displaySwitcherAttempts = 0;
	private World quickHopTargetWorld ;

	private final HotkeyListener hopKeyListener = new HotkeyListener(() -> config.hopKey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> hop(true));
		}
	};

	private final HotkeyListener teleKeyListener = new HotkeyListener(() -> config.teleKey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> teleport());
		}
	};

	@Provides
	public DCPKHopperConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DCPKHopperConfig.class);
	}

	@Override
	protected void startUp() throws Exception {
		keyManager.registerKeyListener(hopKeyListener);
		keyManager.registerKeyListener(teleKeyListener);
	}

	@Override
	protected void shutDown() throws Exception {
		keyManager.unregisterKeyListener(hopKeyListener);
		keyManager.unregisterKeyListener(teleKeyListener);
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		if (quickHopTargetWorld == null)
		{
			return;
		}


		if (!Game.getClient().isWorldSelectOpen())
		{

			Worlds.openHopper();

			if (++displaySwitcherAttempts >= DISPLAY_SWITCHER_MAX_ATTEMPTS)
			{
				String chatMessage = new ChatMessageBuilder()
						.append(ChatColorType.NORMAL)
						.append("Failed to quick-hop after ")
						.append(ChatColorType.HIGHLIGHT)
						.append(Integer.toString(displaySwitcherAttempts))
						.append(ChatColorType.NORMAL)
						.append(" attempts.")
						.build();

				chatMessageManager
						.queue(QueuedMessage.builder()
								.type(ChatMessageType.CONSOLE)
								.runeLiteFormattedMessage(chatMessage)
								.build());

				resetQuickHopper();
			}
			else
			{
//				Worlds.hopTo(quickHopTargetWorld);
				log.info("tick - target world: {}", quickHopTargetWorld.getId());
				resetQuickHopper();
			}
		}

	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{
			resetQuickHopper();
		}
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned playerSpawned)
	{
		Player player =	playerSpawned.getPlayer();

	}

	private void hop(boolean random) {

		if (Game.getState() != GameState.LOGGED_IN) {
			return;
		}

		HashSet<World> attempted = new HashSet<World>();
		attempted.add(Worlds.getCurrentWorld());

		EnumSet<WorldType> currentWorldTypes = Game.getClient().getWorldType();
		currentWorldTypes.remove(WorldType.PVP);
		currentWorldTypes.remove(WorldType.BOUNTY);
		currentWorldTypes.remove(WorldType.HIGH_RISK);
		currentWorldTypes.remove(WorldType.SKILL_TOTAL);
		currentWorldTypes.remove(WorldType.LAST_MAN_STANDING);

		List<World> worlds = Worlds.getAll((w) -> !(w.equals(Worlds.getCurrentWorld())));

		int totalLevel = Game.getClient().getTotalLevel();

		World world = null;
		try {
			do {
				world = Worlds.getRandom((w) -> !attempted.contains(w));
				log.info("World {} {}. Attempt: {}", world.getId(), world.getTypes().toString(), attempted.size());

				attempted.add(world);

				EnumSet<WorldType> types = world.getTypes().clone();
				types.remove(WorldType.BOUNTY);
				types.remove(WorldType.LAST_MAN_STANDING);

				if (types.contains(WorldType.SKILL_TOTAL)) {
					try {
						int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));
						if (totalLevel >= totalRequirement) {
							types.remove(WorldType.SKILL_TOTAL);
						} else {
							log.info("Did not meet level requirements, skipping " + world.getId());
						}
					} catch (NumberFormatException ex) {
						log.warn("Failed to parse total level requirement", ex);
					}
				}
				if (world.getPlayerCount() > config.maxPlayerCount()) {
					log.info("World is too large, skipping " + world.getId());
					continue;
				}

				if (currentWorldTypes.equals(types)) {
					break;
				}

				world = null;
			}
			while (world == null || !(Game.getState() == GameState.LOGGED_IN));
		} catch (Exception e) { log.error("Error looping worlds", e); }

		if (world == null)
		{
			log.info("Couldn't find a world after {} attempts", attempted.size());
			String chatMessage = new ChatMessageBuilder()
					.append(ChatColorType.HIGHLIGHT)
					.append("Couldn't find a world to quick hop to. attempts " + attempted.size())
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(chatMessage)
					.build());
		}
		else
		{
			log.info("Hopping to " + world.getId());
			String chatMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Hopping to " + world.getId())
					.build();

			chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(chatMessage)
					.build());

			final World rsWorld = Game.getClient().createWorld();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayerCount());
			rsWorld.setLocation(world.getLocation());
			rsWorld.setTypes(world.getTypes());

			quickHopTargetWorld = rsWorld;
			displaySwitcherAttempts = 0;

			Worlds.openHopper();
			Worlds.hopTo(rsWorld);
			log.info("hop - Game state " + Game.getState().toString());

		}
	}

	private void resetQuickHopper()
	{
		displaySwitcherAttempts = 0;
		quickHopTargetWorld = null;
	}
	private void teleport()
	{

	}
}

