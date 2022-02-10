package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.util.Pair;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public enum TimeUnit implements ITimeUnit {
	
    Year(new YearTimeUnit()),
    Month(new MonthTimeUnit()),
    Week(new WeekTimeUnit()),
    Day(new DayTimeUnit()),
    Hour(new HourTimeUnit()),
    Minute(new MinuteTimeUnit()),
    Second(new SecondTimeUnit()),
    Millisecond(new MilliTimeUnit())
    ;
	
	ITimeUnit delegate;
	
	private TimeUnit(ITimeUnit proxy) {
		this.delegate=proxy;
	}

	@Override
	public LocalDateTime floor(LocalDateTime value) {
		return this.delegate.floor(value);
	}

	@Override
	public LocalDateTime increment(LocalDateTime value, double delta) {
		return this.delegate.increment(value, delta);
	}

	@Override
	public Pair<double[], String> convertTicks(ITimeUnit timeUnit, double[] ticks, AtomicReference<Double> multiplier,
			UnitSwitchConstants switchConstants) {
		return this.delegate.convertTicks(timeUnit, ticks, multiplier, switchConstants);
	}

	@Override
	public String getUnitLabel() {
		return this.delegate.getUnitLabel();
	}
	
	
}

