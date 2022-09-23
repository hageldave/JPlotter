package hageldave.jplotter.coordsys.timelabels.units;

/**
 * Contains constants when to switch the TimeUnit.
 * Those can be used when the time difference between two ticks is too small/big and another unit is necessary.
 */
public class UnitSwitchConstants {
    public double millies_up = 10000;

    public double seconds_up = 600;
    public double seconds_down = 0.05;

    public double minutes_up = 500;
    public double minutes_down = 0.05;

    public double hours_up = 100;
    public double hours_down = 0.05;

    public double days_up = 120;
    public double days_down = 0.05;

    public double week_up = 100;
    public double week_down = 0.05;

    public double months_up = 48;
    public double months_down = 0.05;

    public double years_down = 0.05;
}
