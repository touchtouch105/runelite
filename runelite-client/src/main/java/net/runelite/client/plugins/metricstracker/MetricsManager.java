package net.runelite.client.plugins.metricstracker;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MetricsManager
{
    private final static String overallKey = "OVERALL_KEY";
    private final static String overallAltKey = "OVERALL_ALT_KEY";
    private final static int NUM_DECIMAL_PLACES = 2;
    private final static float MSEC_PER_SEC = 1000;
    private final static float SEC_PER_MIN = 60;
    private final static float MIN_PER_HOUR = 60;
    public HashMap< String, MetricEvent> lastEvent;
    private HashMap< String, Long > startTimes;
    private HashMap< String, Long > quantities;
    private HashMap< String, String > remappedMetrics;

    public MetricsManager()
    {
        this.lastEvent = new HashMap<>();
        this.startTimes = new HashMap<>();
        this.quantities = new HashMap<>();
        this.remappedMetrics = new HashMap<>();

        this.lastEvent.put( overallKey, new MetricEvent( MetricEvent.eventType.MASTER ) );
        this.quantities.put( overallKey, ( long ) 0 );
        this.lastEvent.put( overallAltKey, new MetricEvent( MetricEvent.eventType.MASTER ) );
        this.quantities.put( overallAltKey, ( long ) 0 );
    }

    public void addDataPoint(MetricEvent metricEvent, boolean isSecondaryMetric, @Nullable String originalMetricName )
    {
        String key = metricEvent.getName();

        if ( isSecondaryMetric
        &&   originalMetricName != null )
        {
            remappedMetrics.put( metricEvent.getName(), originalMetricName );

            if ( !this.startTimes.containsKey( originalMetricName ) )
            {
                this.startTimes.put( originalMetricName, Instant.now().toEpochMilli() );
            }

            if ( !this.startTimes.containsKey( overallAltKey ) )
            {
                this.startTimes.put( overallAltKey, Instant.now().toEpochMilli() );
            }
        }

        if ( !this.startTimes.containsKey( key ) )
        {
            this.startTimes.put( key, Instant.now().toEpochMilli() );
        }

        if ( !this.startTimes.containsKey( overallKey ) )
        {
            this.startTimes.put( overallKey, Instant.now().toEpochMilli() );
        }

        this.lastEvent.put( key, metricEvent);
        this.lastEvent.put( overallKey, metricEvent);

        long quantity = 0;
        if ( this.quantities.containsKey( key ) )
        {
            quantity = this.quantities.get( key );
        }

        quantity += metricEvent.getQuantity();
        this.quantities.put( key, quantity );

        if ( !isSecondaryMetric )
        {
            quantity = this.quantities.get( overallKey ) + metricEvent.getQuantity();
            this.quantities.put( overallKey, quantity );
        }
        else
        {
            quantity = this.quantities.get( overallAltKey ) + metricEvent.getQuantity();
            this.quantities.put( overallAltKey, quantity );
        }
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

        return round( qps );
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

        return round( qph );
    }

    public float getOverallPerSecond( boolean isSecondaryOverall )
    {
        String key = ( isSecondaryOverall ) ? overallAltKey : overallKey;
        return getQuantityPerSecond( key );
    }

    public float getOverallPerHour( boolean isSecondaryOverall )
    {
        String key = ( isSecondaryOverall ) ? overallAltKey : overallKey;
        return getQuantityPerHour( key );
    }

    public long getCumulativeQuantity( String key )
    {
        if ( !this.quantities.containsKey( key ) )
        {
            return 0;
        }
        return this.quantities.get( key );
    }

    public long getOverallCumulativeQuantity( boolean isSecondaryOverall )
    {
        String key = ( isSecondaryOverall ) ? overallAltKey : overallKey;
        return getCumulativeQuantity( key );
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

        for ( String s : remappedMetrics.keySet() )
        {
            if ( remappedMetrics.get( s ).equals( key ) )
            {
                this.quantities.remove( s );
                this.startTimes.remove( s );
                this.lastEvent.remove( s );
            }
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
                if ( remappedMetrics.containsKey( keys[ i ] ) )
                {
                    if ( remappedMetrics.get( keys[ i ] ).equals( key ) )
                    {
                        continue;
                    }
                }

                if ( !( key.equals( keys[ i ] ) )
                &&   !( keys[ i ].equals( overallKey ) )
                &&   !( keys[ i ].equals( overallAltKey ) ) )
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

        this.lastEvent.put( overallKey, new MetricEvent( MetricEvent.eventType.MASTER ) );
        this.quantities.put( overallKey, ( long ) 0 );
        this.lastEvent.put( overallAltKey, new MetricEvent( MetricEvent.eventType.MASTER ) );
        this.quantities.put( overallAltKey, ( long ) 0 );
    }

    public String getMetricKey( String key )
    {
        if ( remappedMetrics.containsKey( key ) )
        {
            return remappedMetrics.get( key );
        }

        return key;
    }

    public List< String > getKeys()
    {
        List< String > list = new ArrayList<>();
        list.addAll( quantities.keySet() );
        list.remove( overallKey );
        list.remove( overallAltKey );
        return list;
    }

    private static float round( float d )
    {
        return BigDecimal.valueOf( d ).setScale( NUM_DECIMAL_PLACES, BigDecimal.ROUND_HALF_UP ).floatValue();
    }
}
