package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.misc.DefaultGlyph;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GlyphSelector implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        JComboBox<DefaultGlyph> glyphSelector = new JComboBox<>(DefaultGlyph.values());

        glyphSelector.setSelectedItem(getter.invoke(obj));
        glyphSelector.setMaximumSize(glyphSelector.getPreferredSize());

        glyphSelector.addItemListener(e -> {
            glyphSelector.setSelectedItem(e.getItem());
            try {
                setter.invoke(obj, e.getItem());
                canvas.scheduleRepaint();
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        });

        panelContainer.add(glyphSelector);
        return panelContainer;
    }
}
