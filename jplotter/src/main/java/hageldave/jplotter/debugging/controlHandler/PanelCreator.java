package hageldave.jplotter.debugging.controlHandler;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.Method;

public interface PanelCreator {
    JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) throws Exception;

    default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method setter, Method getter) {
        try {
            return create(canvas, obj, labelContainer,  setter, getter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
