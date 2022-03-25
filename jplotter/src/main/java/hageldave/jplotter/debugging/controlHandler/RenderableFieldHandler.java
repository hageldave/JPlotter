package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.function.DoubleSupplier;

public class RenderableFieldHandler {
    public static JPanel handleRenderableField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.LINE_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        Object fieldValue = field.get(obj);

        if (fieldValue != null) {
            labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
            labelContainer.add(new JLabel((field.getName()) + ": "));
            if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                DoubleSupplier dSup = (DoubleSupplier) fieldValue;
                labelContainer.add(new JLabel(String.valueOf(dSup.getAsDouble())));
            } else {
                labelContainer.add(new JLabel(String.valueOf(fieldValue)));
            }
        }

        return labelContainer;
    }
}
