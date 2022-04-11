package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.DoubleSupplier;

public class RenderableFieldHandler {
    final static String[] toControl = new String[]{"globalThicknessMultiplier", "globalSaturationMultiplier", "globalAlphaMultiplier", "hidden"};

    public static JPanel handleRenderableField(JPlotterCanvas canvas, Object obj, Field field) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new FlowLayout(FlowLayout.LEFT));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        Object fieldValue = field.get(obj);

        labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
        JLabel fieldName = new JLabel((field.getName()) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        labelContainer.add(fieldName);

        switch (field.getName()) {
            case "globalSaturationMultiplier":
                createGlobalMultiplierUIElements(canvas, obj, labelContainer, "getGlobalSaturationMultiplier", "setGlobalSaturationMultiplier");
                break;
            case "globalThicknessMultiplier":
                createGlobalMultiplierUIElements(canvas, obj, labelContainer, "getGlobalThicknessMultiplier", "setGlobalThicknessMultiplier");
                break;
            case "globalAlphaMultiplier":
                createGlobalMultiplierUIElements(canvas, obj, labelContainer, "getGlobalAlphaMultiplier", "setGlobalAlphaMultiplier");
                break;
            case "hidden":
                createHideUIRenderableElements(canvas, obj, labelContainer);
                break;
            default:
                if (fieldValue != null) {
                    if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                        DoubleSupplier dSup = (DoubleSupplier) fieldValue;
                        labelContainer.add(new JLabel(String.valueOf(dSup.getAsDouble())));
                    } else {
                        labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                    }
                }
                break;
        }

        labelContainer.setPreferredSize(new Dimension((int) labelContainer.getPreferredSize().getWidth(), 1));
        return labelContainer;
    }

    protected static JPanel createGlobalMultiplierUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer, String getter, String setter) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setGlobalSaturationMultiplier = renderableClass.getMethod(setter, double.class);
        Method getGlobalSaturationMultiplier = renderableClass.getMethod(getter);

        JLabel valueLabel = new JLabel(getGlobalSaturationMultiplier.invoke(obj).toString());
        float initValue = (Float) getGlobalSaturationMultiplier.invoke(obj)*100;
        JSlider slider = new JSlider(0, 100, (int) initValue);

        slider.addChangeListener(e -> {
            try {
                slider.setValue(slider.getValue());
                setGlobalSaturationMultiplier.invoke(obj, (double) slider.getValue()/100);
                valueLabel.setText(getGlobalSaturationMultiplier.invoke(obj).toString());
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(valueLabel);
        labelContainer.add(slider);
        return labelContainer;
    }

    protected static JPanel createHideUIRenderableElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method hide = renderableClass.getMethod("hide", boolean.class);
        Method isHidden = renderableClass.getMethod("isHidden");

        JLabel fieldValLabel = new JLabel(String.valueOf(isHidden.invoke(obj)));
        fieldValLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    hide.invoke(obj, ! (boolean) (isHidden.invoke(obj)));
                    fieldValLabel.setText(String.valueOf(isHidden.invoke(obj)));
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
}
