package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.panelcreators.control.ControlPanelCreator;
import hageldave.jplotter.debugging.controlHandler.panelcreators.display.DisplayPanelCreator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public class FieldHandler {
    public static JPanel controlField(JPlotterCanvas canvas,
                                      Object obj,
                                      String field,
                                      AtomicReference<Method> getter,
                                      AtomicReference<Method> setter,
                                      AtomicReference<Class<? extends ControlPanelCreator>> creator) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(0, 0, 7, 0));

        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.X_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(0, 0, 3, 0));

        JLabel fieldType = new JLabel(("" + getter.get().getReturnType().getSimpleName()) + " ");
        JLabel fieldName = new JLabel((field) + " ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        fieldName.setToolTipText("Annotation key of the property");
        fieldType.setFont(new Font(fieldType.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()-2));
        fieldType.setForeground(Color.GRAY);
        fieldType.setToolTipText("Type of the property");

        labelContainer.add(fieldType);
        labelContainer.add(fieldName);

        JPanel controlContainer = new JPanel();
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.X_AXIS));
        controlContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlContainer.setBorder(new EmptyBorder(0, 0, 7, 0));

        container.add(labelContainer);
        container.add(controlContainer);

        try {
            ControlPanelCreator pc = creator.get().getDeclaredConstructor().newInstance();
            pc.createUnchecked(canvas, obj, controlContainer, setter.get(), getter.get());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        JSeparator sep = new JSeparator();
        container.add(sep);

        return container;
    }

    public static JPanel displayField(JPlotterCanvas canvas,
                                      Object obj,
                                      String field,
                                      AtomicReference<Method> getter,
                                      AtomicReference<Class<? extends DisplayPanelCreator>> creator) {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.X_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(9, 0, 9, 0));

        JLabel fieldType = new JLabel(("" + getter.get().getReturnType().getSimpleName()) + " ");
        JLabel fieldName = new JLabel((field) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        fieldName.setToolTipText("Annotation key of the property");
        fieldType.setFont(new Font(fieldType.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()-2));
        fieldType.setForeground(Color.GRAY);
        fieldType.setToolTipText("Type of the property");

        labelContainer.add(fieldType);
        labelContainer.add(fieldName);

        try {
            DisplayPanelCreator pc = creator.get().getDeclaredConstructor().newInstance();
            pc.createUnchecked(canvas, obj, labelContainer, getter.get());
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return labelContainer;
    }
}