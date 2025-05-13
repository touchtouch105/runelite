package net.runelite.client.plugins.metricstracker;

import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.components.MouseDragEventForwarder;
import net.runelite.client.util.ColorUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MetricsInfoBox extends JPanel
{
	public enum infoBoxType
	{
		NONE,
		MONSTERS,
		LOOT
	}

	private enum textLocation
	{
		NAME,
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		NUM_TEXT_LOCATIONS
	}

	private static final String HTML_LABEL_TEMPLATE =
		"<html><body style='color:%s'>%s<span style='color:white'>%s</span></body></html>";

	@Getter
	private final String name;
	private final JPanel container = new JPanel();
	private final JPanel headerPanel = new JPanel();
	private final JPanel statsPanel = new JPanel();
	private final JPanel namePanel = new JPanel();
	private final JLabel emptyLabel = new JLabel();
	private final JLabel nameStat = new JLabel();
	private final JLabel topLeftStat = new JLabel();
	private final JLabel bottomLeftStat = new JLabel();
	private final JLabel topRightStat = new JLabel();
	private final JLabel bottomRightStat = new JLabel();
	private JComponent panel;
	private infoBoxType type = infoBoxType.NONE;
	private final String[] displayText;
	private final String errorDisplayText[] = { "", "Quantity:", "Per Hour:", "Alt Quantity:", "Per Hour:" };
	private final String monsterDisplayText[] = { "", "Killed:", "KPH:", "Damage:", "DPS:" };
	private final String lootDisplayText[] = { "", "Drops:", "Per Hour", "Value:", "GP/H:" };

	MetricsInfoBox( MetricsTrackerPlugin plugin, JComponent panel, String name, infoBoxType type )
	{
		this.name = name;
		this.panel = panel;
		this.type = type;

		switch ( type )
		{
			case MONSTERS:
				this.displayText = monsterDisplayText;
				break;
			case LOOT:
				this.displayText = lootDisplayText;
				break;
			default:
				this.displayText = errorDisplayText;
				break;
		}

		setLayout( new BorderLayout() );
		setBorder( new EmptyBorder( 5, 0, 0, 0 ) );

		container.setLayout( new BorderLayout() );
		container.setBackground( ColorScheme.DARKER_GRAY_COLOR );

		// Create reset menu
		final JMenuItem reset = new JMenuItem( "Reset" );
		reset.addActionListener( e -> plugin.resetSingleMetric( type, name ) );

		final JMenuItem resetOthers = new JMenuItem( "Reset Others" );
		resetOthers.addActionListener( e -> plugin.resetOthers( type, name ) );

		final JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );

		popupMenu.add( reset );
		popupMenu.add( resetOthers );

		headerPanel.setBackground( ColorScheme.DARKER_GRAY_COLOR );
		headerPanel.setLayout( new DynamicGridLayout( 2, 1,0, -7 ) );

		statsPanel.setLayout( new DynamicGridLayout( 2, 2 ) );
		statsPanel.setBackground( ColorScheme.DARKER_GRAY_COLOR );
		statsPanel.setBorder( new EmptyBorder( 9, 2, 9, 2 ) );

		topLeftStat.setFont( FontManager.getRunescapeSmallFont() );
		bottomLeftStat.setFont( FontManager.getRunescapeSmallFont() );
		topRightStat.setFont( FontManager.getRunescapeSmallFont() );
		bottomRightStat.setFont( FontManager.getRunescapeSmallFont() );

		statsPanel.add( topLeftStat );
		statsPanel.add( topRightStat );
		statsPanel.add( bottomLeftStat );
		statsPanel.add( bottomRightStat );

		namePanel.setLayout( new BorderLayout() );
		namePanel.setBackground( ColorScheme.DARKER_GRAY_COLOR );
		namePanel.setBorder( new EmptyBorder( 1, 2, 1, 2 ) );

		nameStat.setFont( FontManager.getRunescapeSmallFont() );

		namePanel.add( nameStat );

		headerPanel.add( namePanel );
		headerPanel.add( statsPanel, BorderLayout.CENTER );
		container.add( headerPanel, BorderLayout.NORTH );

		container.setComponentPopupMenu( popupMenu );

		// forward mouse drag events to parent panel for drag and drop reordering
		MouseDragEventForwarder mouseDragEventForwarder = new MouseDragEventForwarder( panel );
		container.addMouseListener( mouseDragEventForwarder );
		container.addMouseMotionListener( mouseDragEventForwarder );

		add( container, BorderLayout.NORTH );
	}

	public void reset( JComponent panel )
	{
		panel.remove( this );
		panel.revalidate();
	}

	void update( JComponent panel, String name, long quantity, float qph )
	{
		SwingUtilities.invokeLater( () -> rebuildAsync( panel, name, quantity, qph ) );
	}

	void update( JComponent panel, String name, long quantity, float qph, long altQuantity, float altRate )
	{
		SwingUtilities.invokeLater( () -> rebuildAsync( panel, name, quantity, qph, altQuantity, altRate ) );
	}

	private void rebuildAsync( JComponent panel, String name, long quantity, float qph, long altQuantity, float altRate )
	{
		if ( getParent() != panel )
		{
			panel.add( this );
			panel.revalidate();
		}

		nameStat.setText( htmlLabel( displayText[ textLocation.NAME.ordinal() ], name ) );
		topLeftStat.setText( htmlLabel( displayText[ textLocation.TOP_LEFT.ordinal() ], quantity ) );
		topRightStat.setText( htmlLabel( displayText[ textLocation.TOP_RIGHT.ordinal() ], qph ) );
		bottomLeftStat.setText( htmlLabel( displayText[ textLocation.BOTTOM_LEFT.ordinal() ], altQuantity ) );
		bottomRightStat.setText( htmlLabel( displayText[ textLocation.BOTTOM_RIGHT.ordinal() ], altRate ) );
	}

	private void rebuildAsync( JComponent panel, String name, long quantity, float qph )
	{
		if ( getParent() != panel )
		{
			panel.add( this );
			panel.revalidate();
		}

		nameStat.setText( htmlLabel( displayText[ textLocation.NAME.ordinal() ], name ) );
		topLeftStat.setText( htmlLabel( displayText[ textLocation.TOP_LEFT.ordinal() ], quantity ) );
		topRightStat.setText( htmlLabel( displayText[ textLocation.TOP_RIGHT.ordinal() ], qph ) );
	}

	static String htmlLabel( String key, float value )
	{
		String valueStr = Float.toString( value );
		return htmlLabel( key, valueStr );
	}

	static String htmlLabel( String key, long value )
	{
		String valueStr = Integer.toString( ( int ) value );
		return htmlLabel( key, valueStr );
	}

	static String htmlLabel( String key, String valueStr )
	{
		return String.format( HTML_LABEL_TEMPLATE, ColorUtil.toHexColor( ColorScheme.LIGHT_GRAY_COLOR ), key, valueStr );
	}
}
