package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MonthTimeUnit implements ITimeUnit {
    private final static long differenceInMillis = 2678400000L;
    // TODO: To implement
    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return null;
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusMonths((long) delta);
        } else {
            return value.plus((long) (MonthTimeUnit.differenceInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return null;
    }

    @Override
    public String getUnitLabel() {
        return "mth";
    }
}
