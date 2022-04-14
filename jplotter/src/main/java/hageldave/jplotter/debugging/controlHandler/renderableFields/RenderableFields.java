package hageldave.jplotter.debugging.controlHandler.renderableFields;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RenderableFields {
    public static JPanel createGlobalThicknessMultiplierUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setGlobalSaturationMultiplier = renderableClass.getMethod("setGlobalThicknessMultiplier", double.class);
        Method getGlobalSaturationMultiplier = renderableClass.getMethod("getGlobalThicknessMultiplier");

        JLabel valueLabel = new JLabel(getGlobalSaturationMultiplier.invoke(obj).toString());
        float initValue = (Float) getGlobalSaturationMultiplier.invoke(obj)*100;

        // TODO: check initValue
        JSlider slider = new JSlider(0, 100, Math.min((int) initValue, 100));

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

    public static JPanel createGlobalSaturationMultiplierUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setGlobalSaturationMultiplier = renderableClass.getMethod("setGlobalSaturationMultiplier", double.class);
        Method getGlobalSaturationMultiplier = renderableClass.getMethod("getGlobalSaturationMultiplier");

        JLabel valueLabel = new JLabel(getGlobalSaturationMultiplier.invoke(obj).toString());
        float initValue = (Float) getGlobalSaturationMultiplier.invoke(obj)*100;


        // TODO: check initValue
        JSlider slider = new JSlider(0, 100, Math.min((int) initValue, 100));

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

    public static JPanel createGlobalAlphaMultiplierUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setGlobalSaturationMultiplier = renderableClass.getMethod("setGlobalAlphaMultiplier", double.class);
        Method getGlobalSaturationMultiplier = renderableClass.getMethod("getGlobalAlphaMultiplier");

        JLabel valueLabel = new JLabel(getGlobalSaturationMultiplier.invoke(obj).toString());
        float initValue = (Float) getGlobalSaturationMultiplier.invoke(obj)*100;


        // TODO: check initValue
        JSlider slider = new JSlider(0, 100, Math.min((int) initValue, 100));

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

    public static JPanel createHideUIRenderableElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
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

    public static JPanel createAngleUIRenderableElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setAngle = renderableClass.getMethod("setAngle", double.class);
        Method getAngle = renderableClass.getMethod("getAngle");

        JLabel fieldValLabel = new JLabel(String.valueOf(getAngle.invoke(obj)));

        JTextField angleTextfield = new JTextField();
        angleTextfield.addActionListener(e -> {
            try {
                setAngle.invoke(obj, Double.valueOf(angleTextfield.getText()));
                fieldValLabel.setText(String.valueOf(getAngle.invoke(obj)));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(fieldValLabel);
        labelContainer.add(angleTextfield);
        return labelContainer;
    }

    public static JPanel createTextStrUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> renderableClass = obj.getClass();
        Method setTxtStr = renderableClass.getMethod("setTextString", String.class);
        Method getTxtStr = renderableClass.getMethod("getTextString");

        JLabel fieldValLabel = new JLabel(String.valueOf(getTxtStr.invoke(obj)));

        JTextField textfield = new JTextField();
        textfield.addActionListener(e -> {
            try {
                setTxtStr.invoke(obj, textfield.getText());
                fieldValLabel.setText((String) getTxtStr.invoke(obj));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(fieldValLabel);
        labelContainer.add(textfield);
        return labelContainer;
    }
}
