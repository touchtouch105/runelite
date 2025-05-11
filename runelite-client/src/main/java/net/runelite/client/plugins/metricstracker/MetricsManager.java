package net.runelite.client.plugins.metricstracker;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetricsManager
{
    private final static String overallKey = "OVERALL_KEY";
    private final static float MSEC_PER_SEC = 1000;
    private final static float SEC_PER_MIN = 60;
    private final static float MIN_PER_HOUR = 60;
    public HashMap< String, Event > lastEvent;
    private HashMap< String, Long > startTimes;
    private HashMap< String, Long > quantities;

    public MetricsManager()
    {
        this.lastEvent = new HashMap<>();
        this.startTimes = new HashMap<>();
        this.quantities = new HashMap<>();

        this.lastEvent.put( overallKey, new Event( Event.eventType.MASTER ) );
        this.quantities.put( overallKey, ( long ) 0 );
    }

    public void addDataPoint( Event event )
    {
        String key = event.getName();

        if ( !this.startTimes.containsKey( key ) )
        {
            this.startTimes.put( key, Instant.now().toEpochMilli() );
        }

        if ( !this.startTimes.containsKey( overallKey ) )
        {
            this.startTimes.put( overallKey, Instant.now().toEpochMilli() );
        }

        this.lastEvent.put( key, event );
        this.lastEvent.put( overallKey, event );

        long quantity = 0;
        if ( this.quantities.containsKey( key ) )
        {
            quantity = this.quantities.get( key );
        }

        quantity += event.getQuantity();
        this.quantities.put( key, quantity );

        quantity = this.quantities.get( overallKey ) + event.getQuantity();
        this.quantities.put( overallKey, quantity );
    }

    public float getQuantityPerSecond( String key )
    {
        float qps = 0;
        float runTime = 0;

        if ( this.startTimes.containsKey( key ) )
        {
            runTime = Instant.now().toEpochMilli() - this.startTimes.get( key );
            runTime /= MSEC_PER_SEC;
        }

        if ( this.quantities.containsKey( key ) )
        {
            qps = this.quantities.get( key );

            if ( runTime == 0 )
            {
                return ( this.quantities.get( key ) );
            }

            qps /= runTime;
        }

        return qps;
    }
    public float getQuantityPerHour( String key )
    {
        float qph = 0;
        float runTime = 0;

        if ( this.startTimes.containsKey( key ) )
        {
            runTime = Instant.now().toEpochMilli() - this.startTimes.get( key );
            runTime /= MSEC_PER_SEC;
            runTime /= SEC_PER_MIN;
            runTime /= MIN_PER_HOUR;
        }

        if ( this.quantities.containsKey( key ) )
        {
            qph = this.quantities.get( key );

            if ( runTime == 0 )
            {
                return ( this.quantities.get( key ) );
            }

            qph /= runTime;
        }

        return qph;
    }

    public float getOverallPerSecond()
    {
        float qps = 0;
        float runTime = 0;
        String key = overallKey;

        if ( this.startTimes.containsKey( key ) )
        {
            runTime = Instant.now().toEpochMilli() - this.startTimes.get( key );
            runTime /= MSEC_PER_SEC;
        }

        if ( this.quantities.containsKey( key ) )
        {
            qps = this.quantities.get( key );

            if ( runTime == 0 )
            {
                return ( this.quantities.get( key ) );
            }

            qps /= runTime;
        }

        return qps;
    }

    public float getOverallPerHour()
    {
        float qph = 0;
        float runTime = 0;
        String key = overallKey;

        if ( this.startTimes.containsKey( key ) )
        {
            runTime = Instant.now().toEpochMilli() - this.startTimes.get( key );
            runTime /= MSEC_PER_SEC;
            runTime /= SEC_PER_MIN;
            runTime /= MIN_PER_HOUR;

        }

        if ( this.quantities.containsKey( key ) )
        {
            qph = this.quantities.get( key );

            if ( runTime == 0 )
            {
                return ( this.quantities.get( key ) );
            }

            qph /= runTime;
        }

        return qph;
    }

    public long getCumulativeQuantity( String key )
    {
        if ( !this.quantities.containsKey( key ) )
        {
            return 0;
        }
        return this.quantities.get( key );
    }

    public long getOverallCumulativeQuantity()
    {
        String key = overallKey;
        if ( !this.quantities.containsKey( key ) )
        {
            return 0;
        }
        return this.quantities.get( key );
    }

    public void reset( String key )
    {
        if ( this.quantities.containsKey( key ) )
        {
            this.quantities.remove( key );
        }

        if ( this.startTimes.containsKey( key ) )
        {
            this.startTimes.remove( key );
        }

        if ( this.lastEvent.containsKey( key ) )
        {
            this.lastEvent.remove( key );
        }
    }

    public void resetOthers( String key )
    {
        int sz = this.quantities.keySet().size() - 1;

        if ( sz >= 0 )
        {
            String keys[] = this.quantities.keySet().toArray( new String[ 0 ] );
            for ( int i = sz; i >=0; --i )
            {
                if ( !( key.equals( keys[ i ] ) )
                &&   !( keys[ i ].equals( overallKey ) ) )
                {
                    reset( keys[ i ] );
                }
            }
        }
    }

    public void resetAll()
    {
        this.quantities.clear();
        this.startTimes.clear();
        this.lastEvent.clear();

        this.lastEvent = new HashMap<>();
        this.startTimes = new HashMap<>();
        this.quantities = new HashMap<>();

        this.lastEvent.put( overallKey, new Event( Event.eventType.MASTER ) );
        this.quantities.put( overallKey, ( long ) 0 );
    }

    public List< String > getKeys()
    {
        List< String > list = new ArrayList<>();
        list.addAll( quantities.keySet() );
        list.remove( overallKey );
        return list;
    }

}
