package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;

public class HourTimeUnit implements ITimeUnit {
    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return LocalDateTime.of(value.getYear(), value.getMonth(), value.getDayOfMonth(), value.getHour(), 0);
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, int delta) {
        return value.plusHours(delta);
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return null;
    }
}
