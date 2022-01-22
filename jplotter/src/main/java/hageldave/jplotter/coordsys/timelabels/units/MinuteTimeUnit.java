package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class MinuteTimeUnit implements ITimeUnit {

    private final static long differenceInMillis = 60000;

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return LocalDateTime.of(value.getYear(), value.getMonth(), value.getDayOfMonth(), value.getHour(), value.getMinute());
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, double delta) {
        if (delta % 1 == 0) {
            return value.plusMinutes((long) delta);
        } else {

            // old approach - now hardcoded as diff in millis never changes
            /*Instant instant = value.toInstant(ZoneOffset.UTC);
            long millisSince1970 = instant.toEpochMilli();

            LocalDateTime nextMinute = value.plusMinutes(1);
            instant = nextMinute.toInstant(ZoneOffset.UTC);
            long millisPlusMinute = instant.toEpochMilli();

            long differenceInMillis = millisPlusMinute - millisSince1970;

            differenceInMillis *= delta;*/

            return value.plus((long) (MinuteTimeUnit.differenceInMillis * delta), ChronoUnit.MILLIS);
        }
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        return floor(value).toString();
    }

    @Override
    public String getUnitLabel() {
        return "m";
    }
}