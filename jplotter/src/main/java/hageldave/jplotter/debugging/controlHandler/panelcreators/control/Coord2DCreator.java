package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Coord2DCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        Point2D initValue = (Point2D) getter.invoke(obj);

        SpinnerNumberModel xCoordModel = new SpinnerNumberModel((int) initValue.getX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        JSpinner xCoord = new JSpinner(xCoordModel);
        xCoord.setMaximumSize(xCoord.getMinimumSize());

        SpinnerNumberModel yCoordModel = new SpinnerNumberModel((int) initValue.getY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        JSpinner yCoord = new JSpinner(yCoordModel);
        yCoord.setMaximumSize(yCoord.getMinimumSize());

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

        labelContainer.add(xCoord);
        labelContainer.add(yCoord);
        return labelContainer;
    }
}
