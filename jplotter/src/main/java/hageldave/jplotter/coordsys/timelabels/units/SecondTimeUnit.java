package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;

public class SecondTimeUnit implements ITimeUnit {
    // TODO: To implement
    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return null;
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, int delta) {
        return value.plusSeconds(delta);
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return null;
    }
}
