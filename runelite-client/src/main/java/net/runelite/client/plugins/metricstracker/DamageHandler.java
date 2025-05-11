package net.runelite.client.plugins.metricstracker;

import net.runelite.api.Actor;
import net.runelite.api.Hitsplat;
import net.runelite.api.NPC;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.game.NpcUtil;

import java.util.HashMap;

public class DamageHandler
{
    private int tickCounter = 0;
    private final int ticksToSelfDestruct = 100;
    private HashMap< Actor, Event > eventsToValidate = new HashMap<>();

    public boolean isMonsterKilledEvent( Hitsplat hitsplat, Actor actor, NpcUtil npcUtil )
    {
        if ( !( actor instanceof NPC ) )
        {
            return false;
        }

        if ( hitsplat.isMine() )
        {
            // Start tracking the mob after the player deals damage below 50% hp
            if ( actor.getHealthRatio() <= 0 || ( actor.getHealthRatio() <= actor.getHealthScale() / 2 )  )
            {
                return true;
            }

            if ( npcUtil.isDying( ( NPC ) actor) )
            {
                return true;
            };
        }

        return false;
    }

    public void emitMonsterKilledEvent( Actor actor )
    {
        Event event = new Event( Event.eventType.MONSTERS_KILLED, actor.getName(), 1 );
        tickCounter = 0;
        eventsToValidate.put( actor, event );
    }

    public void emitDamageDoneEvent( Actor actor, Hitsplat hitsplat, EventConsumer consumer )
    {
        Event event = new Event( Event.eventType.DAMAGE_DEALT, actor.getName(), hitsplat.getAmount() );
        consumer.addPendingEvent( event );
    }

    public void tick( EventConsumer consumer, NpcUtil npcUtil )
    {
        int sz = eventsToValidate.keySet().size() - 1;
        if ( sz >= 0 )
        {
            Actor actors[] = eventsToValidate.keySet().toArray( new Actor[ 0 ] );
            for ( int i = sz; i >= 0; --i )
            {
                Actor actor = actors[ i ];

                if ( isActorDead( actor, npcUtil ) )
                {
                    consumer.addPendingEvent( eventsToValidate.get( actor ) );
                    eventsToValidate.remove( actor );
                }
            }
            // Delete lists after a minute of inactivity to avoid any memory leaks
            tickCounter++;
            if ( tickCounter == ticksToSelfDestruct )
            {
                eventsToValidate.clear();
                tickCounter = 0;
            }
        }
    }

    private boolean isActorDead( Actor actor, NpcUtil npcUtil )
    {
        if ( actor == null
        ||   npcUtil.isDying( ( NPC ) actor )
        ||   damageHandlerCheckSpecialCases( ( NPC ) actor ) )
        {
            return true;
        }
        return false;
    }

    private boolean damageHandlerCheckSpecialCases( NPC npc )
    {
        int id = npc.getId();

        switch ( id )
        {
            case NpcID.NIGHTMARE_TOTEM_1_CHARGED:
            case NpcID.NIGHTMARE_TOTEM_2_CHARGED:
            case NpcID.NIGHTMARE_TOTEM_3_CHARGED:
            case NpcID.NIGHTMARE_TOTEM_4_CHARGED:
                return true;
            default:
                return false;
        }
    }
}
