package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Creates a JSlider with a range of 100 (starting from 0).
 */
public class PercentageDoubleSliderCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        DecimalFormat df = new DecimalFormat("#0.00", DecimalFormatSymbols.getInstance(Locale.US));
        JLabel valueLabel = new JLabel(df.format(getter.invoke(obj)));
        double initValue = (double) getter.invoke(obj)*100;
        JSlider slider = new JSlider(0, 100, Math.min((int) initValue, 100));
        slider.setCursor(new Cursor(Cursor.HAND_CURSOR));
        slider.setMaximumSize(new Dimension(200, slider.getPreferredSize().height));

        slider.addChangeListener(e -> {
            try {
                slider.setValue(slider.getValue());
                setter.invoke(obj, (double) slider.getValue()/100);
                valueLabel.setText(df.format(getter.invoke(obj)));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        panelContainer.add(valueLabel);
        panelContainer.add(slider);
        return panelContainer;
    }
}
