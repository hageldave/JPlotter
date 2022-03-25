package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.GenericRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.function.DoubleSupplier;

public class RendererHandler {
    public static JPanel handleRendererField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException {
        // TODO:
        if (GenericRenderer.class.isAssignableFrom(obj.getClass())) {
            return handleGenericRendererSub(canvas, obj, field);
        }
        return new JPanel();
    }

    protected static JPanel handleGenericRendererSub(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.LINE_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        Object fieldValue = field.get(obj);

        if (field.getName().equals("isEnabled")) {

            GenericRenderer<?> castedObj = (GenericRenderer<?>) obj;

            labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
            labelContainer.add(new JLabel((field.getName()) + ": "));

            JLabel fieldValLabel = new JLabel(String.valueOf(castedObj.isEnabled()));
            fieldValLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);

                    changeValue(castedObj);
                    fieldValLabel.setText(String.valueOf(castedObj.isEnabled()));
                    canvas.scheduleRepaint();
                }
            });
            labelContainer.add(fieldValLabel);
        } else {
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
        }
        return labelContainer;
    }

    protected static void changeValue(GenericRenderer<?> toChange) {
        toChange.setEnabled(!toChange.isEnabled());
    }
}
