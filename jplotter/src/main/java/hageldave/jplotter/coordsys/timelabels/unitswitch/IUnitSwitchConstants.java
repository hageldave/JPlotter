package hageldave.jplotter.coordsys.timelabels.unitswitch;

/**
 * Interface that (when implemented) defines when to switch time units.
 */
public interface IUnitSwitchConstants {

    /**
     * @param dir to which timeUnit the millis should be switched (UP/DOWN)
     * @return distance between ticks when to switch from millis to next timeUnit
     */
    public double getMillisChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the seconds should be switched (UP/DOWN)
     * @return distance between ticks when to switch from seconds to next timeUnit
     */
    public double getSecondsChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the minutes should be switched (UP/DOWN)
     * @return distance between ticks when to switch from minutes to next timeUnit
     */
    public double getMinutesChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the millis should be switched (UP/DOWN)
     * @return distance between ticks when to switch from millis to next timeUnit
     */
    public double getHoursChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the days should be switched (UP/DOWN)
     * @return distance between ticks when to switch from days to next timeUnit
     */
    public double getDaysChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the weeks should be switched (UP/DOWN)
     * @return distance between ticks when to switch from weeks to next timeUnit
     */
    public double getWeekChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the months should be switched (UP/DOWN)
     * @return distance between ticks when to switch from months to next timeUnit
     */
    public double getMonthsChangePoint(Direction dir);

    /**
     * @param dir to which timeUnit the years should be switched (UP/DOWN)
     * @return distance between ticks when to switch from years to next timeUnit
     */
    public double getYearsChangePoint(Direction dir);
}
