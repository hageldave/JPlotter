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

        if (field.getName().equals("globalSaturationMultiplier")) {
            handleGlobalMultiplier(canvas, obj, field, labelContainer, "getGlobalSaturationMultiplier", "setGlobalSaturationMultiplier");
        } else if (field.getName().equals("globalThicknessMultiplier")) {
            handleGlobalMultiplier(canvas, obj, field, labelContainer, "getGlobalThicknessMultiplier", "setGlobalThicknessMultiplier");
        } else if (field.getName().equals("globalAlphaMultiplier")) {
            handleGlobalMultiplier(canvas, obj, field, labelContainer, "getGlobalAlphaMultiplier", "setGlobalAlphaMultiplier");
        } else if (field.getName().equals("hidden")) {
            handleHideRenderable(canvas, obj, field, labelContainer);
        } else {
            if (fieldValue != null) {
                labelContainer.add(new JLabel(("(" + field.getType()) + ") "));

                JLabel fieldName = new JLabel((field.getName()) + ": ");
                fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
                labelContainer.add(fieldName);
                if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                    DoubleSupplier dSup = (DoubleSupplier) fieldValue;
                    labelContainer.add(new JLabel(String.valueOf(dSup.getAsDouble())));
                } else {
                    labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            }
        }

        Dimension d = labelContainer.getPreferredSize();
        d.height = 1;
        labelContainer.setPreferredSize(d);

        return labelContainer;
    }

    protected static JPanel handleGlobalMultiplier(JPlotterCanvas canvas, Object obj, Field field, JPanel labelContainer, String getter, String setter) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setGlobalSaturationMultiplier = renderableClass.getMethod(setter, double.class);
        Method getGlobalSaturationMultiplier = renderableClass.getMethod(getter);

        labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
        JLabel fieldName = new JLabel((field.getName()) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        labelContainer.add(fieldName);

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

    protected static JPanel handleHideRenderable(JPlotterCanvas canvas, Object obj, Field field, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method hide = renderableClass.getMethod("hide", boolean.class);
        Method isHidden = renderableClass.getMethod("isHidden");

        labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
        JLabel fieldName = new JLabel((field.getName()) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        labelContainer.add(fieldName);

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
