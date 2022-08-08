package hageldave.jplotter.debugging.controlHandler.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugGetter;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugSetter;

import javax.swing.*;
import java.lang.reflect.Method;

/**
 * The ControlPanelCreator interface provides a method to create a debugger panel
 * that shows certain information of the property of an element and provides an easy way to manipulate the property.
 *
 * Each ControlPanelCreator has to be "registered", which means that the property (its getter and setter methods to be more specific) that should be
 * shown in the debugger has to be annotated with the {@link DebugGetter} and the {@link DebugSetter} annotations.
 *
 */
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
