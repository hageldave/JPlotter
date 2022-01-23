package hageldave.jplotter.coordsys.timelabels.units;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DayTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 86400000;

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
    public String getUnitLabel() {
        return "d";
    }
}
