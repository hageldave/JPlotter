package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class FontStyleCreator implements ControlPanelCreator {
    String[] styles = new String[]{"None", "Bold", "Italic"};
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        AtomicInteger current = new AtomicInteger((int) getter.invoke(obj));
        JButton button = new JButton(String.valueOf(styles[current.get()]));
        button.setToolTipText("Click to change font style");
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            try {
                current.set((current.get()+1) % 3);
                setter.invoke(obj, current.get());
                button.setText(String.valueOf(styles[current.get()]));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });
        panelContainer.add(button);
        return panelContainer;
    }
}
