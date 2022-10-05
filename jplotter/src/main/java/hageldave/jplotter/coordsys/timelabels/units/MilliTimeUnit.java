package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.unitswitch.Direction;
import hageldave.jplotter.coordsys.timelabels.unitswitch.IUnitSwitchConstants;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Time unit representing a milli.
 */
class MilliTimeUnit implements ITimeUnit {

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.MILLIS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        long milli2nano = (long) (delta * 1000000);

        return value.plusNanos(milli2nano);
    }

    @Override
    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, IUnitSwitchConstants switchConstants) {
        double difference = ticks[1]-ticks[0];
        double[] convertedTicks = new double[ticks.length];
        String unitLabel;

        if (difference > switchConstants.getMillisChangePoint(Direction.UP)) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]/1000.0;
            timeUnit = new SecondTimeUnit();
            multiplier.set(multiplier.get()*1000.0);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier, switchConstants);
            return new Pair<>(convertedTickPair.first, convertedTickPair.second);
        } else {
            convertedTicks = ticks;
            unitLabel = timeUnit.getUnitLabel();
        }

        return new Pair<>(convertedTicks, unitLabel);
    }

    @Override
    public String getUnitLabel() {
        return "ms";
    }
}
