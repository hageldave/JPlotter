package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Creates a JTextField to manipulate a String object.
 */
public class TextfieldCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        JPanel inlineContainer = new JPanel();
        inlineContainer.setLayout(new BoxLayout(inlineContainer, BoxLayout.X_AXIS));
        inlineContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel textLabel = new JLabel("<html>Current string: <b>" + getter.invoke(obj) + "</b></html>", SwingConstants.LEFT);

        JPanel textFieldPanel = new JPanel();
        JLabel textFieldLabel = new JLabel("Enter new text here:", SwingConstants.LEFT);
        textFieldPanel.setLayout(new BoxLayout(textFieldPanel, BoxLayout.Y_AXIS));
        textFieldPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textFieldPanel.setBorder(new EmptyBorder(15, 0, 7, 0));
        JTextField textField = new JTextField("", 20);
        textField.setToolTipText("Press enter to save the changes");
        textField.setMaximumSize(textField.getPreferredSize());
        textFieldPanel.add(textFieldLabel);
        textFieldPanel.add(textField);

        JButton editBtn = new JButton("Edit text");
        editBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        inlineContainer.add(textLabel);
        inlineContainer.add(editBtn);

        AtomicBoolean expanded = new AtomicBoolean(false);
        editBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (expanded.get()) {
                    panelContainer.remove(textFieldPanel);
                    editBtn.setText("Edit text");
                } else {
                    panelContainer.add(textFieldPanel);
                    editBtn.setText("Close");
                }
                expanded.set(!expanded.get());
                panelContainer.validate();
            }
        });

        textField.addActionListener(e -> {
            try {
                setter.invoke(obj, textField.getText());
                textLabel.setText( "<html>Current string: <b>" + getter.invoke(obj) + "</b></html>");
                textField.setText("");
            } catch (IllegalAccessException | InvocationTargetException ex) {
                ex.printStackTrace();
            }
            canvas.scheduleRepaint();
        });

        panelContainer.setLayout(new BoxLayout(panelContainer, BoxLayout.Y_AXIS));
        panelContainer.setBorder(new EmptyBorder(10, 0, 7, 0));
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelContainer.add(inlineContainer);
        return panelContainer;
    }
}
