package hageldave.jplotter.debugging.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.customPrint.CustomPrinterInterface;

import javax.swing.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public class StandardPanelCreator implements DisplayPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter, CustomPrinterInterface objectPrinter) throws Exception {
        try {
            Object fieldValue = getter.invoke(obj);

            if (Objects.nonNull(fieldValue)) {
                if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                    panelContainer.add(new JLabel(String.valueOf(((DoubleSupplier) fieldValue).getAsDouble())));
                } else if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    panelContainer.add(new JLabel(((Collection<?>) fieldValue).size() + " element(s)"));
                } else if (fieldValue.getClass().isArray()) {
                    int arrLen = Array.getLength(fieldValue);
                    panelContainer.add(new JLabel(String.valueOf(arrLen)));
                } else {
                    panelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return panelContainer;
    }
}
