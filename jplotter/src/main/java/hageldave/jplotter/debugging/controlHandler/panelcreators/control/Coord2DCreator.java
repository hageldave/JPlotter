package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Coord2DCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        Point2D initValue = (Point2D) getter.invoke(obj);

        labelContainer.setBorder(new EmptyBorder(10, 0, 7, 0));

        JLabel xCoordLabel = new JLabel("X Coordinate");
        SpinnerNumberModel xCoordModel = new SpinnerNumberModel((int) initValue.getX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        JSpinner xCoord = new JSpinner(xCoordModel);
        xCoord.setMaximumSize(xCoord.getMinimumSize());

        JPanel xCoordContainer = new JPanel();
        xCoordContainer.setLayout(new BoxLayout(xCoordContainer, BoxLayout.Y_AXIS));
        xCoordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        xCoord.setAlignmentX(Component.LEFT_ALIGNMENT);
        xCoordContainer.add(xCoordLabel);
        xCoordContainer.add(xCoord);

        JLabel yCoordLabel = new JLabel("Y Coordinate");
        SpinnerNumberModel yCoordModel = new SpinnerNumberModel((int) initValue.getY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        JSpinner yCoord = new JSpinner(yCoordModel);
        yCoord.setMaximumSize(yCoord.getMinimumSize());

        JPanel yCoordContainer = new JPanel();
        yCoordContainer.setLayout(new BoxLayout(yCoordContainer, BoxLayout.Y_AXIS));
        yCoordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        yCoord.setAlignmentX(Component.LEFT_ALIGNMENT);
        yCoordContainer.add(yCoordLabel);
        yCoordContainer.add(yCoord);

        xCoord.addChangeListener(e -> {
            try {
                int xCoordValue = (int) xCoord.getModel().getValue();
                int yCoordValue = (int) yCoord.getModel().getValue();
                setter.invoke(obj, xCoordValue, yCoordValue);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        yCoord.addChangeListener(e -> {
            try {
                int xCoordValue = (int) xCoord.getModel().getValue();
                int yCoordValue = (int) yCoord.getModel().getValue();
                setter.invoke(obj, xCoordValue, yCoordValue);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });
        labelContainer.add(xCoordContainer);
        labelContainer.add(yCoordContainer);
        return labelContainer;
    }
}