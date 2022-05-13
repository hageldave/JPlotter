package hageldave.jplotter.debugging.controlHandler.panelcreators;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.PanelCreator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PercentageSliderCreator implements PanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        JLabel valueLabel = new JLabel(getter.invoke(obj).toString());
        float initValue = (Float) getter.invoke(obj)*100;
        JSlider slider = new JSlider(0, 100, Math.min((int) initValue, 100));
        slider.addChangeListener(e -> {
            try {
                slider.setValue(slider.getValue());
                setter.invoke(obj, (double) slider.getValue()/100);
                valueLabel.setText(getter.invoke(obj).toString());
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(valueLabel);
        labelContainer.add(slider);
        return labelContainer;
    }

    @Override
    public JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) {
        return PanelCreator.super.createUnchecked(canvas, obj, labelContainer, setter, getter);
    }
}
