package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.ITimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.TimeUnit;
import hageldave.jplotter.util.Pair;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

/**
 * The DateTimeWilkinson is an extension of the {@link ExtendedWilkinson} tick labeling mechanism.
 * It uses the ExtendedWilkinson to initially calculate the positioning of the tick labels which are then converted to
 * {@link LocalDateTime} units.
 * It calculates those label values by using a {@link TimeUnit} and a reference LocalDateTime
 * (point positioned in the left border/most left point of the chart).
 * Then each tick label value (the ones calculated by the ExtendedWilkinson algorithm) is used as a {@link TimeUnit} value, which is added to the referenceDateTime.
 * A custom formatting {@link BiFunction} can also be set, as there might be special formatting requirements
 * for the time values.
 */
public class DateTimeWilkinson extends ExtendedWilkinson {
    protected TimeUnit timeUnit;
    protected double scalingFactor;
    protected LocalDateTime referenceDateTime;
    protected BiFunction<LocalDateTime, Duration, String> formattingFunction;

    /**
     * Creates an instance of the DateTimeWilkinson.
     *
     * @param timeUnit the timeUnit used when calculating the difference between the axis ticks
     * @param scalingFactor a custom multiplication factor (e.g. used when difference of 1.0 should be 1.5 in the timeUnit)
     * @param referenceDateTime most left point has this value
     * @param formattingFunction custom function used to format the time labels (e.g. showing only the date, not the time)
     */
    public DateTimeWilkinson(final TimeUnit timeUnit, final double scalingFactor, final LocalDateTime referenceDateTime, final BiFunction<LocalDateTime, Duration, String> formattingFunction) {
        this.timeUnit = timeUnit;
        this.scalingFactor = scalingFactor;
        this.referenceDateTime = referenceDateTime;
        this.formattingFunction = formattingFunction;
    }

    public DateTimeWilkinson(final TimeUnit timeUnit, final double scalingFactor, final LocalDateTime referenceDateTime) {
        this.timeUnit = timeUnit;
        this.scalingFactor = scalingFactor;
        this.referenceDateTime = referenceDateTime;
        this.formattingFunction = DateTimeWilkinson::switchFormat;
    }

    protected String[] labelsForTicks(double[] ticks, ITimeUnit timeUnit) {
        String[] labels = new String[ticks.length];

        for (int i = 0; i < ticks.length - 1; i++) {
            double currentDiff2Ref = ticks[i];
            currentDiff2Ref *= scalingFactor;

            double nextDiff2Ref = ticks[i + 1];
            nextDiff2Ref *= scalingFactor;

            LocalDateTime currentDateTime = timeUnit.increment(referenceDateTime, currentDiff2Ref);
            LocalDateTime nextDateTime = timeUnit.increment(referenceDateTime, nextDiff2Ref);

            Duration duration = Duration.between(currentDateTime, nextDateTime);
            labels[i] = formattingFunction.apply(currentDateTime, duration);

            if (i == ticks.length - 2) {
                labels[i + 1] = formattingFunction.apply(nextDateTime, duration);
            }
        }
        return labels;
    }

    @Override
    public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis) {
        double[] ticks = getTicks(min, max, desiredNumTicks, super.Q, super.w);
        String[] labelsForTicks = labelsForTicks(ticks, timeUnit);
        return new Pair<>(ticks, labelsForTicks);
    }

    /**
     * @return the function used to format the time labels
     */
    public BiFunction<LocalDateTime, Duration, String> getFormattingFunction() {
        return formattingFunction;
    }

    /**
     * Sets a new formatting function.
     *
     * @param formattingFunction the function used to format the time labels
     */
    public void setFormattingFunction(BiFunction<LocalDateTime, Duration, String> formattingFunction) {
        this.formattingFunction = formattingFunction;
    }

    /**
     * Returns the given {@link LocalDateTime} as a string.
     *
     * @param localDateTime will be returned as a string
     * @param duration - interface parameter, not used here
     * @return string value of localDateTime
     */
    public static String getDateTime(LocalDateTime localDateTime, Duration duration) {
        return localDateTime.toString();
    }

    /**
     * Formats the given localDateTime with a {@link DateTimeFormatter#ISO_DATE} formatter.
     *
     * @param localDateTime to format
     * @param duration - interface parameter, not used here
     * @return formatted localDateTime
     */
    public static String getDate(LocalDateTime localDateTime, Duration duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        return formatter.format(localDateTime);
    }

    /**
     * Formats the given localDateTime with a {@link DateTimeFormatter#ISO_TIME} formatter.
     *
     * @param localDateTime to format
     * @param duration - interface parameter, not used here
     * @return formatted localDateTime
     */
    public static String getTime(LocalDateTime localDateTime, Duration duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
        return formatter.format(localDateTime);
    }

    /**
     * Formats the localDateTime according to the given duration.
     * If the time duration is large enough (larger than 86400000 millis), the localDateTime will be formatted by
     * {@link DateTimeFormatter#ISO_DATE}, else by {@link DateTimeFormatter#ISO_TIME}
     *
     * @param localDateTime reference point
     * @param duration amount of time added to the localDateTime
     * @return the formatted localDateTime
     */
    public static String switchFormat(LocalDateTime localDateTime, Duration duration) {
        LocalDateTime localDateTimeCopy = LocalDateTime.from(localDateTime);
        LocalDateTime ldt = localDateTimeCopy.plus(duration);

        long millisCurrentTick = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        long millisNextTick = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        long differenceInMillis = millisNextTick - millisCurrentTick;

        DateTimeFormatter formatter;
        // Duration of 1 day in millis (or zero difference in millis, but that is a bit clunky currently)
        if (Math.abs(differenceInMillis) > 86400000) {
            formatter = DateTimeFormatter.ISO_DATE;
        } else {
            formatter = DateTimeFormatter.ISO_TIME;
        }
        return formatter.format(localDateTime);
    }
}