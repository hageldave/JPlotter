package hageldave.jplotter.coordsys.timelabels.units;

import hageldave.jplotter.coordsys.timelabels.DateStyle;

import java.time.LocalDateTime;

public class MinuteTimeUnit implements ITimeUnit {

    @Override
    public LocalDateTime floor(LocalDateTime value) {
        return LocalDateTime.of(value.getYear(), value.getMonth(), value.getDayOfMonth(), value.getHour(), value.getMinute());
    }

    @Override
    public LocalDateTime increment(LocalDateTime value, int delta) {
        return value.plusMinutes(delta);
    }

    @Override
    public String getLabel(LocalDateTime value, DateStyle dateType) {
        StringBuilder strB = new StringBuilder();

        /*switch (dateType) {
            case DATE:
                strB.append(value.getMonthValue()).append(value.getDayOfMonth()).append(value.getYear());
                return strB.toString();
            case TIME:
                strB.append(value.getHour()).append(":").append(value.getMinute());
                return strB.toString();
            case TIME_DATE:
                strB.append(value.getMonthValue()).append(value.getDayOfMonth()).append(value.getYear());
                strB.append("---");
                strB.append("\n");
                strB.append(value.getHour()).append(":").append(value.getMinute());
                return strB.toString();
            default:
                return floor(value).toString();
        }*/
        return floor(value).toString();
    }
}
