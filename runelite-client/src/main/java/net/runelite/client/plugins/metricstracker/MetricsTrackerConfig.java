package net.runelite.client.plugins.metricstracker;

import net.runelite.client.config.*;

@ConfigGroup( "metricstracker" )
public interface MetricsTrackerConfig extends Config
{
    @ConfigItem(
            keyName = "refreshRate",
            name = "Passive Refresh Rate",
            description = "Number of ticks per passive refresh, 0 to disable",
            position = 1
    )
    default int refreshRate() { return 5; }
}
