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
     * Converts the tick label / time unit values to another time unit (larger or smaller),
     * if the difference between them is too large or too small.
     * (E.g. 1st tick label is 1wk, 2nd is 100wk, then the time units might be switched to years)
     *
     * @param timeUnit initially defined time unit
     * @param ticks calculated ticks by the Extended Wilkinson (or another algorithm)
     * @param multiplier multiplier that has to be used when switching time units (e.g. when switching from minutes to hours a multiplier of 60 has to be used.)
     * @param switchConstants constants when to switch the time unit
     * @return pair of a tick array and a time label string array
     */
    Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, IUnitSwitchConstants switchConstants);

    /**
     * @return the label of the respective time unit (e.g. "s" for seconds or "h" for hours)
     */
    String getUnitLabel();
}
