package net.runelite.client.plugins.metricstracker;

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.DragAndDropReorderPane;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.SwingUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class MetricsTrackerPanel extends PluginPanel
{
    @Inject
    private Client client;
    private final MetricsTrackerPlugin plugin;
    private final String PANEL_KEY_STRING = "PanelStringMasterKey";
    private JPanel actionsPanel = new JPanel();
    private final JPanel overallPanel = new JPanel();

    private final JLabel totalQuantity = new JLabel( "Killed" );
    private final JLabel totalRate = new JLabel( "Per hour" );
    private final Map< MetricsInfoBox.infoBoxType, Map< String, MetricsInfoBox > > infoBoxes = new HashMap<>();
    private final Map< MetricsInfoBox.infoBoxType, MetricsManager > metrics = new HashMap<>();
    private final JButton monstersButton = new JButton();
    private final JButton dpsButton = new JButton();
    private MetricsTrackerConfig config;
    private ImageIcon icon;
    private MetricsInfoBox.infoBoxType currentDisplayType = MetricsInfoBox.infoBoxType.MONSTERS;
    JComponent infoBoxPanel;

    public MetricsTrackerPanel( MetricsTrackerPlugin metricsTrackerPlugin, MetricsTrackerConfig config, Client client )
    {
        super();
        this.plugin = metricsTrackerPlugin;
        this.config = config;
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

        popupMenu.addPopupMenuListener( new PopupMenuListener()
        {
            @Override
            public void popupMenuWillBecomeVisible( PopupMenuEvent popupMenuEvent )
            {
            }

            @Override
            public void popupMenuWillBecomeInvisible( PopupMenuEvent popupMenuEvent )
            {
            }

            @Override
            public void popupMenuCanceled( PopupMenuEvent popupMenuEvent )
            {
            }
        });
        overallPanel.setComponentPopupMenu( popupMenu );

        final JLabel overallIcon = new JLabel( new ImageIcon( ImageUtil.loadImageResource(metricsTrackerPlugin.getClass(), "/metrics_tracker_icon.png" ) ) );
        final BufferedImage expandedImg = ImageUtil.loadImageResource(metricsTrackerPlugin.getClass(), "/metrics_tracker_icon.png" );
        icon = new ImageIcon(expandedImg);;

        final JPanel overallInfo = new JPanel();
        overallInfo.setBackground( ColorScheme.DARKER_GRAY_COLOR );
        overallInfo.setLayout( new GridLayout( 2, 1 ) );
        overallInfo.setBorder( new EmptyBorder( 0, 10, 0, 0) );

        totalQuantity.setFont( FontManager.getRunescapeSmallFont() );
        totalRate.setFont( FontManager.getRunescapeSmallFont() );

        overallInfo.add( totalQuantity );
        overallInfo.add( totalRate );

        overallPanel.add( overallIcon, BorderLayout.WEST );
        overallPanel.add( overallInfo, BorderLayout.CENTER );

        infoBoxPanel = new DragAndDropReorderPane();
        actionsPanel = buildActionsPanel();

        layoutPanel.add( actionsPanel );
        layoutPanel.add( overallPanel );
        layoutPanel.add( infoBoxPanel );
    }

    private JPanel buildActionsPanel()
    {
        final JPanel actionsContainer = new JPanel();
        actionsContainer.setLayout(new BorderLayout());
        actionsContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        actionsContainer.setPreferredSize(new Dimension(0, 30));
        actionsContainer.setBorder(new EmptyBorder(5, 5, 5, 10));

        final JPanel viewControls = new JPanel(new GridLayout(1, 3, 10, 0));
        viewControls.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        SwingUtil.removeButtonDecorations(monstersButton);
        monstersButton.setIcon(icon);
        monstersButton.setSelectedIcon(icon);
        SwingUtil.addModalTooltip(monstersButton, "", "Monsters");
        monstersButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        monstersButton.setUI(new BasicButtonUI()); // substance breaks the layout
        monstersButton.addActionListener(ev -> loadInfoBoxes( MetricsInfoBox.infoBoxType.MONSTERS ));
        viewControls.add(monstersButton);

        SwingUtil.removeButtonDecorations(dpsButton);
        dpsButton.setIcon(icon);
        dpsButton.setSelectedIcon(icon);
        SwingUtil.addModalTooltip(dpsButton, "", "DPS");
        dpsButton.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        dpsButton.setUI(new BasicButtonUI()); // substance breaks the layout
        dpsButton.addActionListener(ev -> loadInfoBoxes( MetricsInfoBox.infoBoxType.DPS ));
        viewControls.add(dpsButton);

        final JPanel leftTitleContainer = new JPanel(new BorderLayout(5, 0));
        leftTitleContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        actionsContainer.add(viewControls, BorderLayout.EAST);
        actionsContainer.add(leftTitleContainer, BorderLayout.WEST);

        actionsContainer.setVisible( true );
        return actionsContainer;
    }

    private void loadMetrics( MetricsInfoBox.infoBoxType type )
    {
        if ( !infoBoxes.containsKey( type )
        ||   metrics.containsKey( type ) )
        {
            return;
        }

        MetricsManager mgr = metrics.get( type );
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
            infoBox.update( infoBoxPanel, eventOriginalName, metric.getCumulativeQuantity( eventOriginalName ), metric.getQuantityPerHour( eventOriginalName ), metric.getQuantityPerSecond( event.getName() ) );
        }
        else
        {
            infoBox.update( infoBoxPanel, eventOriginalName, metric.getCumulativeQuantity( eventOriginalName ), metric.getQuantityPerHour( eventOriginalName ) );
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

        totalQuantity.setText( "Total:" );
        totalRate.setText( "Total Rate:" );
    }

    public void loadInfoBoxes( MetricsInfoBox.infoBoxType type )
    {
        currentDisplayType = type;

        for ( MetricsInfoBox.infoBoxType t : infoBoxes.keySet() )
        {
            for ( String name : infoBoxes.get( t ).keySet() )
            {
                infoBoxes.get( t ).get( name ).reset( infoBoxPanel );
            }
        }

        refreshActive();
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

        if ( !metrics.containsKey( currentDisplayType ) )
        {
            totalQuantity.setText( "Total Killed:" );
            totalRate.setText( "Total Per Hour:" );
            return;
        }

        switch ( currentDisplayType )
        {
            case DPS:
                quantity = "Total Damage:" + metrics.get( currentDisplayType ).getOverallCumulativeQuantity();
                rate = "Total DPS:" + metrics.get( currentDisplayType ).getOverallPerSecond();
                break;
            case MONSTERS:
            default:
                quantity = "Total Killed:" + metrics.get( currentDisplayType ).getOverallCumulativeQuantity();
                rate = "Total Per Hour:" + metrics.get( currentDisplayType ).getOverallPerHour();
                break;
        }

        totalQuantity.setText( quantity );
        totalRate.setText( rate );
    }

    public void refreshActive()
    {
        Event event;

        for ( MetricsInfoBox.infoBoxType type : metrics.keySet() )
        {
            if ( type == currentDisplayType )
            {
                for ( String key : metrics.get( type ).getKeys() )
                {
                    String metricKey = metrics.get( type ).getMetricKey( key );
                    event = metrics.get( type ).lastEvent.get( key );
                    event.quantity = 0;

                    updateInfoBox( type, event, metricKey );
                }
            }
        }

        updateOverallTrackerText();
    }
}
