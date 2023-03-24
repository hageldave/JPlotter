package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TODO
 */
public class TextDecorationCreator implements ControlPanelCreator {
    String[] decorations = new String[]{"None", "Underline", "Strikethrough"};
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        AtomicInteger current = new AtomicInteger((int) getter.invoke(obj));
        JButton button = new JButton(String.valueOf(decorations[current.get()+1]));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            try {
                if (current.get() == 1) {
                    current.set(-1);
                    setter.invoke(obj, current.get());
                } else {
                    current.set((current.get() + 1) % 2);
                    setter.invoke(obj, current.get());
                }
                button.setText(String.valueOf(decorations[current.get()+1]));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });
        panelContainer.add(button);
        return panelContainer;
    }
}
