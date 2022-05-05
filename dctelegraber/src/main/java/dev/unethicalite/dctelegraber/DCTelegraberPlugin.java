package dev.unethicalite.dctelegraber;

import com.google.inject.Provides;
import dev.unethicalite.api.entities.NPCs;
import dev.unethicalite.api.entities.Players;
import dev.unethicalite.api.entities.TileItems;
import dev.unethicalite.api.entities.TileObjects;
import dev.unethicalite.api.events.*;
import dev.unethicalite.api.game.Combat;
import dev.unethicalite.api.game.Game;
import dev.unethicalite.api.game.Worlds;
import dev.unethicalite.api.items.Bank;
import dev.unethicalite.api.items.Inventory;
import dev.unethicalite.api.magic.Magic;
import dev.unethicalite.api.magic.Regular;
import dev.unethicalite.api.movement.Movement;
import dev.unethicalite.api.movement.Reachable;
import dev.unethicalite.api.movement.pathfinder.BankLocation;
import dev.unethicalite.api.plugins.LoopedPlugin;
import dev.unethicalite.api.widgets.Dialog;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
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
import java.lang.reflect.InvocationTargetException;
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

	public Projectile teleGrabProjectile = null;
	public ItemObtained lootedItem;
	public Player pker;
	public World quickHopWorld;
	
	final private WorldPoint NATURE_RUNE_WORLD_POINT = new WorldPoint(3302, 3855, 0);


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
		reset();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState gameState = gameStateChanged.getGameState();
		if (gameState == GameState.HOPPING)
		{
			reset();

		}
		if (gameState == GameState.LOGGED_IN)
		{
			Worlds.loadWorlds();
		}
		log.info("Gamestate changed {}", gameState);
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned playerSpawned)
	{

		if (withinRange(playerSpawned.getPlayer()))
		{
			pker = playerSpawned.getPlayer();
			Game.getClient().playSoundEffect(SoundEffectID.TOWN_CRIER_BELL_DING);
		}
	}

	@Override
	protected int loop()
	{

		Player local = Players.getLocal();

//		log.info("Widgets {}",Game.getClient().getWidgets().length);

		if (local.getInteracting() != null && !Dialog.canContinue())
		{
			return -1;
		}

		if (config.eat() && Combat.getHealthPercent() <= config.healthPercent())
		{
			Item food = Inventory.getFirst(x -> (x.getName() != null && x.hasAction("Eat")));
			if (food != null)
			{
				food.interact("Eat");
				return -3;
			}
		}

		if (Movement.getRunEnergy() < 100 && Game.getWildyLevel() == 0)
		{
			Item potion = Inventory.getFirst(x -> (x.getName() != null && x.getName().contains("Energy")));
			if (potion != null)
			{
				potion.interact("Drink");
				return -1;
			}
		}

		if (Movement.getRunEnergy() < 50 && Movement.isRunEnabled())
		{
			Item potion = Inventory.getFirst(x -> (x.getName() != null && x.getName().contains("Energy")));
			if (potion != null)
			{
				potion.interact("Drink");
				return -1;
			}
		}

		Player nearbyPlayer = Players.getNearest((p) -> !p.equals(local));
		if (pker != null || (nearbyPlayer != null && Game.getWildyLevel() != 0))
		{
			log.info("nearby {}", nearbyPlayer.getName());
			if (!Movement.isRunEnabled())
			{
				Movement.toggleRun();
			}
			return move();
		}

		if (Movement.isWalking())
		{
			return -4;
		}

		List<String> itemsToLoot = List.of(config.loot().split(","));
		if (!Inventory.isFull())
		{
			TileItem loot = TileItems.getNearest(x ->
					x.getTile().getWorldLocation().distanceTo(local.getWorldLocation()) < config.lootRange()
							&& ((x.getName() != null && itemsToLoot.contains(x.getName())
							|| (config.lootValue() > -1 && itemManager.getItemPrice(x.getId()) * x.getQuantity() > config.lootValue())))
			);

			if (loot != null)
			{
				if (Inventory.getCount(true, ItemID.LAW_RUNE) > 2)
				{
					grab(loot);
					return -3;
				}
				else
				{
					return move();
				}
			}
			else if (Inventory.getCount(true, ItemID.LAW_RUNE) > 2
					&& local.distanceTo(NATURE_RUNE_WORLD_POINT) > 0) // we should go to the spot
			{
				Movement.walkTo(NATURE_RUNE_WORLD_POINT);
				return -4;
			}
			else if (Inventory.getCount(true, ItemID.LAW_RUNE) <= 2) // used the last law rune, time to leave
			{
				return move();
			}
			else if (Inventory.getCount(true, ItemID.NATURE_RUNE) > 0) // still good, time to hop
			{
				// open the hopper if it isn't already
				// in case there is a problem (onchatmessage) will null quickhopworld
				if (Worlds.isHopperOpen() && quickHopWorld == null)
				{
					clientThread.invoke(this::hop);
					return -2;
				}
				else
				{
					// open to world switcher and try again.
					log.info("HOPPER CLOSED >> anim {}, idle {}, loot {}, quickWorld {}", local.isAnimating(), local.isIdle(), lootedItem != null, quickHopWorld != null);
					Worlds.loadWorlds();
					return -1;
				}
			}
			else if (local.distanceTo(NATURE_RUNE_WORLD_POINT) > 1) // no loot near, out of laws, no natures. start over
			{
				Movement.walkTo(NATURE_RUNE_WORLD_POINT);
				return -4;
			}
		}
		// all else fails, go back to the bank
		return move();
	}

	private int bank()
	{
		int natureRunes = Inventory.getCount(true,ItemID.NATURE_RUNE);
		if (natureRunes > 0)
		{
			Bank.depositAllExcept(ItemID.LAW_RUNE, ItemID.ENERGY_POTION4, ItemID.LOBSTER, ItemID.FIRE_RUNE);
			log.info("depositing nature runes");
			return -1;
		}

		final int lawsNeeded = 250 - Inventory.getCount(true, ItemID.LAW_RUNE);
		final int energyNeeded = 6 - Inventory.getCount((x) -> x.getName().equals("Energy potion(4)"));
		final int lobsterNeeded = Inventory.getFreeSlots() - 3;
		final boolean fireNeeded = Inventory.getCount(true, ItemID.FIRE_RUNE) < 5;

		if (fireNeeded)
		{
			log.info("Withdrawing 50 fire runes");
			Bank.withdraw(ItemID.FIRE_RUNE, 50, Bank.WithdrawMode.ITEM);
			return -1;
		}
		else if (energyNeeded > 0)
		{
			log.info("Withdrawing {} energy pots", energyNeeded);
			Bank.withdraw(ItemID.ENERGY_POTION4, energyNeeded, Bank.WithdrawMode.ITEM);
			return -2;
		}
		else if (lobsterNeeded > 0)
		{
			Bank.withdraw(ItemID.LOBSTER, lobsterNeeded, Bank.WithdrawMode.DEFAULT);
			return -2;
		}
		else if (lawsNeeded > 0)
		{
			log.info("Withdrawing {} laws", lawsNeeded);
			Bank.withdraw(ItemID.LAW_RUNE, lawsNeeded, Bank.WithdrawMode.ITEM);
			return -1;
		}
		return -1;
	}

	private int move()
	{
		TileObject bankObject = TileObjects.getNearest((x) -> x.hasAction("Bank"));

		if (bankObject == null)
		{
			log.info("Walking to bank area");
			Movement.walkTo(BankLocation.getNearest());
			return -4;
		}

		if (bankObject.distanceTo(Game.getClient().getLocalPlayer().getWorldLocation()) > 1)
		{
			log.info("finding bank booth");
			bankObject.interact("Bank");
			return -2;
		}

		if (!Bank.isOpen())
		{
			log.info("Opening bank");
			bankObject.interact("Bank");
		}

		return bank();
	}

	private void hop() {

		if (Game.getState() != GameState.LOGGED_IN) {
			return;
		}

		quickHopWorld = null;
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

			log.info("Hopping to " + world.getId());

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
			Game.getClient().playSoundEffect(SoundEffectID.TELEPORT_VWOOP);
			Worlds.hopTo(rsWorld);
			quickHopWorld = rsWorld;

		}
	}

	public boolean withinRange(Player player)
	{
		if (player == null)
		{
			return false;
		}

		final int wildernessLevel = Game.getWildyLevel();
		if (wildernessLevel == 0)
		{
			return false;
		}

		final Player local = Game.getClient().getLocalPlayer();
		if (player.equals(local))
		{
			return false;
		}

		final int minCombatLevel = Math.max(3,local.getCombatLevel() - wildernessLevel);
		final int maxCombatLevel = Math.min(Experience.MAX_COMBAT_LEVEL, local.getCombatLevel() + wildernessLevel);

		return (player.getCombatLevel() >= minCombatLevel && player.getCombatLevel() <= maxCombatLevel && !(player.equals(local)));
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved projectileMoved)
	{
		final Projectile projectile = projectileMoved.getProjectile();

//		Player caster = (Player) projectile.getInteracting().getInteracting();
//		Player target = (Player) projectile.getInteracting();

//		if (caster != null && !caster.equals(Game.getClient().getLocalPlayer()))
//		{
//			log.info("{} launched {}", caster.getName(), projectile.getId());
//		}
//		if (target != null && target.equals(Game.getClient().getLocalPlayer()))
//		{/\
//			log.info("You are being attacked");
//		}
		if (projectile.getId() == 143)
		{
			log.info("telegrab sent");
			teleGrabProjectile = projectile;

		}

	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (e.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		Player local = Players.getLocal();
		if (e.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{
			log.info("Chat message >> anim {}, idle {}, loot {}, quickWorld {} ", local.isAnimating(), local.isIdle(), lootedItem != null, quickHopWorld != null);
			quickHopWorld = null;

		}

	}

	@Subscribe
	public void onItemObtained(ItemObtained itemObtained)
	{

	}

	@Subscribe
	public void onItemSpawned(ItemSpawned itemSpawned)
	{
		TileItem item = itemSpawned.getItem();
	}

	private void grab(TileItem loot)
	{
		Magic.cast(Regular.TELEKINETIC_GRAB, loot);
		log.info("casted telegrab on {} [id:{}]", loot.getName(), loot.getId());
	}

	private void reset()
	{
		pker = null;
		quickHopWorld = null;
		teleGrabProjectile = null;
		lootedItem = null;
	}

}


