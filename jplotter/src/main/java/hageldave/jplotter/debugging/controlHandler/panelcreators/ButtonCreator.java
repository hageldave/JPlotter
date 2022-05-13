package hageldave.jplotter.debugging.controlHandler.panelcreators;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.PanelCreator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ButtonCreator implements PanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        boolean currentValue = (boolean) getter.invoke(obj);
        JLabel valueLabel = new JLabel(String.valueOf(currentValue));
        JButton button = new JButton(String.valueOf(currentValue));
        button.addActionListener(e -> {
            try {
                setter.invoke(obj, !(boolean) getter.invoke(obj));
                valueLabel.setText(String.valueOf(!(boolean) getter.invoke(obj)));
                button.setText(String.valueOf(!(boolean) getter.invoke(obj)));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });
        labelContainer.add(button);
        labelContainer.add(valueLabel);

        return labelContainer;
    }

    @Override
    public JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) {
        return PanelCreator.super.createUnchecked(canvas, obj, labelContainer, setter, getter);
    }
}
