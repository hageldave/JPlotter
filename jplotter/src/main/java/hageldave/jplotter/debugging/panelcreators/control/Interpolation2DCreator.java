package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.util.Pair;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates two JSpinners with a range from 0 to 1 and a step size of 0.01.
 * This can be used if two values should be changed in the range between 0 and 1.
 */
public class Interpolation2DCreator implements ControlPanelCreator {
    protected SpinnerNumberModel xCoordModel;
    protected SpinnerNumberModel yCoordModel;

    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        Pair<Double, Double> initValue = (Pair<Double, Double>) getter.invoke(obj);
        panelContainer.setBorder(new EmptyBorder(10, 0, 7, 0));
        this.xCoordModel = new SpinnerNumberModel((double) initValue.first, 0.0, 1.0, 0.01);
        this.yCoordModel = new SpinnerNumberModel((double) initValue.second, 0.0, 1.0, 0.01);
        panelContainer.add(constructCoordContainer(xCoordModel, "X Coordinate", setter, obj, canvas));
        panelContainer.add(constructCoordContainer(yCoordModel, "Y Coordinate", setter, obj, canvas));
        return panelContainer;
    }

    private void addListener(double xCoordValue, double yCoordValue, Method setter, Object obj, JPlotterCanvas canvas) {
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
        JLabel coordLabel = new JLabel(label, SwingConstants.LEFT);

        JSpinner coordSpinner = new JSpinner(spinnerNumberModel);
        coordSpinner.setMaximumSize(new Dimension(100, 200));

        coordSpinner.setAlignmentX(Component.LEFT_ALIGNMENT);
        coordContainer.add(coordLabel);
        coordContainer.add(coordSpinner);

        coordSpinner.addChangeListener(e -> addListener(
                (Double) this.xCoordModel.getValue(),
                (Double) this.yCoordModel.getValue(),
                setter,
                obj,
                canvas)
        );

        return coordContainer;
    }
}
