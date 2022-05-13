package hageldave.jplotter.debugging.controlHandler.panelcreators;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.PanelCreator;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TextfieldCreator implements PanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        JLabel fieldValLabel = new JLabel(String.valueOf(getter.invoke(obj)));

        JTextField textfield = new JTextField("", 20);
        textfield.setMaximumSize(textfield.getPreferredSize());

        textfield.addActionListener(e -> {
            try {
                setter.invoke(obj, textfield.getText());
                fieldValLabel.setText((String) getter.invoke(obj));
                textfield.setText("");
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(fieldValLabel);
        labelContainer.add(textfield);
        return labelContainer;
    }

    @Override
    public JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) {
        return PanelCreator.super.createUnchecked(canvas, obj, labelContainer, setter, getter);
    }
}
