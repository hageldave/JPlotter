package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MilliTimeUnit implements ITimeUnit {

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.MILLIS);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        long milli2nano = (long) (delta * 1000000);

        return value.plusNanos(milli2nano);
    }

    public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier) {
        return new Pair<>(new double[]{}, "");
    }

    @Override
    public String getUnitLabel() {
        return "ms";
    }
}
