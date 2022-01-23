package hageldave.jplotter.coordsys.timelabels.units;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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

    @Override
    public String getUnitLabel() {
        return "ms";
    }
}
