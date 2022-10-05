package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.unitswitch.Direction;
import hageldave.jplotter.coordsys.timelabels.unitswitch.IUnitSwitchConstants;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Time unit representing a day.
 */
class DayTimeUnit implements ITimeUnit {
    public final static long durationInMillis = 86400000;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.DAYS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusDays((long) delta);
        } else {
            return value.plus((long) (DayTimeUnit.durationInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, IUnitSwitchConstants switchConstants) {
        double difference = ticks[1]-ticks[0];
        double[] convertedTicks = new double[ticks.length];
        String unitLabel;

        if (difference > switchConstants.getDaysChangePoint(Direction.UP)) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]/7.0;
            timeUnit = new WeekTimeUnit();
            multiplier.set(multiplier.get()*7.0);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier, switchConstants);
            return new Pair<>(convertedTickPair.first, convertedTickPair.second);

        } else if (difference < switchConstants.getDaysChangePoint(Direction.DOWN)) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]*24.0;
            timeUnit = new HourTimeUnit();
            multiplier.set(multiplier.get()/24.0);
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
        return "d";
    }
}
