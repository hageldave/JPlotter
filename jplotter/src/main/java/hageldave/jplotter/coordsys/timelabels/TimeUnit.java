package hageldave.jplotter.coordsys.timelabels;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import hageldave.jplotter.coordsys.timelabels.units.DayTimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.HourTimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.ITimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.MilliTimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.MinuteTimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.MonthTimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.SecondTimeUnit;
import hageldave.jplotter.coordsys.timelabels.units.UnitSwitchConstants;
import hageldave.jplotter.coordsys.timelabels.units.YearTimeUnit;
import hageldave.jplotter.util.Pair;

public enum TimeUnit implements ITimeUnit {
	
    Year(new YearTimeUnit()),
    Month(new MonthTimeUnit()),
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

