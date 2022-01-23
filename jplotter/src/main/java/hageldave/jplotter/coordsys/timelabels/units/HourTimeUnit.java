package hageldave.jplotter.coordsys.timelabels.units;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class HourTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 3600000;

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

    @Override
    public String getUnitLabel() {
        return "h";
    }
}
