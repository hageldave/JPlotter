package hageldave.jplotter.debugging.controlHandler.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.Method;

public interface DisplayPanelCreator {
    JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter) throws Exception;

    default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter) {
        try {
            return create(canvas, obj, panelContainer, getter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
