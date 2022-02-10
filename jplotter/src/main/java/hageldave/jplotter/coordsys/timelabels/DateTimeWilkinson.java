package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.ITimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.TimeUnit;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.BiFunction;

public class DateTimeWilkinson extends ExtendedWilkinson {
    protected TimeUnit timeUnit;
    protected double number2unit;
    protected LocalDateTime referenceDateTime;
    protected BiFunction<LocalDateTime, Double, String> formattingFunction;

    public DateTimeWilkinson(final TimeUnit timeUnit, final double number2unit, final LocalDateTime referenceDateTime, final BiFunction<LocalDateTime, Double, String> formattingFunction) {
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

    protected String[] labelsForTicks(double min, double[] ticks, ITimeUnit timeUnit) {
        String[] labels = new String[ticks.length];

        double tickDiff = ticks[ticks.length-1] - ticks[ticks.length-2];
        for (int i=0; i<ticks.length; i++) {
            double difference = ticks[i] - min;
            difference *= number2unit;
            LocalDateTime ldt = timeUnit.increment(referenceDateTime, difference);
            labels[i] = formattingFunction.apply(ldt, tickDiff);
        }

        return labels;
    }

    public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis) {
        double[] ticks = getTicks(min, max, desiredNumTicks, super.Q, super.w);
        String[] labelsForTicks = labelsForTicks(min, ticks, timeUnit);
        return new Pair<>(ticks, labelsForTicks);
    }


    public BiFunction<LocalDateTime, Double, String> getFormattingFunction() {
        return formattingFunction;
    }

    public void setFormattingFunction(BiFunction<LocalDateTime, Double, String> formattingFunction) {
        this.formattingFunction = formattingFunction;
    }

    public String getDateTime(LocalDateTime localDateTime, Double duration) {
        return localDateTime.toString();
    }

    public String getDate(LocalDateTime localDateTime, Double duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE;
        return formatter.format(localDateTime);
    }

    public String getTime(LocalDateTime localDateTime, Double duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_TIME;
        return formatter.format(localDateTime);
    }

    public String switchFormat(LocalDateTime localDateTime, Double duration) {
        ITimeUnit tu = timeUnit;
        LocalDateTime LocalDateTimeCopy = LocalDateTime.from(localDateTime);
        LocalDateTime ldt = tu.increment(LocalDateTimeCopy, duration);

        long millisCurrentTick = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
        long millisNextTick = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        long differenceInMillis = millisNextTick - millisCurrentTick;

        DateTimeFormatter formatter;
        // Duration of 1 day in millis
        if (differenceInMillis > 86400000) {
            formatter = DateTimeFormatter.ISO_DATE;
        } else {
            formatter = DateTimeFormatter.ISO_TIME;
        }
        return formatter.format(localDateTime);
    }
}

