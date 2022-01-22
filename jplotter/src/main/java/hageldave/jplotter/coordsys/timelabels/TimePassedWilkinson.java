package hageldave.jplotter.coordsys.timelabels;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.timelabels.units.*;
import hageldave.jplotter.util.Pair;

public class TimePassedWilkinson extends ExtendedWilkinson {

    protected String[] labelsForTicks(double[] ticks, ITimeUnit timeUnit) {
        String[] labels = super.labelsForTicks(ticks);

        // add time unit labels to the generated ticklabels
        for (int i = 0; i < labels.length; i++)
            labels[i] = labels[i] + timeUnit.getUnitLabel();

        return labels;
    }

    public Pair<double[], String[]> genTicksAndLabels(double min, double max, TimeUnit timeUnit, int desiredNumTicks/*, boolean verticalAxis*/) {
        double[] ticks = getTicks(min, max, desiredNumTicks, Q, w);

        ITimeUnit tu;
        switch (timeUnit) {
            case Year:
                tu = new YearTimeUnit();
                break;
            case Month:
                tu = new MonthTimeUnit();
                break;
            case Day:
                tu = new DayTimeUnit();
                break;
            case Hour:
                tu = new HourTimeUnit();
                break;
            case Minute:
                tu = new MinuteTimeUnit();
                break;
            case Second:
                tu = new SecondTimeUnit();
                break;
            case Millisecond:
                tu = new MilliTimeUnit();
                break;
            default:
                tu = null;
        }

        String[] labelsForTicks = labelsForTicks(ticks, tu);
        return new Pair<>(ticks, labelsForTicks);
    }
}
