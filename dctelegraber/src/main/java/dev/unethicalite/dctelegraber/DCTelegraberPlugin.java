package dev.unethicalite.dctelegraber;

import com.google.inject.Provides;
import dev.unethicalite.api.entities.Players;
import dev.unethicalite.api.entities.TileItems;
import dev.unethicalite.api.events.*;
import dev.unethicalite.api.game.Game;
import dev.unethicalite.api.game.Worlds;
import dev.unethicalite.api.items.Inventory;
import dev.unethicalite.api.magic.Magic;
import dev.unethicalite.api.magic.Regular;
import dev.unethicalite.api.movement.Movement;
import dev.unethicalite.api.movement.Reachable;
import dev.unethicalite.api.plugins.LoopedPlugin;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

@PluginDescriptor(
		name = "DC Telegraber",
		description = "Telegrab, bank repeat",
		enabledByDefault = false
)
@Slf4j
@Extension
public class DCTelegraberPlugin extends LoopedPlugin
{
	private ScheduledExecutorService executor;
	
	@Inject
	private DCTelegraberConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ClientThread clientThread;

	@Inject
	private DCTelegraberPanel dcTelegraberPanel;

	@Inject
	OverlayManager overlayManager;

	public Projectile teleGrabProjectile;

	@Override
	public void startUp() throws Exception
	{
		super.startUp();
		overlayManager.add(dcTelegraberPanel);
	}

	@Provides
	public DCTelegraberConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DCTelegraberConfig.class);
	}

	@Override
	public void shutDown() throws Exception
	{
		super.shutDown();
		if (executor != null)
		{
			executor.shutdown();
		}
		overlayManager.remove(dcTelegraberPanel);


	}

	@Override
	protected int loop()
	{
		Player local = Players.getLocal();
		List<String> itemsToLoot = List.of(config.loot().split(","));
		if (!Inventory.isFull())
		{
			TileItem loot = TileItems.getNearest(x ->
					x.getTile().getWorldLocation().distanceTo(local.getWorldLocation()) < config.lootRange()
							&& ((x.getName() != null && itemsToLoot.contains(x.getName())
							|| (config.lootValue() > -1 && itemManager.getItemPrice(x.getId()) * x.getQuantity() > config.lootValue())
							|| (config.untradables() && (!x.isTradable()) || x.hasInventoryAction("Destroy"))))
			);
			if (loot != null)
			{
				if (Regular.TELEKINETIC_GRAB.canCast())
				{
					log.info("telegrabbing " + loot.getName());
					Magic.cast(Regular.TELEKINETIC_GRAB, loot);
					return -2;
				}
				else if (!Reachable.isInteractable(loot.getTile()))
				{
					Movement.walkTo(loot.getTile().getWorldLocation());
					return -4;
				}
				loot.pickup();
				return -3;


			} else if (Regular.TELEKINETIC_GRAB.canCast() && config.noLoot())
			{
				clientThread.invoke(() -> hop());
				return -1;
			}
		}
		return -1;
	}

	private void hop() {

		if (Game.getState() != GameState.LOGGED_IN) {
			return;
		}

		Worlds.loadWorlds();
		HashSet<World> attempted = new HashSet<World>();
		attempted.add(Worlds.getCurrentWorld());

		EnumSet<WorldType> currentWorldTypes = Game.getClient().getWorldType();
		currentWorldTypes.remove(WorldType.PVP);
		currentWorldTypes.remove(WorldType.BOUNTY);
		currentWorldTypes.remove(WorldType.HIGH_RISK);
		currentWorldTypes.remove(WorldType.SKILL_TOTAL);
		currentWorldTypes.remove(WorldType.LAST_MAN_STANDING);

		int totalLevel = Game.getClient().getTotalLevel();

		World world = null;
		try {
			do {

				world = Worlds.getRandom((w) -> !attempted.contains(w));
				attempted.add(world);

				EnumSet<WorldType> types = world.getTypes().clone();
				types.remove(WorldType.BOUNTY);
				types.remove(WorldType.LAST_MAN_STANDING);

				if (types.contains(WorldType.SKILL_TOTAL)) {
					try {
						int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));
						if (totalLevel >= totalRequirement) {
							types.remove(WorldType.SKILL_TOTAL);
						}
					} catch (NumberFormatException ex) {
						log.warn("Failed to parse total level requirement", ex);
					}
				}
				if (world.getPlayerCount() > 1950) {
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

			log.info(chatMessage);
		}
		else
		{
			String chatMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Hopping to " + world.getId())
					.build();

			log.info(chatMessage);

			final World rsWorld = Game.getClient().createWorld();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayerCount());
			rsWorld.setLocation(world.getLocation());
			rsWorld.setTypes(world.getTypes());

			if (!Worlds.isHopperOpen())
			{
				Worlds.openHopper();
				log.info("Opening hopper");

			}
			Worlds.hopTo(rsWorld);

		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved projectileMoved)
	{
		final Projectile projectile = projectileMoved.getProjectile();
		final int endCycle = projectile.getEndCycle();
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{

	}

	@Subscribe
	public void onItemObtained(ItemObtained itemObtained)
	{

	}

}


