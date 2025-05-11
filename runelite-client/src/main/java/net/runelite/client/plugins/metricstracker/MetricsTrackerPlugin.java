package net.runelite.client.plugins.metricstracker;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
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
    private static final String ICON_FILE = "/metrics_tracker_icon.png";
    private static final String PLUGIN_NAME = "Metrics Tracker";
    private final DamageHandler damageHandler = new DamageHandler();
    private MetricsTrackerPanel loggerPanel;
    private EventConsumer consumer;
    private NavigationButton navigationButton;
    private int tickCounter = 0;

    @Override
    protected void startUp() throws Exception
    {
         loggerPanel = new MetricsTrackerPanel( this , config, client );
         final BufferedImage icon = ImageUtil.loadImageResource( getClass(), ICON_FILE );
         navigationButton = NavigationButton.builder()
            .tooltip( PLUGIN_NAME )
            .icon( icon )
            .priority( 6 )
            .panel( loggerPanel )
            .build();
        clientToolbar.addNavigation( navigationButton );
        consumer = new EventConsumer( loggerPanel );
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

        damageHandler.tick( consumer, npcUtil );
        consumer.consumePendingEvents();
    }

    @Subscribe
    public void onHitsplatApplied( HitsplatApplied event )
    {

        Actor actor = event.getActor();
        Hitsplat hitsplat = event.getHitsplat();

        if ( hitsplat.isMine()
        &&   ( actor instanceof NPC) )
        {
            damageHandler.emitDamageDoneEvent( actor, hitsplat, consumer );
        }

        if ( damageHandler.isMonsterKilledEvent( hitsplat, actor, npcUtil ) )
        {
            damageHandler.emitMonsterKilledEvent( actor );
        }
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
