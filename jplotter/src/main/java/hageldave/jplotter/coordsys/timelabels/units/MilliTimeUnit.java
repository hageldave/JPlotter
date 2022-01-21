package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;

public class MilliTimeUnit implements ITimeUnit {
    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return null;
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {

        long milli2nano = (long) (delta * 1000000);

        return value.plusNanos(milli2nano);
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return null;
    }
}
