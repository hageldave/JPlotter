package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.TimeUnit;
import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public interface ITimeUnit {

    LocalDateTime floor(LocalDateTime value);

    LocalDateTime increment(LocalDateTime value, double delta);

    Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier, UnitSwitchConstants switchConstants);

    String getUnitLabel();

    static ITimeUnit getInterface(TimeUnit timeUnit) {
        switch (timeUnit) {
            case Year:
                return new YearTimeUnit();
            case Month:
                return new MonthTimeUnit();
            case Day:
                return new DayTimeUnit();
            case Hour:
                return new HourTimeUnit();
            case Minute:
                return new MinuteTimeUnit();
            case Second:
                return new SecondTimeUnit();
            default:
                return new MilliTimeUnit();
        }
    }
}
