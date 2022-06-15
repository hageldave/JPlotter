package hageldave.jplotter.debugging.controlHandler.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.customPrint.CustomPrinterInterface;

import javax.swing.*;
import java.lang.reflect.Method;

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
