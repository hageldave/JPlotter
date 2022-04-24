package hageldave.jplotter.debugging.controlHandler.renderableFields;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Renderable;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LinesFields {
    public static JPanel createStrokeLengthUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method getStrokeLength = rendererClass.getMethod("getStrokeLength");
        Method setStrokeLength = rendererClass.getMethod("setStrokeLength", double.class);

        int currentStrokeLength = (int) ((float) getStrokeLength.invoke(obj));
        JLabel strokeLengthLabel = new JLabel(String.valueOf(currentStrokeLength));

        SpinnerNumberModel strokeLengthModel = new SpinnerNumberModel(currentStrokeLength, 1, canvas.asComponent().getWidth(), 1);
        JSpinner spinner = new JSpinner(strokeLengthModel);
        spinner.setMaximumSize(spinner.getPreferredSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setStrokeLength.invoke(obj, modelValue);
                strokeLengthLabel.setText(String.valueOf(modelValue));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(strokeLengthLabel);
        labelContainer.add(spinner);
        return labelContainer;
    }

    public static JPanel createStrokePatternUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();

        // TODO: this needs to be handled differently
        if (Renderable.class.isAssignableFrom(rendererClass)) {
            Method getStrokePattern = rendererClass.getMethod("getStrokePattern");
            Method setStrokePattern = rendererClass.getMethod("setStrokePattern", int.class);

            short currentStrokePattern = (short) getStrokePattern.invoke(obj);
            JLabel strokeLengthLabel = new JLabel(String.valueOf(currentStrokePattern));

            SpinnerNumberModel strokePatternModel = new SpinnerNumberModel(currentStrokePattern, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
            JSpinner spinner = new JSpinner(strokePatternModel);
            spinner.setMaximumSize(spinner.getPreferredSize());

            spinner.addChangeListener(e -> {
                try {
                    int modelValue = (int) spinner.getModel().getValue();
                    setStrokePattern.invoke(obj, modelValue);
                    strokeLengthLabel.setText(String.valueOf(modelValue));
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
                canvas.scheduleRepaint();
            });

            labelContainer.add(strokeLengthLabel);
            labelContainer.add(spinner);
        }
        return labelContainer;
    }
}
