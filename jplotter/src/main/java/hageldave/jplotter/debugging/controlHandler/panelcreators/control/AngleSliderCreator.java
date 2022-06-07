package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AngleSliderCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        float angle = (float) getter.invoke(obj);
        int roundedAngle = (int) (angle * 180 / Math.PI);

        JLabel valueLabel = new JLabel(String.valueOf(roundedAngle));
        valueLabel.setToolTipText("Angle in Degrees");
        JSlider slider = new JSlider(-360, 360, roundedAngle);
        slider.setMaximumSize(new Dimension(250, slider.getPreferredSize().height));

        slider.addChangeListener(e -> {
            try {
                slider.setValue(slider.getValue());
                valueLabel.setText(String.valueOf(slider.getValue()));

                double angleInRad = (slider.getValue() * Math.PI) / 180;
                setter.invoke(obj, angleInRad);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(valueLabel);
        labelContainer.add(slider);
        return labelContainer;
    }
}
