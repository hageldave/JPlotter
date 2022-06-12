package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class ColorPicker implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        AtomicReference<Color> currentColor = new AtomicReference<>((Color) getter.invoke(obj));

        panelContainer.setLayout(new BoxLayout(panelContainer, BoxLayout.Y_AXIS));
        panelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel colorInfoContainer = new JPanel();
        colorInfoContainer.setLayout(new BoxLayout(colorInfoContainer, BoxLayout.X_AXIS));
        colorInfoContainer.setBorder(new EmptyBorder(10, 0, 7, 0));
        colorInfoContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel rgbLabel = new JLabel("RBG value: ");

        JLabel redValue = new JLabel(String.valueOf(currentColor.get().getRed()));
        redValue.setToolTipText("Value of the red color component.");

        JLabel greenValue = new JLabel(", " + currentColor.get().getGreen());
        greenValue.setToolTipText("Value of the green color component.");

        JLabel blueValue = new JLabel(", " + currentColor.get().getBlue());
        blueValue.setToolTipText("Value of the blue color component.");

        JLabel alphaValue = new JLabel(", " + currentColor.get().getAlpha());
        alphaValue.setToolTipText("Value of the alpha color component.");

        JButton editButton = new JButton("Edit color");
        editButton.setMargin(new Insets(0,0,0,0));
        editButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        Color textColor = new Color(currentColor.get().getRed(), currentColor.get().getGreen(), currentColor.get().getBlue());
        ColoredRectangle coloredRectangle = new ColoredRectangle(textColor);

        editButton.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(panelContainer, "Pick a color", currentColor.get());
            try {
                if (Objects.nonNull(selectedColor)) {
                    setter.invoke(obj, selectedColor);
                    redValue.setText("" + selectedColor.getRed());
                    greenValue.setText(", " + selectedColor.getGreen());
                    blueValue.setText(", " + selectedColor.getBlue());
                    alphaValue.setText(", " + selectedColor.getAlpha());
                    coloredRectangle.updateColor(new Color(selectedColor.getRGB(), true));
                    currentColor.set(selectedColor);
                }
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        colorInfoContainer.add(coloredRectangle);
        colorInfoContainer.add(rgbLabel);
        colorInfoContainer.add(redValue);
        colorInfoContainer.add(greenValue);
        colorInfoContainer.add(blueValue);
        colorInfoContainer.add(alphaValue);
        panelContainer.add(colorInfoContainer);
        panelContainer.add(editButton);
        return panelContainer;
    }

    private static class ColoredRectangle extends JComponent {
        protected Color c;
        protected int size = 17;
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
