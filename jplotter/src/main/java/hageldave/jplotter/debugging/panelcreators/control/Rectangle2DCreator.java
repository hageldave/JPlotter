package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates multiple JSpinners to manipulate the coordinates of a rectangle.
 */
public class Rectangle2DCreator implements ControlPanelCreator {
    protected SpinnerNumberModel xMinCoordModel;
    protected SpinnerNumberModel xMaxCoordModel;
    protected SpinnerNumberModel yMinCoordModel;
    protected SpinnerNumberModel yMaxCoordModel;

    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        Rectangle2D initValue = (Rectangle2D) getter.invoke(obj);

        this.xMinCoordModel = new SpinnerNumberModel(initValue.getMinX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        this.xMaxCoordModel = new SpinnerNumberModel(initValue.getMaxX(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        this.yMinCoordModel = new SpinnerNumberModel(initValue.getMinY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);
        this.yMaxCoordModel = new SpinnerNumberModel(initValue.getMaxY(), Integer.MIN_VALUE, Integer.MAX_VALUE, 0.1);

        JPanel topContainer = new JPanel();
        JPanel bottomContainer = new JPanel();

        panelContainer.setLayout(new BoxLayout(panelContainer, BoxLayout.Y_AXIS));
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.X_AXIS));
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.X_AXIS));

        topContainer.add(constructCoordContainer(xMinCoordModel, "X Min", setter, obj, canvas));
        topContainer.add(constructCoordContainer(xMaxCoordModel, "X Max", setter, obj, canvas));
        bottomContainer.add(constructCoordContainer(yMinCoordModel, "Y Min", setter, obj, canvas));
        bottomContainer.add(constructCoordContainer(yMaxCoordModel, "Y Max", setter, obj, canvas));

        panelContainer.add(topContainer);
        panelContainer.add(bottomContainer);
        return panelContainer;
    }

    private void addListener(double minX, double minY, double maxX, double maxY, Method setter, Object obj, JPlotterCanvas canvas) {
        try {
            setter.invoke(obj, minX, minY, maxX, maxY);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
        canvas.scheduleRepaint();
    }

    private JPanel constructCoordContainer(SpinnerNumberModel spinnerNumberModel, String label, Method setter, Object obj, JPlotterCanvas canvas) {
        JPanel coordContainer = new JPanel();
        coordContainer.setLayout(new BoxLayout(coordContainer, BoxLayout.Y_AXIS));
        JLabel coordLabel = new JLabel(label, SwingConstants.LEFT);

        JSpinner coordSpinner = new JSpinner(spinnerNumberModel);
        coordSpinner.setMaximumSize(coordSpinner.getMinimumSize());

        coordSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        coordContainer.add(coordLabel);
        coordContainer.add(coordSpinner);

        coordSpinner.addChangeListener(e -> addListener(
                (double) this.xMinCoordModel.getValue(),
                (double) this.yMinCoordModel.getValue(),
                (double) this.xMaxCoordModel.getValue(),
                (double) this.yMaxCoordModel.getValue(),
                setter,
                obj,
                canvas)
        );

        return coordContainer;
    }
}

