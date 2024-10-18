package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Lines;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Manipulates the stroke pattern (used in the {@link Lines} for example).
 */
public class StrokePatternCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        short currentStrokePattern = (short) getter.invoke(obj);
        String strokePatternStringAsBin = Integer.toBinaryString(currentStrokePattern);
        String strokePatternStringAsHex = Integer.toHexString(currentStrokePattern);

        int strokePatternLength = strokePatternStringAsBin.length();

        JPanel labelWrapper = new JPanel();
        labelWrapper.setLayout(new BoxLayout(labelWrapper, BoxLayout.X_AXIS));
        labelWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel binaryWrapper = new JPanel();
        binaryWrapper.setLayout(new BoxLayout(binaryWrapper, BoxLayout.X_AXIS));
        binaryWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        createBinaryStringPanel(labelWrapper, strokePatternStringAsBin, strokePatternLength, canvas, obj, setter);

        JButton editButton = new JButton("Edit as Hex");
        binaryWrapper.add(labelWrapper);
        binaryWrapper.add(editButton);
        panelContainer.add(binaryWrapper);

        JPanel container16bit = new JPanel();
        container16bit.setLayout(new BoxLayout(container16bit, BoxLayout.X_AXIS));
        container16bit.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField textField16bit = new JTextField(strokePatternStringAsHex, 20);
        container16bit.add(textField16bit);

        JButton submitButton = new JButton("Submit");
        container16bit.add(submitButton);

        editButton.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = 1L;
			@Override
            public void actionPerformed(ActionEvent e) {
                switchPanels(panelContainer, binaryWrapper, container16bit);
            }
        });

        submitButton.addActionListener(new AbstractAction() {
        	private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                int textfieldValueAsInt = Integer.parseUnsignedInt(textField16bit.getText(), 16);
                String textfieldValueAsBin = Integer.toBinaryString(textfieldValueAsInt);
                int strokePatternLength = textfieldValueAsBin.length();
                try {
                    setter.invoke(obj, textfieldValueAsInt);
                    canvas.scheduleRepaint();
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }
                createBinaryStringPanel(labelWrapper, textfieldValueAsBin, strokePatternLength, canvas, obj, setter);
                switchPanels(panelContainer, container16bit, binaryWrapper);
            }
        });

        return panelContainer;
    }

    private void switchPanels(JPanel wrapper, JPanel toRemove, JPanel toAdd) {
        wrapper.remove(toRemove);
        wrapper.add(toAdd);
        wrapper.validate();
        wrapper.repaint();
    }

    private void createBinaryStringPanel(JPanel wrapper, String strokePatternString, int strokePatternLength, JPlotterCanvas canvas, Object obj, Method setter) {
        wrapper.removeAll();
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
            currentLabel.setToolTipText("Click on the number to flip the bit.");
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

            wrapper.add(label[i]);
            if (i % 4 == 3) {
                JLabel emptyLabel = new JLabel();
                emptyLabel.setBorder(new EmptyBorder(0, 0, 0, 7));
                wrapper.add(emptyLabel);
            }
        }
    }
}
