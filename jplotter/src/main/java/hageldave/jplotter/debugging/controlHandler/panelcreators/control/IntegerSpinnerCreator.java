package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class IntegerSpinnerCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        int initValue = (int) getter.invoke(obj);

        SpinnerNumberModel globalThicknessModel = new SpinnerNumberModel(initValue, 0, Integer.MAX_VALUE, 1);
        JSpinner spinner = new JSpinner(globalThicknessModel);
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