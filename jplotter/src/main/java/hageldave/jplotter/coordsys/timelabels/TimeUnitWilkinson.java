package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.DefaultUnitSwitchConstants;
import hageldave.jplotter.coordsys.timelabels.units.ITimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.IUnitSwitchConstants;
import hageldave.jplotter.coordsys.timelabels.units.TimeUnit;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The TimePassedWilkinson is an extension of the {@link ExtendedWilkinson} tick labeling mechanism.
 * It uses the ExtendedWilkinson mechanism to calculate the positioning of the tick labels which are then converted to
 * {@link LocalDateTime} units.
 * It calculates those label values by using a {@link TimeUnit}. Then each tick label value is used as a {@link TimeUnit} value.
 * The first value on the left border is 0[TimeUnit] (e.g. 0 weeks), which is then increased (e.g. 1 week, 2 weeks, ...) by going further to the right.
 */
public class TimeUnitWilkinson extends ExtendedWilkinson {

    protected TimeUnit timeUnit;
    protected IUnitSwitchConstants unitSwitchConstants;

    /**
     * Creates an instance of the TimePassedWilkinson.
     *
     * @param timeUnit to use for calculating and displaying the tick labels
     * @param unitSwitchConstants used to determine when to switch time units
     */
    public TimeUnitWilkinson(final TimeUnit timeUnit, final IUnitSwitchConstants unitSwitchConstants) {
        this.timeUnit = timeUnit;
        this.unitSwitchConstants = unitSwitchConstants;
    }

    public TimeUnitWilkinson(final TimeUnit timeUnit) {
        this(timeUnit, new DefaultUnitSwitchConstants());
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
