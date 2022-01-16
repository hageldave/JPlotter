package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.*;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeWilkinson extends ExtendedWilkinson {

    protected String[] labelsForTicks(double min, double[] ticks, ITimeUnit timeUnit, double number2unit, LocalDateTime referenceDateTime, DateTimeFormatter dateTimeFormatter) {
        String[] labels = new String[ticks.length];

        for (int i=0; i<ticks.length; i++) {
            double difference = ticks[i] - min;
            difference *= number2unit;
            LocalDateTime ldt = timeUnit.increment(referenceDateTime, (int) difference);
            labels[i] = ldt.format(dateTimeFormatter);
        }

        return labels;
    }

    public Pair<double[], String[]> genTicksAndLabels(double min, double max, TimeUnit timeUnit, double number2unit, LocalDateTime referenceDateTime, int desiredNumTicks, DateTimeFormatter dateTimeFormatter/*, boolean verticalAxis*/) {
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

        String[] labelsForTicks = labelsForTicks(min, ticks, tu, number2unit, referenceDateTime, dateTimeFormatter);
        return new Pair<>(ticks, labelsForTicks);
    }

    /**
     *
     * @param min
     * @param max
     * @param number2unit - how much should the time/date whatever be incremented with each step: e.g. if
     *                    the ticks go from 0 to 30, does this mean each int number 0,1,2,3,... means one minute or half-minute steps...
     * @param timeUnit
     * @param referenceDateTime
     * @param desiredNumTicks
     * @return
     */
    public Pair<double[], String[]> genTicksAndLabels(double min, double max, TimeUnit timeUnit, double number2unit, LocalDateTime referenceDateTime, int desiredNumTicks/*, boolean verticalAxis*/) {
        return genTicksAndLabels(min, max, timeUnit, number2unit, referenceDateTime, desiredNumTicks, DateTimeFormatter.ISO_DATE_TIME);
    }
}

