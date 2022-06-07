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

        JLabel rgbLabel = new JLabel("RBG value: ");
        JLabel redValue = new JLabel(String.valueOf(currentColor.getRed()));
        redValue.setToolTipText("Value of the red color component.");

        JLabel greenValue = new JLabel(", " + currentColor.getGreen());
        greenValue.setToolTipText("Value of the green color component.");

        JLabel blueValue = new JLabel(", " + currentColor.getBlue());
        blueValue.setToolTipText("Value of the blue color component.");

        JLabel alphaValue = new JLabel(", " + currentColor.getAlpha());
        alphaValue.setToolTipText("Value of the alpha color component.");

        JButton editButton = new JButton("Edit color");

        Color textColor = new Color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue());
        ColoredRectangle coloredRectangle = new ColoredRectangle(textColor);

        editButton.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(labelContainer, "Pick a color", currentColor);
            try {
                if (Objects.nonNull(selectedColor)) {
                    setter.invoke(obj, selectedColor);
                    redValue.setText("" + selectedColor.getRed());
                    greenValue.setText(", " + selectedColor.getGreen());
                    blueValue.setText(", " + selectedColor.getBlue());
                    alphaValue.setText(", " + selectedColor.getAlpha());
                    coloredRectangle.updateColor(new Color(selectedColor.getRGB(), true));
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        labelContainer.add(coloredRectangle);
        labelContainer.add(rgbLabel);
        labelContainer.add(redValue);
        labelContainer.add(greenValue);
        labelContainer.add(blueValue);
        labelContainer.add(alphaValue);
        labelContainer.add(editButton);
        return labelContainer;
    }

    private static class ColoredRectangle extends JComponent {
        protected Color c;
        protected int size = 20;
        public ColoredRectangle(Color c) {
            this.c = c;
            this.setMaximumSize(new Dimension(size + 5, size));
            this.setToolTipText("Represents the current color.");
        }

        public void updateColor(Color c) {
            this.c = c;
            this.repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(c);
            g.fillRect(0, 0, size, size);
        }
    }
}
