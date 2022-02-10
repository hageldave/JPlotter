package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public interface ITimeUnit {

    LocalDateTime floor(LocalDateTime value);

    LocalDateTime increment(LocalDateTime value, double delta);

    Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, UnitSwitchConstants switchConstants);

    String getUnitLabel();
}
