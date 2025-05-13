package net.runelite.client.plugins.metricstracker;

import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.loottracker.LootReceived;

import java.util.Collection;

public class LootHandler
{
    public void lootReceived( final LootReceived lootReceived, ItemManager itemManager, EventBus eventBus )
    {
        final Collection<ItemStack> items = lootReceived.getItems();

        for ( ItemStack item : items )
        {
            MetricEvent metricEvent = new MetricEvent( MetricEvent.eventType.ITEM_DROPS, itemManager.getItemComposition(item.getId()).getName(), item.getQuantity() );
            eventBus.post( metricEvent );

            metricEvent = new MetricEvent( MetricEvent.eventType.ITEM_GP_EARNED, itemManager.getItemComposition( item.getId() ).getName(), itemManager.getItemPrice( item.getId() ) );
            eventBus.post( metricEvent );
        }
    }
}
