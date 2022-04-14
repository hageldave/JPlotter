package hageldave.jplotter.debugging.controlHandler.rendererFields;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RendererFields {
    public static JPanel createIsEnabledUIElements(JPlotterCanvas canvas, Object obj, JPanel labelContainer) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<?> rendererClass = obj.getClass();
        Method isEnabled = rendererClass.getMethod("isEnabled");
        Method setEnabled = rendererClass.getMethod("setEnabled", boolean.class);

        JLabel fieldValLabel = new JLabel(String.valueOf(isEnabled.invoke(obj)));
        fieldValLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    boolean invoked = (boolean) isEnabled.invoke(obj);
                    setEnabled.invoke(obj, !invoked);

                    fieldValLabel.setText(String.valueOf(isEnabled.invoke(obj)));
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
                canvas.scheduleRepaint();
            }
        });
        labelContainer.add(fieldValLabel);
        return labelContainer;
    }
}
