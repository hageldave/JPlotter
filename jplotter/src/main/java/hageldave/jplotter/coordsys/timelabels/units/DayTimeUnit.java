package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DayTimeUnit implements ITimeUnit {
    public final static long differenceInMillis = 86400000;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.DAYS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusDays((long) delta);
        } else {
            return value.plus((long) (DayTimeUnit.differenceInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, UnitSwitchConstants switchConstants) {
        double difference = ticks[1]-ticks[0];
        double[] convertedTicks = new double[ticks.length];
        String unitLabel;

        if (difference > switchConstants.days_up) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]/30.0;
            timeUnit = new MonthTimeUnit();
            multiplier.set(multiplier.get()*30.0);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier, switchConstants);
            return new Pair<>(convertedTickPair.first, convertedTickPair.second);

        } else if (difference < switchConstants.days_down) {
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
