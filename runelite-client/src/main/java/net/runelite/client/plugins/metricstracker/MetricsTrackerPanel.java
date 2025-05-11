package net.runelite.client.plugins.metricstracker;

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.DragAndDropReorderPane;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MetricsTrackerPanel extends PluginPanel
{
    @Inject
    private Client client;
    private final MetricsTrackerPlugin plugin;
    private final JPanel overallPanel = new JPanel();
    private final JLabel totalQuantity = new JLabel( "Killed:" );
    private final JLabel totalRate = new JLabel( "KPH:" );
    private final JLabel altQuantity = new JLabel( "Damage:" );
    private final JLabel altRate = new JLabel( "DPS:" );
    private final Map< MetricsInfoBox.infoBoxType, Map< String, MetricsInfoBox > > infoBoxes = new HashMap<>();
    private final Map< MetricsInfoBox.infoBoxType, MetricsManager > metrics = new HashMap<>();
    private MetricsInfoBox.infoBoxType currentDisplayType = MetricsInfoBox.infoBoxType.MONSTERS;
    JComponent infoBoxPanel;

    public MetricsTrackerPanel( MetricsTrackerPlugin metricsTrackerPlugin, Client client )
    {
        super();
        this.plugin = metricsTrackerPlugin;
        this.client = client;

        setBorder( new EmptyBorder( 6, 6, 6, 6 ) );
        setBackground( ColorScheme.DARK_GRAY_COLOR );
        setLayout( new BorderLayout() );

        final JPanel layoutPanel = new JPanel();
        BoxLayout boxLayout = new BoxLayout( layoutPanel, BoxLayout.Y_AXIS );
        layoutPanel.setLayout( boxLayout );
        add( layoutPanel, BorderLayout.NORTH );

        overallPanel.setBorder( new EmptyBorder( 10, 10, 10, 10 ) );
        overallPanel.setBackground( ColorScheme.DARKER_GRAY_COLOR );
        overallPanel.setLayout( new BorderLayout() );
        overallPanel.setVisible( true ); // this will only become visible when the player gets exp

        // Create reset all menu
        final JMenuItem reset = new JMenuItem( "Reset All" );
        reset.addActionListener( e -> plugin.resetState() );

        // Create popup menu
        final JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
        popupMenu.add( reset );

        overallPanel.setComponentPopupMenu( popupMenu );

        final JLabel overallIcon = new JLabel( new ImageIcon( ImageUtil.loadImageResource(metricsTrackerPlugin.getClass(), "/metrics_tracker_icon.png" ) ) );
        final JPanel overallInfo = new JPanel();

        overallInfo.setBackground( ColorScheme.DARKER_GRAY_COLOR );
        overallInfo.setLayout( new GridLayout( 2, 2 ) );
        overallInfo.setBorder( new EmptyBorder( 0, 10, 0, 0) );

        totalQuantity.setFont( FontManager.getRunescapeSmallFont() );
        totalRate.setFont( FontManager.getRunescapeSmallFont() );
        altQuantity.setFont( FontManager.getRunescapeSmallFont() );
        altRate.setFont( FontManager.getRunescapeSmallFont() );

        overallInfo.add( totalQuantity );
        overallInfo.add( totalRate );
        overallInfo.add( altQuantity );
        overallInfo.add( altRate );

        overallPanel.add( overallIcon, BorderLayout.WEST );
        overallPanel.add( overallInfo, BorderLayout.CENTER );

        infoBoxPanel = new DragAndDropReorderPane();

        layoutPanel.add( overallPanel );
        layoutPanel.add( infoBoxPanel );
    }

    public void addEvent( Event event )
    {
        MetricsInfoBox.infoBoxType type = getInfoBoxType( event.getType() );
        String eventOriginalName = event.getName();
        boolean isSecondaryMetric = false;

        if ( event.getType() == Event.eventType.DAMAGE_DEALT )
        {
            isSecondaryMetric = true;
            event.name = ( "DPS_" + eventOriginalName );
        }

        if ( metrics.containsKey( type ) )
        {
            metrics.get( type ).addDataPoint( event, isSecondaryMetric, eventOriginalName );
        }
        else
        {
            metrics.put( type, new MetricsManager() );
            metrics.get( type ).addDataPoint( event, isSecondaryMetric, eventOriginalName );
        }

        if ( type == currentDisplayType )
        {
            updateInfoBox( type, event, eventOriginalName );
        }

        updateOverallTrackerText();
    }

    private void updateInfoBox( MetricsInfoBox.infoBoxType type, Event event, String eventOriginalName )
    {
        Map< String, MetricsInfoBox > map;
        MetricsManager metric;

        if ( infoBoxes.containsKey( type ) )
        {
            map = infoBoxes.get( type );
        }
        else
        {
            map = new HashMap<>();
        }

        if ( !map.containsKey( eventOriginalName ) )
        {
            map.put( eventOriginalName, new MetricsInfoBox( plugin, infoBoxPanel, eventOriginalName, type ) );
            infoBoxes.put( type, map );
        }

        metric = metrics.get( type );
        MetricsInfoBox infoBox = infoBoxes.get( type ).get( eventOriginalName );

        if ( type == MetricsInfoBox.infoBoxType.MONSTERS
        &&   event.getType() == Event.eventType.DAMAGE_DEALT )
        {
            infoBox.update( infoBoxPanel,
                            eventOriginalName,
                            metric.getCumulativeQuantity( eventOriginalName ),
                            metric.getQuantityPerHour( eventOriginalName ),
                            metric.getCumulativeQuantity( event.getName() ),
                            metric.getQuantityPerSecond( event.getName() ) );
        }
        else
        {
            infoBox.update( infoBoxPanel,
                            eventOriginalName,
                            metric.getCumulativeQuantity( eventOriginalName ),
                            metric.getQuantityPerHour( eventOriginalName ) );
        }
    }

    private MetricsInfoBox.infoBoxType getInfoBoxType( Event.eventType eventType )
    {
        MetricsInfoBox.infoBoxType type = MetricsInfoBox.infoBoxType.NONE;
        switch ( eventType )
        {
            case MONSTERS_KILLED:
            case DAMAGE_DEALT:
                type = MetricsInfoBox.infoBoxType.MONSTERS;
                break;

            default:
                break;
        }

        return type;
    }

    public void resetAllInfoBoxes()
    {
        for ( MetricsManager metricsManager : metrics.values() )
        {
            metricsManager.resetAll();
        }

        for ( Map< String, MetricsInfoBox > map : infoBoxes.values() )
        {
            for ( MetricsInfoBox box : map.values() )
            {
                box.reset( infoBoxPanel );
            }
        }

        infoBoxes.clear();
        updateOverallTrackerText();
    }

    public void removeInfoBox( MetricsInfoBox.infoBoxType type, String name )
    {
        if ( infoBoxes.containsKey( type ) )
        {
            if ( infoBoxes.get( type ).containsKey( name ) )
            {
                infoBoxes.get( type ).get( name ).reset( infoBoxPanel );
                infoBoxes.get( type ).remove( name );
            }
        }

        if ( metrics.containsKey( type ) )
        {
            metrics.get( type ).reset( name );
            metrics.remove( type, name );
        }
    }

    public void removeOthers( MetricsInfoBox.infoBoxType type, String name )
    {
        Map< String, MetricsInfoBox > infoBoxesLocal;
        if ( !infoBoxes.containsKey( type ) )
        {
            return;
        }

        infoBoxesLocal = infoBoxes.get( type );
        int sz = infoBoxesLocal.keySet().size() - 1;
        if ( sz >= 0 )
        {
            String keys[] = infoBoxesLocal.keySet().toArray( new String[ 0 ] );
            for ( int i = sz; i >= 0; --i )
            {
                if ( !( keys[ i ].equals( name ) ) )
                {
                    removeInfoBox( type, keys[ i ] );
                }
            }
        }

        if ( metrics.containsKey( type ) )
        {
            metrics.get( type ).resetOthers( name );
        }
    }

    private void updateOverallTrackerText()
    {
        String quantity;
        String rate;
        String altQ;
        String altR;

        if ( !metrics.containsKey( currentDisplayType ) )
        {
            totalQuantity.setText( "Killed:" );
            totalRate.setText( "KPH:" );
            altQuantity.setText( "Damage:" );
            altRate.setText( "DPS:" );
            return;
        }

        switch ( currentDisplayType )
        {
            case MONSTERS:
            default:
                quantity = "Killed:" + metrics.get( currentDisplayType ).getOverallCumulativeQuantity( false );
                rate = "KPH:" + metrics.get( currentDisplayType ).getOverallPerHour( false );
                altQ = "Damage:" + metrics.get( currentDisplayType ).getOverallCumulativeQuantity( true );
                altR = "DPS:" + metrics.get( currentDisplayType ).getOverallPerSecond( true );
                break;
        }

        totalQuantity.setText( quantity );
        totalRate.setText( rate );
        altQuantity.setText( altQ );
        altRate.setText( altR );
    }

    public void refreshActive()
    {
        Event event;
        String infoBoxKey;
        for ( MetricsInfoBox.infoBoxType type : metrics.keySet() )
        {
            if ( type == currentDisplayType )
            {
                for ( String key : metrics.get( type ).getKeys() )
                {
                    infoBoxKey = metrics.get( type ).getMetricKey( key );
                    event = metrics.get( type ).lastEvent.get( key );
                    event.quantity = 0;

                    updateInfoBox( type, event, infoBoxKey );
                }
            }
        }

        updateOverallTrackerText();
    }
}
