package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Time unit representing a week.
 */
class WeekTimeUnit implements ITimeUnit {
    public final static long durationInMillis = 604800000L;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.WEEKS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusWeeks((long) delta);
        } else {
            return value.plus((long) (WeekTimeUnit.durationInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, UnitSwitchConstants switchConstants) {
        double difference = ticks[1]-ticks[0];
        double[] convertedTicks = new double[ticks.length];
        String unitLabel;

        if (difference > switchConstants.week_up) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]/52.0;
            timeUnit = new YearTimeUnit();
            multiplier.set(multiplier.get()*52);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier, switchConstants);
            return new Pair<>(convertedTickPair.first, convertedTickPair.second);

        } else if (difference < switchConstants.week_down) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]*7.0;
            timeUnit = new DayTimeUnit();
            multiplier.set(multiplier.get()/7.0);
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
        return "wk";
    }
}
