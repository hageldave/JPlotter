package hageldave.jplotter.debugging.controlHandler.panelcreators;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.PanelCreator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StrokeLengthCreator implements PanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        int currentStrokeLength = (int) ((float) getter.invoke(obj));
        JLabel strokeLengthLabel = new JLabel(String.valueOf(currentStrokeLength));

        SpinnerNumberModel strokeLengthModel = new SpinnerNumberModel(currentStrokeLength, 1, canvas.asComponent().getWidth(), 1);
        JSpinner spinner = new JSpinner(strokeLengthModel);
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
        return PanelCreator.super.createUnchecked(canvas, obj, labelContainer, setter, getter);
    }
}
