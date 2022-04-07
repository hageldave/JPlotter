package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.ITimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.TimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.UnitSwitchConstants;
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

    protected Pair<double[], String[]> labelsForConvertedTicks(double[] ticks, ITimeUnit timeUnit, int desiredNumTicks) {
        AtomicReference<Double> multiplier = new AtomicReference<>(1.0);
        Pair<double[], String> convertedTicks = timeUnit.convertTicks(timeUnit, ticks, multiplier, unitSwitchConstants);

        // after the ticks are converted we want "nice" labels again
        ticks = getTicks(convertedTicks.first[0], convertedTicks.first[convertedTicks.first.length-1], desiredNumTicks, super.Q, super.w);

        double[] ticksForLabels = Arrays.copyOf(ticks, ticks.length);
        for (int j = 0; j < ticks.length; j++)
            ticks[j] *= multiplier.get();

        String[] labels = super.labelsForTicks(ticksForLabels);

        for (int i = 0; i < ticks.length; i++)
            labels[i] = labels[i] + convertedTicks.second;

        return new Pair<>(ticks, labels);
    }

    @Override
    public Pair<double[], String[]> genTicksAndLabels(double min, double max, int desiredNumTicks, boolean verticalAxis) {
        double[] ticks = getTicks(min, max, desiredNumTicks, super.Q, super.w);
        Pair<double[], String[]> generatedTicksAndLabels = labelsForConvertedTicks(ticks, timeUnit, desiredNumTicks);
        return new Pair<>(generatedTicksAndLabels.first, generatedTicksAndLabels.second);
    }
}