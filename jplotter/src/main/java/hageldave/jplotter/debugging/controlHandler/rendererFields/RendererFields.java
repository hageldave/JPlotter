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

        String toggleEnabledButtonText = (boolean) isEnabled.invoke(obj) ? "Disable": "Enable";
        JButton toggleEnabled = new JButton(toggleEnabledButtonText);
        toggleEnabled.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    setEnabled.invoke(obj, ! (boolean) (isEnabled.invoke(obj)));
                    fieldValLabel.setText(String.valueOf(isEnabled.invoke(obj)));
                    toggleEnabled.setText((boolean) isEnabled.invoke(obj) ? "Disable": "Enable");
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
                canvas.scheduleRepaint();
            }
        });
        labelContainer.add(fieldValLabel);
        labelContainer.add(toggleEnabled);
        return labelContainer;
    }
}
