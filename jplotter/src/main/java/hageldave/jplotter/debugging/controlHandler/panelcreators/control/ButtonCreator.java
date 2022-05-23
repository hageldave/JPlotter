package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ButtonCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        boolean currentValue = (boolean) getter.invoke(obj);
        JButton button = new JButton(String.valueOf(currentValue));
        button.addActionListener(e -> {
            try {
                setter.invoke(obj, !(boolean) getter.invoke(obj));
                button.setText(String.valueOf((boolean) getter.invoke(obj)));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });
        labelContainer.add(button);
        return labelContainer;
    }
}
