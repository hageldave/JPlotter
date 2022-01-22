package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class YearTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 31536000000L;
    // TODO: To implement
    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return null;
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusYears((long) delta);
        } else {
            return value.plus((long) (YearTimeUnit.differenceInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return null;
    }

    @Override
    public String getUnitLabel() {
        return "y";
    }
}
