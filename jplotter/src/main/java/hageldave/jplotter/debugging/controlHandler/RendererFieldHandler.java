package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public class RendererFieldHandler {
    final static String[] toControl = new String[]{"isEnabled", "paddingLeft", "paddingRight", "paddingTop", "paddingBot", "legendRightWidth", "legendBottomHeight", "guideColor", "tickColor", "textColor"};
    final static String[] toDisplay = new String[]{"strokePattern", "view", "itemToRender", "isGLDoublePrecisionEnabled", "orthoMX", "coordsysAreaRT", "coordsysAreaRB", "coordsysAreaLT", "coordsysAreaLB", "currentViewPort", "tickMarkLabels", "tickMarkGenerator", "colorScheme"};

    public static JPanel handleRendererField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.LINE_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(4, 0, 4, 0));
        Object fieldValue = field.get(obj);

        labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
        JLabel fieldName = new JLabel((field.getName()) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        labelContainer.add(fieldName);

        if (field.getName().equals("isEnabled")) {
            createIsEnabledUIElements(canvas, obj, labelContainer);
        } else {
            if (Objects.nonNull(fieldValue)) {
                if (DoubleSupplier.class.isAssignableFrom(field.getType())) {
                    labelContainer.add(new JLabel(String.valueOf(((DoubleSupplier) fieldValue).getAsDouble())));
                } else if (IntSupplier.class.isAssignableFrom(field.getType())) {
                    labelContainer.add(new JLabel(String.valueOf(((IntSupplier) fieldValue).getAsInt())));
                } else if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    labelContainer.add(new JLabel(((Collection<?>) fieldValue).size() + " element(s)"));
                } else {
                    labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            } else {
                labelContainer.add(new JLabel("null"));
            }
        }
        return labelContainer;
    }

    protected static JPanel createIsEnabledUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method isEnabled = rendererClass.getMethod("isEnabled");
        Method setEnabled = rendererClass.getMethod("setEnabled", boolean.class);

        JLabel fieldValLabel = new JLabel(String.valueOf(isEnabled.invoke(obj)));
        fieldValLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    boolean invoked = (boolean) isEnabled.invoke(obj);
                    setEnabled.invoke(obj, !invoked);

                    fieldValLabel.setText(String.valueOf(isEnabled.invoke(obj)));
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
                canvas.scheduleRepaint();
            }
        });
        labelContainer.add(fieldValLabel);
        return labelContainer;
    }

    public static boolean displayInControlArea(String fieldName) {
        return Arrays.asList(toControl).contains(fieldName);
    }

    public static boolean displayInInformationArea(String fieldName) {
        return Arrays.asList(toDisplay).contains(fieldName);
    }
}
