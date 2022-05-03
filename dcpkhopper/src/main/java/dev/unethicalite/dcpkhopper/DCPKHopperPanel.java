package dev.unethicalite.dcpkhopper;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

class DCPKHopperPanel extends OverlayPanel
{

    private final Client client;
    private final DCPKHopperPlugin plugin;
    private final DCPKHopperConfig config;

    @Inject
    private DCPKHopperPanel(Client client, DCPKHopperPlugin plugin, DCPKHopperConfig config)
    {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        getMenuEntries().add(new OverlayMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "DC PK Hopper"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        {
            String hopping = plugin.hopping ? "HOPPING" : "idle";
            String targetWorld = plugin.quickHopTargetWorld == null
                    ? "none"
                    : String.valueOf(plugin.quickHopTargetWorld.getId());

            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("PK Hopper")
                    .color(Color.cyan)
                    .build());
            
        
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Hopping:")
                    .leftColor(Color.LIGHT_GRAY)
                    .right(hopping)
                    .rightColor(Color.ORANGE)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Target world")
                    .leftColor(Color.LIGHT_GRAY)
                    .right(targetWorld)
                    .rightColor(Color.GREEN)
                    .build());
        }
        return super.render(graphics);
    }
}

