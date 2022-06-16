package hageldave.jplotter.debugging.controlHandler.panelcreators.display;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.customPrint.CustomPrinterInterface;

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
import java.util.stream.Collectors;

public class RenderableDetailsCreator implements DisplayPanelCreator {
    @Override
    public JPanel create(JPlotterCanvas canvas, Object obj, JPanel panelContainer, Method getter, CustomPrinterInterface objectPrinter) throws Exception {
        try {
            Object fieldValue = getter.invoke(obj);

            if (Objects.nonNull(fieldValue)) {
                if (List.class.isAssignableFrom(fieldValue.getClass())) {
                    List<?> list = (List<?>) fieldValue;
                    JLabel elements = new JLabel(((Collection<?>) fieldValue).size() + " element(s)");

                    if (list.size() > 0) {
                        elements = new JLabel("<HTML><U>" + ((Collection<?>) fieldValue).size() + " element(s)" + "</U></HTML>");

                        Field[] fields = list.get(0).getClass().getFields();

                        List<String> fieldNameList = Arrays.stream(fields).map(e -> "(" + e.getType().getSimpleName() + ") " + e.getName()).collect(Collectors.toList());
                        fieldNameList.add(0, "Object Hashcode");
                        String[] fieldNames = fieldNameList.toArray(new String[0]);

                        elements.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        elements.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                super.mouseClicked(e);
                                Object[][] data = new Object[list.size()][fieldNames.length+1];

                                int rowIndex = 0;
                                for (Object o : list) {
                                    int columnindex = 1;
                                    data[rowIndex][0] = o.hashCode();
                                    for (Field f : o.getClass().getFields()) {
                                        try {
                                            Object value = f.get(o);
                                            data[rowIndex][columnindex] = objectPrinter.print(value, f.getName());
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
