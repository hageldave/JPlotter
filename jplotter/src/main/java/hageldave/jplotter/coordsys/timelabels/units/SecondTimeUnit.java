package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SecondTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 1000;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.SECONDS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusSeconds((long) delta);
        } else {
            return value.plus((long) (SecondTimeUnit.differenceInMillis * delta), ChronoUnit.MILLIS);
        }

    }


    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier) {
        timeUnit = new SecondTimeUnit();
        return new Pair<>(ticks, timeUnit.getUnitLabel());
    }

    @Override
    public String getUnitLabel() {
        return "s";
    }
}
