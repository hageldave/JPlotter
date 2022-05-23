package hageldave.jplotter.debugging.controlHandler.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public class StandardPanelCreator implements DisplayPanelCreator {

    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel labelContainer, Method getter) throws Exception {
        try {
            Object fieldValue = getter.invoke(obj);

            if (Objects.nonNull(fieldValue)) {
                if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                    labelContainer.add(new JLabel(String.valueOf(((DoubleSupplier) fieldValue).getAsDouble())));
                } else if (Collection.class.isAssignableFrom(fieldValue.getClass())) {
                    labelContainer.add(new JLabel(((Collection<?>) fieldValue).size() + " element(s)"));
                } else if (fieldValue.getClass().isArray()) {
                    int arrLen = Array.getLength(fieldValue);
                    labelContainer.add(new JLabel(String.valueOf(arrLen)));
                } else {
                    labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return labelContainer;
    }
}
