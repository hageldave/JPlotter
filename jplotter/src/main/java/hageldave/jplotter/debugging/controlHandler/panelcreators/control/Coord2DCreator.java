package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Coord2DCreator implements ControlPanelCreator {
    protected SpinnerNumberModel xCoordModel;
    protected SpinnerNumberModel yCoordModel;

    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception {
        Point2D initValue = (Point2D) getter.invoke(obj);
        labelContainer.setBorder(new EmptyBorder(10, 0, 7, 0));
        this.xCoordModel = new SpinnerNumberModel((int) initValue.getX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        this.yCoordModel = new SpinnerNumberModel((int) initValue.getY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 1);
        labelContainer.add(constructCoordContainer(xCoordModel, "X Coordinate", setter, obj, canvas));
        labelContainer.add(constructCoordContainer(yCoordModel, "Y Coordinate", setter, obj, canvas));
        return labelContainer;
    }

    private void addListener(int xCoordValue, int yCoordValue, Method setter, Object obj, JPlotterCanvas canvas) {
        try {
            setter.invoke(obj, xCoordValue, yCoordValue);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        canvas.scheduleRepaint();
    }

    private JPanel constructCoordContainer(SpinnerNumberModel spinnerNumberModel, String label, Method setter, Object obj, JPlotterCanvas canvas) {
        JPanel coordContainer = new JPanel();
        coordContainer.setLayout(new BoxLayout(coordContainer, BoxLayout.Y_AXIS));
        JLabel coordLabel = new JLabel(label);

        JSpinner coordSpinner = new JSpinner(spinnerNumberModel);
        coordSpinner.setMaximumSize(coordSpinner.getMinimumSize());

        coordLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        coordSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        coordContainer.add(coordLabel);
        coordContainer.add(coordSpinner);

        coordSpinner.addChangeListener(e -> addListener(
                (int) this.xCoordModel.getValue(),
                (int) this.yCoordModel.getValue(),
                setter,
                obj,
                canvas)
        );

        return coordContainer;
    }
}