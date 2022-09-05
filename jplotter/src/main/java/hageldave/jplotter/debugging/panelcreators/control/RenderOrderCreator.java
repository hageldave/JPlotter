package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderers.CompleteRenderer;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Manipulates the render order.
 */
public class RenderOrderCreator implements ControlPanelCreator {
    protected int[] initRenderOrder;
    Map<Integer, String> rendererString2Index = mapRendererString2Index();
    Map<Integer, Integer> index2renderer = new HashMap<>();

    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        this.initRenderOrder = (int[]) getter.invoke(obj);

        for (int i = 0; i < this.initRenderOrder.length; i++)
            index2renderer.put(i, this.initRenderOrder[i]);

        panelContainer.setLayout(new BoxLayout(panelContainer, BoxLayout.Y_AXIS));
        panelContainer.add(createComboBox(new JPanel(), "First Renderer", 0, setter, obj, canvas));
        panelContainer.add(createComboBox(new JPanel(), "Second Renderer", 1, setter, obj, canvas));
        panelContainer.add(createComboBox(new JPanel(), "Third Renderer", 2, setter, obj, canvas));
        panelContainer.add(createComboBox(new JPanel(), "Fourth Renderer", 3, setter, obj, canvas));
        panelContainer.add(createComboBox(new JPanel(), "Fifth Renderer", 4, setter, obj, canvas));
        return panelContainer;
    }

    protected Map<Integer, String> mapRendererString2Index() {
        Map<Integer, String> rendererString2Index = new HashMap<>();
        rendererString2Index.put(CompleteRenderer.TRI, "Triangle");
        rendererString2Index.put(CompleteRenderer.LIN, "Line");
        rendererString2Index.put(CompleteRenderer.PNT, "Point");
        rendererString2Index.put(CompleteRenderer.TXT, "Text");
        rendererString2Index.put(CompleteRenderer.CRV, "Curve");

        return rendererString2Index;
    }

    private JPanel createComboBox(JPanel container, String label, int index, Method setter, Object obj, JPlotterCanvas canvas) {
        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(new JLabel(label, SwingConstants.LEFT));

        String[] stringList = rendererString2Index.values().toArray(new String[0]);

        JComboBox<String> rendererSelect = new JComboBox<>(stringList);
        rendererSelect.setMaximumSize(rendererSelect.getPreferredSize());
        rendererSelect.setSelectedItem(stringList[index]);

        rendererSelect.addItemListener(e -> {
            this.initRenderOrder[index] = rendererSelect.getSelectedIndex();
            try {
                setter.invoke(obj, this.initRenderOrder[0], this.initRenderOrder[1], this.initRenderOrder[2], this.initRenderOrder[3], this.initRenderOrder[4]);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        container.add(rendererSelect);
        return container;
    }
}
