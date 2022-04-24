package hageldave.jplotter.debugging.controlHandler.rendererFields;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CoordSysRendererFields {
    public static JPanel createPaddingLeftUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method getPaddingLeft = rendererClass.getMethod("getPaddingLeft");
        Method setPaddingLeft = rendererClass.getMethod("setPaddingLeft", int.class);

        int currentPadding = (int) getPaddingLeft.invoke(obj);
        JLabel paddingLeftLabel = new JLabel(String.valueOf(currentPadding));

        SpinnerNumberModel paddingLeftModel = new SpinnerNumberModel(currentPadding, 0, canvas.asComponent().getWidth(), 1);
        JSpinner spinner = new JSpinner(paddingLeftModel);
        spinner.setMaximumSize(spinner.getPreferredSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setPaddingLeft.invoke(obj, modelValue);
                paddingLeftLabel.setText(String.valueOf(modelValue));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(paddingLeftLabel);
        labelContainer.add(spinner);
        return labelContainer;
    }

    public static JPanel createPaddingRightUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method getPaddingRight = rendererClass.getMethod("getPaddingRight");
        Method setPaddingRight = rendererClass.getMethod("setPaddingRight", int.class);

        int currentPadding = (int) getPaddingRight.invoke(obj);
        JLabel paddingRightLabel = new JLabel(String.valueOf(currentPadding));

        SpinnerNumberModel paddingRightModel = new SpinnerNumberModel(currentPadding, 0, canvas.asComponent().getWidth(), 1);
        JSpinner spinner = new JSpinner(paddingRightModel);
        spinner.setMaximumSize(spinner.getPreferredSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setPaddingRight.invoke(obj, modelValue);
                paddingRightLabel.setText(String.valueOf(modelValue));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(paddingRightLabel);
        labelContainer.add(spinner);
        return labelContainer;
    }

    public static JPanel createPaddingTopUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method getPaddingTop = rendererClass.getMethod("getPaddingTop");
        Method setPaddingTop = rendererClass.getMethod("setPaddingTop", int.class);

        int currentPadding = (int) getPaddingTop.invoke(obj);
        JLabel paddingTopLabel = new JLabel(String.valueOf(currentPadding));

        SpinnerNumberModel paddingTopModel = new SpinnerNumberModel(currentPadding, 0, canvas.asComponent().getHeight(), 1);
        JSpinner spinner = new JSpinner(paddingTopModel);
        spinner.setMaximumSize(spinner.getPreferredSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setPaddingTop.invoke(obj, modelValue);
                paddingTopLabel.setText(String.valueOf(modelValue));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(paddingTopLabel);
        labelContainer.add(spinner);
        return labelContainer;
    }

    public static JPanel createPaddingBotUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method getPaddingBot = rendererClass.getMethod("getPaddingBot");
        Method setPaddingBot = rendererClass.getMethod("setPaddingBot", int.class);

        int currentPadding = (int) getPaddingBot.invoke(obj);
        JLabel paddingBotLabel = new JLabel(String.valueOf(currentPadding));

        SpinnerNumberModel paddingBotModel = new SpinnerNumberModel(currentPadding, 0, canvas.asComponent().getHeight(), 1);
        JSpinner spinner = new JSpinner(paddingBotModel);
        spinner.setMaximumSize(spinner.getPreferredSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setPaddingBot.invoke(obj, modelValue);
                paddingBotLabel.setText(String.valueOf(modelValue));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(paddingBotLabel);
        labelContainer.add(spinner);
        return labelContainer;
    }
}
