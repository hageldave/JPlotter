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

public class DateTimeWilkinson extends ExtendedWilkinson {
    protected TimeUnit timeUnit;
    protected double number2unit;
    protected LocalDateTime referenceDateTime;
    protected BiFunction<LocalDateTime, Duration, String> formattingFunction;

    public DateTimeWilkinson(final TimeUnit timeUnit, final double number2unit, final LocalDateTime referenceDateTime, final BiFunction<LocalDateTime, Duration, String> formattingFunction) {
        this.timeUnit = timeUnit;
        this.number2unit = number2unit;
        this.referenceDateTime = referenceDateTime;
        this.formattingFunction = formattingFunction;
    }

    public DateTimeWilkinson(final TimeUnit timeUnit, final double number2unit, final LocalDateTime referenceDateTime) {
        this.timeUnit = timeUnit;
        this.number2unit = number2unit;
        this.referenceDateTime = referenceDateTime;
        this.formattingFunction = this::switchFormat;
    }

    protected String[] labelsForTicks(double[] ticks, ITimeUnit timeUnit) {
        String[] labels = new String[ticks.length];

        for (int i = 0; i < ticks.length - 1; i++) {
            double currentDiff2Ref = ticks[i];
            currentDiff2Ref *= number2unit;

            double nextDiff2Ref = ticks[i + 1];
            nextDiff2Ref *= number2unit;

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

    public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis) {
        double[] ticks = getTicks(min, max, desiredNumTicks, super.Q, super.w);
        String[] labelsForTicks = labelsForTicks(ticks, timeUnit);
        return new Pair<>(ticks, labelsForTicks);
    }


    public BiFunction<LocalDateTime, Duration, String> getFormattingFunction() {
        return formattingFunction;
    }

    public void setFormattingFunction(BiFunction<LocalDateTime, Duration, String> formattingFunction) {
        this.formattingFunction = formattingFunction;
    }

    public String getDateTime(LocalDateTime localDateTime, Double duration) {
        return localDateTime.toString();
    }

    public String getDate(LocalDateTime localDateTime, Duration duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        return formatter.format(localDateTime);
    }

    public String getTime(LocalDateTime localDateTime, Duration duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
        return formatter.format(localDateTime);
    }

    public String switchFormat(LocalDateTime localDateTime, Duration duration) {
        ITimeUnit tu = timeUnit;
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

