package dev.unethicalite.dctelegraber;

import com.google.inject.Provides;
import dev.unethicalite.api.entities.NPCs;
import dev.unethicalite.api.entities.Players;
import dev.unethicalite.api.entities.TileItems;
import dev.unethicalite.api.events.*;
import dev.unethicalite.api.game.Combat;
import dev.unethicalite.api.game.Game;
import dev.unethicalite.api.game.Worlds;
import dev.unethicalite.api.items.Inventory;
import dev.unethicalite.api.magic.Magic;
import dev.unethicalite.api.magic.Regular;
import dev.unethicalite.api.movement.Movement;
import dev.unethicalite.api.movement.Reachable;
import dev.unethicalite.api.movement.pathfinder.BankLocation;
import dev.unethicalite.api.plugins.LoopedPlugin;
import dev.unethicalite.api.widgets.Dialog;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import javax.inject.Inject;
import java.util.Arrays;
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
public class DCTelegraberPlugin extends Plugin
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

	public Projectile teleGrabProjectile = null;
	public ItemObtained lootedItem;

	@Override
	public void startUp() throws Exception
	{
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
		overlayManager.remove(dcTelegraberPanel);
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{


		Player local = Players.getLocal();
		List<String> itemsToLoot = List.of(config.loot().split(","));

		if (local.getInteracting() != null && !Dialog.canContinue())
		{
			return;
		}


		if (config.eat() && Combat.getHealthPercent() <= config.healthPercent())
		{
			Item food = Inventory.getFirst(x -> (x.getName() != null && x.hasAction("Eat")));
			if (food != null)
			{
				food.interact("Eat");
				return;
			}
		}


		Item alchItem = Inventory.getFirst(x -> x.getName() != null && itemsToLoot.contains(x.getName()));
		if (alchItem != null && !local.isAnimating() && Regular.HIGH_LEVEL_ALCHEMY.canCast())
		{

			log.info("Casting high alchemy");
			Magic.cast(Regular.HIGH_LEVEL_ALCHEMY, alchItem);
			return ;
		}

		if (!Inventory.isFull())
		{
			TileItem loot = TileItems.getNearest(x ->
					x.getTile().getWorldLocation().distanceTo(local.getWorldLocation()) < config.lootRange()
							&& ((x.getName() != null && itemsToLoot.contains(x.getName())
							|| (config.lootValue() > -1 && itemManager.getItemPrice(x.getId()) * x.getQuantity() > config.lootValue())
							|| (config.untradables() && (!x.isTradable()) || x.hasInventoryAction("Destroy"))))
			);
//			Player nearbyPlayer = Players.getNearest((p) -> !p.equals(local));
//			Player nearbyPlayer = Game.getClient().getPlayers().stream().filter(p -> !p.equals(local)).findFirst().orElse(null);
//			log.info("nearby {}", nearbyPlayer.getName());

			if (loot != null)
			{
				log.info("anim {} {}, idle {}, loot {} ", local.getAnimation(), local.isAnimating(), local.isIdle(), (loot.getName()));

				if (Regular.TELEKINETIC_GRAB.canCast())
				{
//					log.info("items >> {}" ,itemsToLoot.toString());
					grab(loot);
//					Magic.cast(Regular.TELEKINETIC_GRAB, loot);
					return;
				}
				else if (!Reachable.isInteractable(loot.getTile()))
				{
					Movement.walkTo(loot.getTile().getWorldLocation());
					return;
				}
				loot.pickup();
				return;


			}
			else if (Regular.TELEKINETIC_GRAB.canCast() && config.noLoot())
			{

				if (Worlds.isHopperOpen())
				{
					clientThread.invoke(this::hop);
				}
				else
				{
					Worlds.loadWorlds();
				}

			}
		}
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

		Player caster = (Player) projectile.getInteracting().getInteracting();
		Player target = (Player) projectile.getInteracting();

		if (caster != null && !caster.equals(Game.getClient().getLocalPlayer()))
		{
			log.info("{} launched {}", caster.getName(), projectile.getId());
		}
		if (target != null && target.equals(Game.getClient().getLocalPlayer()))
		{
			log.info("You are being attacked");
		}
		log.info("projectile >> {}", projectile.getId());
		if (projectile.getId() == 143)
		{
			log.info("telegrab sent");
			teleGrabProjectile = projectile;
		}

	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{

		}

	}

	@Subscribe
	public void onItemObtained(ItemObtained itemObtained)
	{
		lootedItem = itemObtained;
		teleGrabProjectile = null;
	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		TileItem item = itemSpawned.getItem();
		log.info("{} spawned",item.getName());

		if (!item.getName().contains("Iron mace"))
		{
			return;
		}
	}


	private void grab(TileItem loot)
	{
		try
		{
			Magic.cast(Regular.TELEKINETIC_GRAB, loot);
//			log.info("casted telegrab on {} [id:{}]", loot.getName(), loot.getId());

		}
		catch (Exception ex)
		{
			log.error("error casting", ex.getMessage());
		}
		//		lootedItem = null;
	}





}


