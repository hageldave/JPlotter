package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates a JSpinner with a step size of 1.0.
 */
public class IntegerSpinnerCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        int initValue = (int) getter.invoke(obj);

        SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(initValue, 0, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(spinnerNumberModel);
        spinner.setMaximumSize(spinner.getMinimumSize());

        spinner.addChangeListener(e -> {
            try {
                int modelValue = (int) spinner.getModel().getValue();
                setter.invoke(obj, modelValue);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });
        panelContainer.add(spinner);
        return panelContainer;
    }
}