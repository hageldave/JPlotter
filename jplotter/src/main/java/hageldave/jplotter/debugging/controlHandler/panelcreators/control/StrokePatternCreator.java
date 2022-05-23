package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StrokePatternCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        short currentStrokePattern = (short) getter.invoke(obj);
        JLabel strokeLengthLabel = new JLabel(String.valueOf(currentStrokePattern));

        SpinnerNumberModel strokePatternModel = new SpinnerNumberModel(currentStrokePattern, Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(strokePatternModel);
        spinner.setMaximumSize(spinner.getPreferredSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setter.invoke(obj, modelValue);
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

    @Override
    public JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) {
        return ControlPanelCreator.super.createUnchecked(canvas, obj, labelContainer, setter, getter);
    }
}
