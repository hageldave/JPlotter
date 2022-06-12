package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

public class TextfieldCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        JPanel inlineContainer = new JPanel();
        inlineContainer.setLayout(new BoxLayout(inlineContainer, BoxLayout.X_AXIS));
        inlineContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel textLabel = new JLabel("Current string: " + getter.invoke(obj));
        JTextField textField = new JTextField("", 20);
        textField.setMaximumSize(textField.getPreferredSize());
        JButton editBtn = new JButton("edit");

        inlineContainer.add(textLabel);
        inlineContainer.add(editBtn);

        AtomicBoolean expanded = new AtomicBoolean(false);
        editBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (expanded.get()) {
                    panelContainer.remove(textField);
                    editBtn.setText("edit");
                } else {
                    panelContainer.add(textField);
                    editBtn.setText("close");
                }
                expanded.set(!expanded.get());
                panelContainer.revalidate();
                panelContainer.repaint();
            }
        });

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

        panelContainer.setLayout(new BoxLayout(panelContainer, BoxLayout.Y_AXIS));
        panelContainer.setBorder(new EmptyBorder(10, 0, 7, 0));
        textLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panelContainer.add(inlineContainer);
        return panelContainer;
    }
}
