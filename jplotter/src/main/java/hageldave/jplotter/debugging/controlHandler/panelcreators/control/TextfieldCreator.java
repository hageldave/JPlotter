package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TextfieldCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        JLabel textLabel = new JLabel("Current string: " + getter.invoke(obj));
        JTextField textField = new JTextField("", 20);
        textField.setMaximumSize(textField.getPreferredSize());

        textField.addActionListener(e -> {
            try {
                setter.invoke(obj, textField.getText());
                textLabel.setText("Current string: " + getter.invoke(obj));
                textField.setText("");
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.Y_AXIS));
        labelContainer.setBorder(new EmptyBorder(10, 0, 7, 0));
        textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);

        labelContainer.add(textLabel);
        labelContainer.add(textField);
        return labelContainer;
    }
}
