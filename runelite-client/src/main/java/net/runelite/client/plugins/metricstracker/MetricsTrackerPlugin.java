package net.runelite.client.plugins.metricstracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.loottracker.LootReceived;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Metrics Tracker",
        description = "Trackers miscellaneous player metrics"
)
public class MetricsTrackerPlugin extends Plugin
{
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ConfigManager configManager;
    @Inject
    private EventBus eventBus;
    @Inject
    private NpcUtil npcUtil;
    @Inject
    private MetricsTrackerConfig config;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private ItemManager itemManager;
    private static final String ICON_FILE = "/metrics_tracker_icon.png";
    private static final String PLUGIN_NAME = "Metrics Tracker";
    private final DamageHandler damageHandler = new DamageHandler();
    private final LootHandler lootHandler = new LootHandler();
    private MetricsTrackerPanel loggerPanel;
    private NavigationButton navigationButton;
    private int tickCounter = 0;
    private boolean lootInvalid = false;

    @Override
    protected void startUp() throws Exception
    {
         loggerPanel = new MetricsTrackerPanel( this, client );
         final BufferedImage icon = ImageUtil.loadImageResource( getClass(), ICON_FILE );
         navigationButton = NavigationButton.builder()
            .tooltip( PLUGIN_NAME )
            .icon( icon )
            .priority( 6 )
            .panel( loggerPanel )
            .build();
        clientToolbar.addNavigation( navigationButton );
    }

    @Override
    protected void shutDown() throws Exception
    {
        resetState();
        clientToolbar.removeNavigation( navigationButton );
    }

    @Provides
    MetricsTrackerConfig provideConfig( ConfigManager configManager )
    {
        return configManager.getConfig( MetricsTrackerConfig.class );
    }

    @Subscribe
    public void onLootReceived( final LootReceived lootReceived )
    {
        lootHandler.lootReceived( lootReceived, itemManager, eventBus );
    }

    @Subscribe
    public void onMetricEvent( MetricEvent metricEvent )
    {
        loggerPanel.addEvent( metricEvent );
    }

    @Subscribe
    public void onGameTick( GameTick gameTick )
    {
        if ( config.refreshRate() > 0 )
        {
            tickCounter = ( tickCounter + 1 ) % config.refreshRate();
            if ( tickCounter == 0 )
            {
                loggerPanel.refreshActive();
            }
        }

        damageHandler.tick( npcUtil, eventBus );
    }

    @Subscribe
    public void onHitsplatApplied( HitsplatApplied event )
    {
        damageHandler.hitsplatApplied( event, npcUtil, eventBus );
    }

    public void resetState()
    {
        loggerPanel.resetAllInfoBoxes();
    }

    public void resetSingleMetric( MetricsInfoBox.infoBoxType type, String name )
    {
        loggerPanel.removeInfoBox( type, name );
    }

    void resetOthers( MetricsInfoBox.infoBoxType type, String name )
    {
        loggerPanel.removeOthers( type, name );
    }
}
