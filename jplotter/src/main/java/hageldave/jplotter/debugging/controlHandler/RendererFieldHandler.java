package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.GenericRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.DoubleSupplier;

public class RendererFieldHandler {
    public static JPanel handleRendererField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.LINE_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(4, 0, 4, 0));
        Object fieldValue = field.get(obj);

        if (GenericRenderer.class.isAssignableFrom(obj.getClass())) {
            return handleGenericRendererSub(canvas, obj, field, fieldValue, labelContainer);
        }
        return handleRendererSub(canvas, obj, field, fieldValue, labelContainer);
    }

    protected static JPanel handleGenericRendererSub(JPlotterCanvas canvas, Object obj, Field field, Object fieldValue, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (field.getName().equals("isEnabled")) {
            Class<?> rendererClass = obj.getClass();
            Method isEnabled = rendererClass.getSuperclass().getDeclaredMethod("isEnabled");
            Method setEnabled = rendererClass.getSuperclass().getDeclaredMethod("setEnabled", boolean.class);

            labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
            labelContainer.add(new JLabel((field.getName()) + ": "));

            JLabel fieldValLabel = new JLabel(String.valueOf(isEnabled.invoke(obj)));
            fieldValLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    try {
                        changeValue(obj, setEnabled, isEnabled);
                        fieldValLabel.setText(String.valueOf(isEnabled.invoke(obj)));
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                    canvas.scheduleRepaint();
                }
            });
            labelContainer.add(fieldValLabel);
        } else {
            labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
            labelContainer.add(new JLabel((field.getName()) + ": "));
            if (DoubleSupplier.class.isAssignableFrom(field.getType())) {
                DoubleSupplier dSup = (DoubleSupplier) fieldValue;
                labelContainer.add(new JLabel(String.valueOf(dSup.getAsDouble())));
            } else {
                labelContainer.add(new JLabel(String.valueOf(fieldValue)));
            }
        }
        return labelContainer;
    }

    protected static JPanel handleRendererSub(JPlotterCanvas canvas, Object obj, Field field, Object fieldValue, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (field.getName().equals("isEnabled")) {
            Class<?> rendererClass = obj.getClass();
            Method isEnabled = rendererClass.getDeclaredMethod("isEnabled");
            Method setEnabled = rendererClass.getDeclaredMethod("setEnabled", boolean.class);

            labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
            labelContainer.add(new JLabel((field.getName()) + ": "));

            JLabel fieldValLabel = new JLabel(String.valueOf(isEnabled.invoke(obj)));
            fieldValLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    try {
                        changeValue(obj, setEnabled, isEnabled);
                        fieldValLabel.setText(String.valueOf(isEnabled.invoke(obj)));
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        ex.printStackTrace();
                    }
                    canvas.scheduleRepaint();
                }
            });
            labelContainer.add(fieldValLabel);
        } else {
            labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
            labelContainer.add(new JLabel((field.getName()) + ": "));
            if (DoubleSupplier.class.isAssignableFrom(field.getType())) {
                DoubleSupplier dSup = (DoubleSupplier) fieldValue;
                labelContainer.add(new JLabel(String.valueOf(dSup.getAsDouble())));
            } else {
                labelContainer.add(new JLabel(String.valueOf(fieldValue)));
            }
        }
        return labelContainer;
    }

    protected static void changeValue(Object obj, Method setEnabled, Method isEnabled) throws InvocationTargetException, IllegalAccessException {
        boolean invoked = (boolean) isEnabled.invoke(obj);
        setEnabled.invoke(obj, !invoked);
    }
}
