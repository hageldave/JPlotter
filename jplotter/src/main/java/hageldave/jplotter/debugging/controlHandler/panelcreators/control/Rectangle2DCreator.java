package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Rectangle2DCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        Rectangle2D initValue = (Rectangle2D) getter.invoke(obj);

        SpinnerNumberModel xMinCoordModel = new SpinnerNumberModel((int) initValue.getMinX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        JSpinner xMinCoord = new JSpinner(xMinCoordModel);
        xMinCoord.setMaximumSize(xMinCoord.getMinimumSize());

        SpinnerNumberModel xMaxCoordModel = new SpinnerNumberModel((int) initValue.getMaxX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        JSpinner xMaxCoord = new JSpinner(xMaxCoordModel);
        xMaxCoord.setMaximumSize(xMaxCoord.getMinimumSize());

        SpinnerNumberModel yMinCoordModel = new SpinnerNumberModel((int) initValue.getMinY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        JSpinner yMinCoord = new JSpinner(yMinCoordModel);
        yMinCoord.setMaximumSize(yMinCoord.getMinimumSize());

        SpinnerNumberModel yMaxCoordModel = new SpinnerNumberModel((int) initValue.getMaxY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        JSpinner yMaxCoord = new JSpinner(yMaxCoordModel);
        yMaxCoord.setMaximumSize(yMaxCoord.getMinimumSize());

        xMinCoord.addChangeListener(e -> addListener(
                (double) xMinCoord.getModel().getValue(),
                (double) xMaxCoord.getModel().getValue(),
                (double) yMinCoord.getModel().getValue(),
                (double) yMaxCoord.getModel().getValue(),
                setter,
                obj,
                canvas)
        );

        yMinCoord.addChangeListener(e -> addListener(
                (double) xMinCoord.getModel().getValue(),
                (double) xMaxCoord.getModel().getValue(),
                (double) yMinCoord.getModel().getValue(),
                (double) yMaxCoord.getModel().getValue(),
                setter,
                obj,
                canvas)
        );

        xMaxCoord.addChangeListener(e -> addListener(
                (double) xMinCoord.getModel().getValue(),
                (double) xMaxCoord.getModel().getValue(),
                (double) yMinCoord.getModel().getValue(),
                (double) yMaxCoord.getModel().getValue(),
                setter,
                obj,
                canvas)
        );

        yMaxCoord.addChangeListener(e -> addListener(
                (double) xMinCoord.getModel().getValue(),
                (double) xMaxCoord.getModel().getValue(),
                (double) yMinCoord.getModel().getValue(),
                (double) yMaxCoord.getModel().getValue(),
                setter,
                obj,
                canvas)
        );

        labelContainer.add(xMinCoord);
        labelContainer.add(xMaxCoord);
        labelContainer.add(yMinCoord);
        labelContainer.add(yMaxCoord);
        return labelContainer;
    }

    private void addListener(double minX, double minY, double maxX, double maxY, Method setter, Object obj, JPlotterCanvas canvas) {
        try {
            setter.invoke(obj, minX, minY, maxX, maxY);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        canvas.scheduleRepaint();
    }
}

