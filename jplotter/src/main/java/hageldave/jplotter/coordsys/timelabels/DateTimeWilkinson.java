package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.*;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeWilkinson extends ExtendedWilkinson {

    protected TimeUnit timeUnit;
    protected double number2unit;
    protected LocalDateTime referenceDateTime;
    protected DateTimeFormatter dateTimeFormatter;

    public DateTimeWilkinson(final TimeUnit timeUnit, final double number2unit, final LocalDateTime referenceDateTime, final DateTimeFormatter dateTimeFormatter) {
        this.timeUnit = timeUnit;
        this.number2unit = number2unit;
        this.referenceDateTime = referenceDateTime;
        this.dateTimeFormatter = dateTimeFormatter;
    }

    protected String[] labelsForTicks(double min, double[] ticks, ITimeUnit timeUnit) {
        String[] labels = new String[ticks.length];

        for (int i=0; i<ticks.length; i++) {
            double difference = ticks[i] - min;
            difference *= number2unit;
            LocalDateTime ldt = timeUnit.increment(referenceDateTime, difference);
            labels[i] = ldt.format(dateTimeFormatter);
        }

        return labels;
    }

    public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis) {
        double[] ticks = getTicks(min, max, desiredNumTicks, Q, w);

        ITimeUnit tu;
        switch (timeUnit) {
            case Year:
                tu = new YearTimeUnit();
                break;
            case Month:
                tu = new MonthTimeUnit();
                break;
            case Day:
                tu = new DayTimeUnit();
                break;
            case Hour:
                tu = new HourTimeUnit();
                break;
            case Minute:
                tu = new MinuteTimeUnit();
                break;
            case Second:
                tu = new SecondTimeUnit();
                break;
            case Millisecond:
                tu = new MilliTimeUnit();
                break;
            default:
                tu = null;
        }

        String[] labelsForTicks = labelsForTicks(min, ticks, tu);
        return new Pair<>(ticks, labelsForTicks);
    }
}

