package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

public class HourTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 3600000;

    private final static int upperDifferenceLimit = 100;
    private final static double lowerDifferenceLimit = 0.05;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.HOURS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusHours((long) delta);
        } else {
            return value.plus((long) (HourTimeUnit.differenceInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier) {
        double difference = ticks[1]-ticks[0];
        double[] convertedTicks = new double[ticks.length];
        String unitLabel;

        if (difference > upperDifferenceLimit) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]/24.0;
            timeUnit = new DayTimeUnit();
            multiplier.set(multiplier.get()*24.0);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier);
            return new Pair<>(convertedTickPair.first, convertedTickPair.second);

        } else if (difference < lowerDifferenceLimit) {
            for (int i = 0; i < ticks.length; i++)
                convertedTicks[i] = ticks[i]*60.0;
            timeUnit = new MinuteTimeUnit();
            multiplier.set(multiplier.get()/60.0);
            Pair<double[], String> convertedTickPair = timeUnit.convertTicks(timeUnit, convertedTicks, multiplier);
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
