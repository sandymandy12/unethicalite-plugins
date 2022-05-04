package dev.unethicalite.dctelegraber;

import net.runelite.client.config.*;

@ConfigGroup("dctelegraber")
public interface DCTelegraberConfig extends Config
{
	@ConfigSection(
			name = "Section 1",
			description = "placeholder",
			closedByDefault = true,
			position = 0
	)
	String section1 = "section1";

	@ConfigItem(
			keyName = "loots",
			name = "Loot Items",
			description = "Items to loot separated by comma. ex: Lobster,Tuna",
			position = 0
	)
	default String loot()
	{
		return "Any";
	}

	@ConfigItem(
			keyName = "lootValue",
			name = "Loot GP value",
			description = "Items to loot by value, -1 to check by name only",
			position = 1
	)
	default int lootValue()
	{
		return -1;
	}

	@Range(max = 100)
	@ConfigItem(
			keyName = "lootRange",
			name = "Loot range",
			description = "Monster attack range",
			position = 2
	)
	default int lootRange()
	{
		return 15;
	}

	@ConfigItem(
			keyName = "untradables",
			name = "Loot untradables",
			description = "Loot untradables",
			position = 3
	)
	default boolean untradables()
	{
		return true;
	}

	@ConfigItem(
			position = 4,
			keyName = "noLoot",
			name = "No loot",
			description = "hop if world doesn't have loot"
	)
	default boolean noLoot()
	{
		return false;
	}

	@ConfigSection(
			name = "Health",
			description = "General settings",
			position = 5,
			closedByDefault = true
	)
	String health = "Health";

	@Range(max = 100)
	@ConfigItem(
			keyName = "eatHealthPercent",
			name = "Health %",
			description = "Health % to eat at",
			position = 6
	)
	default int healthPercent()
	{
		return 65;
	}

	@ConfigItem(
			keyName = "eat",
			name = "Eat food",
			description = "Eat food to heal",
			position = 7
	)
	default boolean eat()
	{
		return true;
	}

}

