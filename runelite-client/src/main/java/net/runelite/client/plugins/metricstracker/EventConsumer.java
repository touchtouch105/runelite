package net.runelite.client.plugins.metricstracker;

import java.util.ArrayList;
import java.util.List;

public class EventConsumer
{
    private MetricsTrackerPanel panel;
    private List< Event > pendingEvents = new ArrayList<>();

    public EventConsumer( MetricsTrackerPanel panel )
    {
        this.panel = panel;
    }

    public void consumePendingEvents()
    {
        int sz = pendingEvents.size() - 1;
        for ( int i = sz; i >= 0; --i )
        {
            Event event = pendingEvents.get( i );
            panel.addEvent( event );
            pendingEvents.remove( i );
        }
    }

    public void addPendingEvent( Event event )
    {
        pendingEvents.add( event );
    }
}
