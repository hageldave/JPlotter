package hageldave.jplotter.debugging.controlHandler.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

public class RenderableDetailsCreator implements DisplayPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter) throws Exception {
        try {
            Object fieldValue = getter.invoke(obj);

            if (Objects.nonNull(fieldValue)) {
                if (List.class.isAssignableFrom(fieldValue.getClass())) {
                    List<?> list = (List<?>) fieldValue;
                    JLabel elements = new JLabel(((Collection<?>) fieldValue).size() + " element(s)");
                    if (list.size() > 0) {
                        Field[] fields = list.get(0).getClass().getFields();
                        String[] fieldNames = Arrays.stream(fields).map(e -> "(" + e.getType().getSimpleName() + ") " + e.getName()).toArray(String[]::new);
                        elements.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                        elements.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                super.mouseClicked(e);
                                Object[][] data = new Object[list.size()][fieldNames.length];

                                int rowIndex = 0;
                                for (Object o : list) {
                                    int columnindex = 0;
                                    for (Field f : o.getClass().getFields()) {
                                        try {
                                            Class<?> fieldClass = f.get(o).getClass();
                                            Object value = f.get(o);
                                            if (DoubleSupplier.class.isAssignableFrom(fieldClass)) {
                                                double val = ((DoubleSupplier) value).getAsDouble();
                                                data[rowIndex][columnindex] = val;
                                            } else if (IntSupplier.class.isAssignableFrom(fieldClass)) {
                                                int val = ((IntSupplier) value).getAsInt();
                                                data[rowIndex][columnindex] = val;
                                            } else if (Color.class.isAssignableFrom(fieldClass)) {
                                                Color c = ((Color) value);
                                                String colorString = c.getRed() + " " + c.getGreen() + " " + c.getBlue() + " " + c.getAlpha();
                                                data[rowIndex][columnindex] = colorString;
                                            } else {
                                                data[rowIndex][columnindex] = value;
                                            }
                                        } catch (IllegalAccessException ex) {
                                            throw new RuntimeException(ex);
                                        }
                                        columnindex++;
                                    }
                                    rowIndex++;
                                }
                                JTable table = new JTable(data, fieldNames);
                                JFrame frame = new JFrame("Renderable Details");
                                frame.setPreferredSize(new Dimension(1000, 450));
                                frame.setLayout(new BorderLayout());
                                frame.add(new JScrollPane(table));
                                frame.pack();
                                frame.setVisible(true);
                            }
                        });
                    }
                    panelContainer.add(elements);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return panelContainer;
    }
}
