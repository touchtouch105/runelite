package net.runelite.client.plugins.metricstracker;

import net.runelite.client.config.*;

@ConfigGroup( "metricstracker" )
public interface MetricsTrackerConfig extends Config
{
    @ConfigItem(
            keyName = "monstersKilled",
            name = "NPC Kill Tracker",
            description = "Enable the npc kill metrics tracker",
            position = 1
    )
    default boolean monstersKilled() { return true; }

}
