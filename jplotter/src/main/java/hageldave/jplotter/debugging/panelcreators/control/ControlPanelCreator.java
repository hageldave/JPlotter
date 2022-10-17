package hageldave.jplotter.debugging.panelcreators.control;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.annotations.DebugGetter;
import hageldave.jplotter.debugging.annotations.DebugSetter;

import javax.swing.*;
import java.lang.reflect.Method;

/**
 * The ControlPanelCreator interface provides a method to create a debugger panel
 * that shows certain information of the property of an element and provides an easy way to manipulate the property.
 * Each ControlPanelCreator has to be "registered", which means that the property (its getter and setter methods to be more specific) that should be
 * shown in the debugger has to be annotated with the {@link DebugGetter} and the {@link DebugSetter} annotations.
 */
public interface ControlPanelCreator {

    /**
     * This interface method is used to display
     * all the panel creators content (buttons, sliders, ...).
     * The content will be displayed in the "Manipulate object properties" section of the respective object.
     *
     * @param canvas the content of this canvas is displayed by the debugger
     * @param obj the properties of this (selected) object will be displayed by the panel
     * @param panelContainer the wrapper container which the panel will be placed in
     * @param getter this getter method of the "obj" object will be used to retrieve the information
     * @param setter this setter method of the "obj" object will be used to change the objects properties
     * @return panelContainer
     * @throws Exception reflection errors
     */
    JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) throws Exception;

    /**
     * Wrapping method to catch exceptions in the {@link #create(JPlotterCanvas, Object, JPanel, Method, Method)} method immediately.
     *
     * @param canvas the content of this canvas is displayed by the debugger
     * @param obj the properties of this (selected) object will be displayed by the panel
     * @param panelContainer the wrapper container which the panel will be placed in
     * @param getter this getter method of the "obj" object will be used to retrieve the information
     * @param setter this setter method of the "obj" object will be used to change the objects properties
     * @return panelContainer
     */
    default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method setter, Method getter) {
        try {
            return create(canvas, obj, panelContainer,  setter, getter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
