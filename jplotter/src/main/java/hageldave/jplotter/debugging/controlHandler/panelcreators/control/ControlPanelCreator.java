package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.Method;

public interface ControlPanelCreator {
    JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception;

    default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) {
        try {
            return create(canvas, obj, panelContainer,  setter, getter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
