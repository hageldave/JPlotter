package hageldave.jplotter.debugging.controlHandler.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugGetter;
import hageldave.jplotter.debugging.controlHandler.customPrint.CustomPrinterInterface;

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
    JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter, CustomPrinterInterface objectPrinter) throws Exception;

    default JPanel createUnchecked(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter, CustomPrinterInterface objectPrinter) {
        try {
            return create(canvas, obj, panelContainer, getter, objectPrinter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
