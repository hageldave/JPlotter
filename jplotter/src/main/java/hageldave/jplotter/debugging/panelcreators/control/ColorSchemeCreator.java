package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.color.DefaultColorScheme;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ColorSchemeCreator implements ControlPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception {
        ColorScheme currentColorScheme = (ColorScheme) getter.invoke(obj);
        ColorScheme[] colorSchemes = {currentColorScheme, DefaultColorScheme.LIGHT.get(), DefaultColorScheme.DARK.get()};
        String[] rendererStrings = {"Default", "Light", "Dark"};

        JComboBox<String> rendererSelect = new JComboBox<>(rendererStrings);
        rendererSelect.setMaximumSize(rendererSelect.getPreferredSize());
        rendererSelect.setSelectedItem(rendererStrings[0]);

        rendererSelect.addItemListener(e -> {
            ColorScheme selected = colorSchemes[rendererSelect.getSelectedIndex()];
            try {
                setter.invoke(obj, selected);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
            canvas.scheduleRepaint();
        });

        panelContainer.add(rendererSelect);
        return panelContainer;
    }
}
