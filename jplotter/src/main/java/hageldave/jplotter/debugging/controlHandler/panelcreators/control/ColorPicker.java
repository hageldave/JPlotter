package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public class ColorPicker implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        Color currentColor = (Color) getter.invoke(obj);
        JLabel redValue = new JLabel("Red: " + currentColor.getRed());
        JLabel greenValue = new JLabel(", Green: " + currentColor.getGreen());
        JLabel blueValue = new JLabel(", Blue: " + currentColor.getBlue());
        JButton editButton = new JButton("Edit color");

        Color textColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
        redValue.setForeground(textColor);
        greenValue.setForeground(textColor);
        blueValue.setForeground(textColor);

        editButton.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(labelContainer, "Pick a color", currentColor);
            try {
                if (Objects.nonNull(selectedColor)) {
                    setter.invoke(obj, selectedColor);
                    redValue.setText("Red: " + selectedColor.getRed());
                    greenValue.setText(", Green: " + selectedColor.getGreen());
                    blueValue.setText(", Blue: " + selectedColor.getBlue());

                    Color selectedTextColor =
                            new Color(selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue()).darker();
                    redValue.setForeground(selectedTextColor);
                    greenValue.setForeground(selectedTextColor);
                    blueValue.setForeground(selectedTextColor);
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(redValue);
        labelContainer.add(greenValue);
        labelContainer.add(blueValue);
        labelContainer.add(editButton);
        return labelContainer;
    }
}
