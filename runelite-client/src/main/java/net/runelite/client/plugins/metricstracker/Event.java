package net.runelite.client.plugins.metricstracker;

import lombok.Getter;

public class Event
{
    enum eventType
    {
        MASTER,
        NONE,
        ITEM_DROPS,
        XP_DROPS,
        LVLS_GAINED,
        MONSTERS_KILLED,
        DAMAGE_DEALT,
        DAMAGE_TAKEN,
        RESOURCES_GATHERED,
        CHAT_MESSAGES
    }

    // Type of the data point being created
    @Getter
    public eventType type;
    @Getter
    public String name;
    @Getter
    public int quantity;

    public Event( eventType type, String name, int quantity )
    {
        this.type = type;
        this.name = name;
        this.quantity = quantity;
    }

    public Event( eventType type )
    {
        this.type = type;
        this.name = null;
        this.quantity = 0;
    }
}
