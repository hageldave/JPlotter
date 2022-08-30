package hageldave.jplotter.debugging.customPrint;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public class RenderableDetailsPrinter implements CustomPrinterInterface {
    String[] colorVarNames = {"color0", "color1", "c0", "c1", "c2", "color"};

    @Override
    public String print(Object object, String variableName) {
        if (Point2D.class.isAssignableFrom(object.getClass())) {
            Point2D pointObject = (Point2D) object;
            return "XY: (" + pointObject.getX() + ", " + pointObject.getY() + ")";
        } else if (IntSupplier.class.isAssignableFrom(object.getClass()) && Arrays.asList(colorVarNames).contains(variableName)) {
            IntSupplier colorSupplier = (IntSupplier) object;
            Color color = new Color(colorSupplier.getAsInt());
            return "RGBA: (" + color.getRed() + ", " + color.getGreen() + ", " + color.getBlue() + ", " + color.getAlpha() + ")";
        } else if (DoubleSupplier.class.isAssignableFrom(object.getClass())) {
            DoubleSupplier thicknessSupplier = (DoubleSupplier) object;
            return String.valueOf(thicknessSupplier.getAsDouble());
        }
        return object.toString();
    }
}
