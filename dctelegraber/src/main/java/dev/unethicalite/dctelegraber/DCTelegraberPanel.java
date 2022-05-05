package dev.unethicalite.dctelegraber;

import dev.unethicalite.api.entities.Players;
import dev.unethicalite.api.entities.TileItems;
import dev.unethicalite.api.game.Game;
import dev.unethicalite.api.items.Inventory;
import dev.unethicalite.api.movement.Movement;
import dev.unethicalite.api.*;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.TileItem;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

class DCTelegraberPanel extends OverlayPanel
{

    private final Client client;
    private final DCTelegraberPlugin plugin;
    private final DCTelegraberConfig config;

    @Inject
    private DCTelegraberPanel(Client client, DCTelegraberPlugin plugin, DCTelegraberConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "DC PK Telegraber"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        {
        }

//        final Player pker = Players.getNearest((plugin::withinRange));


        final int natureRunes = Inventory.getCount(ItemID.NATURE_RUNE);
        final int lawRunes = Inventory.getCount(ItemID.LAW_RUNE);

        final String qhw = plugin.quickHopWorld == null ? "no target" : plugin.quickHopWorld.getId()+"" ;
        boolean isIdle = Game.getClient().getLocalPlayer().isIdle();

        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Telegraber [" + client.getLocalPlayer().getActionFrame() + "]")
                .color(isIdle ? Color.cyan : Color.yellow)
                .build());


        panelComponent.getChildren().add(LineComponent.builder()
                .left("Law runes")
                .leftColor(Color.LIGHT_GRAY)
                .right(String.valueOf(lawRunes))
                .rightColor(Color.ORANGE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Nature runes")
                .leftColor(Color.LIGHT_GRAY)
                .right(String.valueOf(natureRunes))
                .rightColor(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .right("Nature runes")
                .leftColor(Color.LIGHT_GRAY)
                .left("Looted item")
                .rightColor(Color.GREEN)
                .build());

        return super.render(graphics);
    }
}

