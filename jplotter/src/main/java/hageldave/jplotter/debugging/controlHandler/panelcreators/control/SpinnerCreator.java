package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;

public class SpinnerCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        JLabel valueLabel = new JLabel(getter.invoke(obj).toString());
        float initValue = (Float) getter.invoke(obj);

        SpinnerNumberModel globalThicknessModel = new SpinnerNumberModel(initValue, 0, 5, 0.1);
        JSpinner spinner = new JSpinner(globalThicknessModel);

        Dimension prefSize = spinner.getPreferredSize();
        prefSize = new Dimension(50, prefSize.height);
        spinner.setMaximumSize(prefSize);

        spinner.addChangeListener(e -> {
            try {
                double modelValue = (double) spinner.getModel().getValue();
                setter.invoke(obj, modelValue);
                valueLabel.setText(String.valueOf(BigDecimal.valueOf(modelValue).round(MathContext.DECIMAL64).stripTrailingZeros()));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(valueLabel);
        labelContainer.add(spinner);
        return labelContainer;
    }

    @Override
    public JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) {
        return ControlPanelCreator.super.createUnchecked(canvas, obj, labelContainer, setter, getter);
    }
}
