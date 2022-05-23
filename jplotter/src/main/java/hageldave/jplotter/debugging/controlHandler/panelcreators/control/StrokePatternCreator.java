package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StrokePatternCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        short currentStrokePattern = (short) getter.invoke(obj);
        String strokePatternString = Integer.toUnsignedString(currentStrokePattern, 2);
        int strokePatternLength = strokePatternString.length();

        String[] value = new String[strokePatternLength];
        JLabel[] label = new JLabel[strokePatternLength];
        for (int i = 0; i < strokePatternLength; i++) {
            value[i] = String.valueOf(strokePatternString.charAt(i));
            label[i] = new JLabel(value[i]);

            Font bold = new Font(label[i].getFont().getName(), Font.BOLD, label[i].getFont().getSize());
            Font regular = new Font(label[i].getFont().getName(), Font.PLAIN, label[i].getFont().getSize());

            if (value[i].equals("1"))
                label[i].setFont(bold);

            label[i].setBorder(new EmptyBorder(2, 1, 2, 1));
            label[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel currentLabel = label[i];
            currentLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    super.mouseClicked(e);
                    int currentValue = Integer.parseInt(currentLabel.getText());
                    if (currentValue == 0) {
                        currentLabel.setText(String.valueOf(1));
                        currentLabel.setFont(bold);
                    } else if (currentValue == 1) {
                        currentLabel.setText(String.valueOf(0));
                        currentLabel.setFont(regular);
                    }

                    for (int i = 0; i < strokePatternLength; i++) {
                        value[i] = label[i].getText();
                    }
                    try {
                        setter.invoke(obj, Integer.parseUnsignedInt(String.join("", value), 2));
                        canvas.scheduleRepaint();
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            labelContainer.add(label[i]);
            if (i % 4 == 3) {
                JLabel emptyLabel = new JLabel();
                emptyLabel.setBorder(new EmptyBorder(0, 0, 0, 7));
                labelContainer.add(emptyLabel);
            }
        }
        return labelContainer;
    }
}
