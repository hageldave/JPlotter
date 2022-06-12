package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DecimalSpinnerCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        float initValue = (float) getter.invoke(obj);

        SpinnerNumberModel globalThicknessModel = new SpinnerNumberModel(initValue, 0, Integer.MAX_VALUE, 0.1);
        JSpinner spinner = new JSpinner(globalThicknessModel);

        spinner.setMaximumSize(
                new Dimension((int) (spinner.getMinimumSize().getWidth() + 10), (int) spinner.getMinimumSize().getHeight())
        );

        spinner.addChangeListener(e -> {
            try {
                double modelValue = (double) spinner.getModel().getValue();
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
