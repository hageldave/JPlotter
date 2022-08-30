package hageldave.jplotter.debugging.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.annotations.DebugGetter;
import hageldave.jplotter.debugging.customPrint.CustomPrinterInterface;

import javax.swing.*;
import java.lang.reflect.Method;

/**
 * The DisplayPanelCreator interface provides a method to create a debugger panel
 * that shows certain information of the property of an element.
 *
 * Each DisplayPanelCreator has to be "registered", which means that the property (its getter method to be more specific) that should be
 * shown in the debugger has to be annotated with the {@link DebugGetter} annotation.
 *
 */
public interface DisplayPanelCreator {
    /**
     * This interface method is used to display
     * all the panel creators content (labels, buttons, ...).
     * The content will be displayed in the "Additional information" section of the respective object.
     *
     * @param canvas the content of this canvas is displayed by the debugger
     * @param obj the properties of this (selected) object will be displayed by the panel
     * @param panelContainer the wrapper container which the panel will be placed in
     * @param getter this getter method of the "obj" object will be used to retrieve the information
     * @param objectPrinter the objectPrinter can be used to display nested information of complex objects (see {@link CustomPrinterInterface})
     * @return panelContainer
     * @throws Exception reflection errors
     */
    JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter, CustomPrinterInterface objectPrinter) throws Exception;

    /**
     * Wrapping method to catch exceptions in the {@link #create(JPlotterCanvas, Object, JPanel, Method, CustomPrinterInterface)} method immediately.
     *
     * @param canvas the content of this canvas is displayed by the debugger
     * @param obj the properties of this (selected) object will be displayed by the panel
     * @param panelContainer the wrapper container which the panel will be placed in
     * @param getter this getter method of the "obj" object will be used to retrieve the information
     * @param objectPrinter the objectPrinter can be used to display nested information of complex objects (see {@link CustomPrinterInterface})
     * @return panelContainer
     */
    default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter, CustomPrinterInterface objectPrinter) {
        try {
            return create(canvas, obj, panelContainer, getter, objectPrinter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
