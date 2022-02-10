package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

class HourTimeUnit implements ITimeUnit {
    public final static long durationInMillis = 3600000;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.HOURS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusHours((long) delta);
        } else {
            return value.plus((long) (HourTimeUnit.durationInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, UnitSwitchConstants switchConstants) {
        double difference = ticks[1]-ticks[0];
        double[] convertedTicks = new double[ticks.length];
        String unitLabel;

        if (difference > switchConstants.hours_up) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]/24.0;
            timeUnit = new DayTimeUnit();
            multiplier.set(multiplier.get()*24.0);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier, switchConstants);
            return new Pair<>(convertedTickPair.first, convertedTickPair.second);

        } else if (difference < switchConstants.hours_down) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]*60.0;
            timeUnit = new MinuteTimeUnit();
            multiplier.set(multiplier.get()/60.0);
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
        return "h";
    }
}
