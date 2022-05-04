package dev.unethicalite.dctelegraber;

import dev.unethicalite.api.entities.TileItems;
import dev.unethicalite.api.movement.Movement;
import dev.unethicalite.api.*;
import net.runelite.api.Client;
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

        boolean isIdle = client.getLocalPlayer().isIdle();
        panelComponent.getChildren().add(TitleComponent.builder()
                .text("Telegraber" + (isIdle ? "" : "[doing]"))
                .color(isIdle ? Color.cyan : Color.yellow)
                .build());


        panelComponent.getChildren().add(LineComponent.builder()
                .left("Projectile:")
                .leftColor(Color.LIGHT_GRAY)
                .right(plugin.teleGrabProjectile == null ? "none" : plugin.teleGrabProjectile.getId()+"")
                .rightColor(Color.ORANGE)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Cycles:")
                .leftColor(Color.LIGHT_GRAY)
                .right(plugin.teleGrabProjectile != null ? String.valueOf(plugin.teleGrabProjectile.getEndCycle()) : "0")
                .rightColor(Color.GREEN)
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .right(plugin.lootedItem != null ? String.valueOf(plugin.lootedItem.getItemId()) : "no loot")
                .leftColor(Movement.isWalking() ? Color.ORANGE : Color.LIGHT_GRAY)
                .left("Looted item")
                .rightColor(Color.GREEN)
                .build());

        return super.render(graphics);
    }
}

