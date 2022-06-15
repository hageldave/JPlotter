package hageldave.jplotter.debugging.controlHandler.customPrint;

import java.awt.geom.Point2D;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public class SegmentDetailsPrinter implements CustomPrinterInterface {
    @Override
    public String print(Object object, String variableName) {
        if (Point2D.class.isAssignableFrom(object.getClass()) && (variableName.equals("p0") || variableName.equals("p1"))) {
            return object.toString();
        } else if (IntSupplier.class.isAssignableFrom(object.getClass()) && (variableName.equals("color0") || variableName.equals("color1"))) {
            return object.toString();
        } else if (DoubleSupplier.class.isAssignableFrom(object.getClass()) && (variableName.equals("thickness0") || variableName.equals("thickness1"))) {
            return object.toString();
        } else if (int.class.isAssignableFrom(object.getClass()) && variableName.equals("pickColor")) {
            return object.toString();
        }
        return object.toString();
    }
}
