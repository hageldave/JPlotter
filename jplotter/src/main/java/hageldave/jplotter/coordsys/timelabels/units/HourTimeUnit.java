package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class HourTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 3600000;
    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return LocalDateTime.of(value.getYear(), value.getMonth(), value.getDayOfMonth(), value.getHour(), 0);
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
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return null;
    }

    @Override
    public String getUnitLabel() {
        return "h";
    }
}
