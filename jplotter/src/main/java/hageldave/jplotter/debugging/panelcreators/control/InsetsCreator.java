package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Creates four JSpinners with a range from 0 to {@link Integer#MAX_VALUE} and a step size of 1.
 */
public class InsetsCreator implements ControlPanelCreator {
    protected SpinnerNumberModel topModel;
    protected SpinnerNumberModel bottomModel;
    protected SpinnerNumberModel leftModel;
    protected SpinnerNumberModel rightModel;

    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        Insets initValue = (Insets) getter.invoke(obj);

        this.topModel = new SpinnerNumberModel(initValue.top, 0, Integer.MAX_VALUE, 1);
        this.bottomModel = new SpinnerNumberModel(initValue.bottom, 0, Integer.MAX_VALUE, 1);
        this.leftModel = new SpinnerNumberModel(initValue.left, 0, Integer.MAX_VALUE, 1);
        this.rightModel = new SpinnerNumberModel(initValue.right, 0, Integer.MAX_VALUE, 1);

        JPanel topContainer = new JPanel();
        JPanel bottomContainer = new JPanel();

        panelContainer.setLayout(new BoxLayout(panelContainer, BoxLayout.Y_AXIS));
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.X_AXIS));
        bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.X_AXIS));

        topContainer.add(constructCoordContainer(topModel, "Top", setter, obj, canvas));
        topContainer.add(constructCoordContainer(bottomModel, "Bottom", setter, obj, canvas));
        bottomContainer.add(constructCoordContainer(leftModel, "Left", setter, obj, canvas));
        bottomContainer.add(constructCoordContainer(rightModel, "Right", setter, obj, canvas));

        panelContainer.add(topContainer);
        panelContainer.add(bottomContainer);
        return panelContainer;
    }

    private void addListener(double minX, double minY, double maxX, double maxY, Method setter, Object obj, JPlotterCanvas canvas) {
        try {
            setter.invoke(obj, new Insets((int) minX, (int) minY, (int) maxX, (int) maxY));
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
                (double) this.topModel.getValue(),
                (double) this.leftModel.getValue(),
                (double) this.bottomModel.getValue(),
                (double) this.rightModel.getValue(),
                setter,
                obj,
                canvas)
        );

        return coordContainer;
    }
}
