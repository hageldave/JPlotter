package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;

public interface ITimeUnit {

    LocalDateTime floor(LocalDateTime value);

    LocalDateTime increment(LocalDateTime value, int delta);

    // TODO: probably unnecessary
    String getLabel(LocalDateTime value, DateStyle dateType);
}
