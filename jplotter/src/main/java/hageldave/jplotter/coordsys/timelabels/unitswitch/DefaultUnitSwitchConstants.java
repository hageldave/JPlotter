package hageldave.jplotter.coordsys.timelabels.unitswitch;

import java.util.Objects;

/**
 * Contains constants when to switch the TimeUnit.
 * Those can be used when the time difference between two ticks is too small/big and another unit is necessary.
 */
public class DefaultUnitSwitchConstants implements IUnitSwitchConstants {
    private static DefaultUnitSwitchConstants instance = null;

    DefaultUnitSwitchConstants() {}

    @Override
    public double getMillisChangePoint(Direction dir) {
        if (dir == Direction.UP) {
            return 10000;
        }
        return -1;
    }

    @Override
    public double getSecondsChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return 600;
    }

    @Override
    public double getMinutesChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return 500;
    }

    @Override
    public double getHoursChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return 100;
    }

    @Override
    public double getDaysChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return 120;
    }

    @Override
    public double getWeekChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return 100;
    }

    @Override
    public double getMonthsChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return 48;
    }

    @Override
    public double getYearsChangePoint(Direction dir) {
        if (dir == Direction.DOWN) {
            return 0.05;
        }
        return -1;
    }

    /**
     * Returns the instance of the DefaultUnitSwitchConstants class and creates one if it doesn't exist already.
     * It uses the singleton pattern, so this instance is the only one of this class.
     *
     * @return the instance of the DefaultUnitSwitchConstants class
     */
    public static DefaultUnitSwitchConstants getInstance() {
        if (Objects.isNull(instance)) {
            instance = new DefaultUnitSwitchConstants();
        }
        return instance;
    }
}
