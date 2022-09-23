package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The ITimeUnit interface contains multiple methods to manipulate LocalDateTime instances.
 * These methods can be implemented for a specific TimeUnit (e.g. {@link HourTimeUnit}).
 */
public interface ITimeUnit {

    /**
     * Floors the given LocalDateTime.
     * (E.g. if TimeUnit is {@link DayTimeUnit} the Hours, Minutes, ... will be set to 0)
     *
     * @param value to floor
     * @return floored LocalDateTime
     */
    LocalDateTime floor(LocalDateTime value);

    /**
     * Adds the given delta to the {@link LocalDateTime} value.
     *
     * @param value {@link LocalDateTime} value which will be incremented by the delta
     * @param delta increments the value
     * @return incremented {@link LocalDateTime}
     */
    LocalDateTime increment(LocalDateTime value, double delta);

    /**
     * Converts the ticks to time labels.
     *
     * @param timeUnit TODO
     * @param ticks
     * @param multiplier
     * @param switchConstants
     * @return pair of a tick array and a time label string array
     */
    Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, UnitSwitchConstants switchConstants);

    /**
     * @return the label of the respective time unit (e.g. "s" for seconds or "h" for hours)
     */
    String getUnitLabel();
}
