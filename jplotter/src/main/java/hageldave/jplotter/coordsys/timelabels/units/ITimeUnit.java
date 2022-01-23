package hageldave.jplotter.coordsys.timelabels.units;

import java.time.LocalDateTime;

public interface ITimeUnit {

    LocalDateTime floor(LocalDateTime value);

    LocalDateTime increment(LocalDateTime value, double delta);

    String getUnitLabel();
}
