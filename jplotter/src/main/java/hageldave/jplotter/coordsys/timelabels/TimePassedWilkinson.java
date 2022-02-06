package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.*;
import hageldave.jplotter.util.Pair;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class TimePassedWilkinson extends ExtendedWilkinson {

    protected TimeUnit timeUnit;
    protected UnitSwitchConstants unitSwitchConstants;

    public TimePassedWilkinson(final TimeUnit timeUnit, final UnitSwitchConstants unitSwitchConstants) {
        this.timeUnit = timeUnit;
        this.unitSwitchConstants = unitSwitchConstants;
    }

    public TimePassedWilkinson(final TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        this.unitSwitchConstants = new UnitSwitchConstants();
    }

    // TODO: rename this
    protected Pair<double[], String[]> labelsForTicks(double[] ticks, ITimeUnit timeUnit, int desiredNumTicks) {
        AtomicReference<Double> multiplier = new AtomicReference<>(1.0);
        Pair<double[], String> convertedStuff = timeUnit.convertTicks(timeUnit, ticks, multiplier, unitSwitchConstants);

        // after the ticks are converted we want "nice" labels again
        ticks = getTicks(convertedStuff.first[0], convertedStuff.first[convertedStuff.first.length-1], desiredNumTicks, Q, w);

        double[] ticksForLabels = Arrays.copyOf(ticks, ticks.length);
        for (int j = 0; j < ticks.length; j++)
            ticks[j] *= multiplier.get();

        String[] labels = new String[ticks.length];
        for (int i = 0; i < ticks.length; i++)
            labels[i] = ticksForLabels[i] + convertedStuff.second;

        return new Pair<>(ticks, labels);
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

        Pair<double[], String[]> generatedTicksAndLabels = labelsForTicks(ticks, tu, desiredNumTicks);
        return new Pair<>(generatedTicksAndLabels.first, generatedTicksAndLabels.second);
    }
}
