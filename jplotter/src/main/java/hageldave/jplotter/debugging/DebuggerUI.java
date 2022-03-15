package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Text;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected Container informationContainer = new Container();
    final protected JPlotterCanvas canvas;

    public DebuggerUI(JPlotterCanvas canvas) {
        this.canvas = canvas;
        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                try {
                    onMouseClick(mouseEvent);
                } catch (NoSuchFieldException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void display() throws IllegalAccessException, ClassNotFoundException {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set constructed tree as tree model
        DefaultTreeModel treeModel = new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas));
        tree.setModel(treeModel);

        Container c = new Container();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.add(new JScrollPane(tree));

        informationContainer.setLayout(new BoxLayout(informationContainer, BoxLayout.Y_AXIS));
        informationContainer.add(new JLabel("Control Area"));
        c.add(informationContainer);

        frame.getContentPane().add(c);
        frame.pack();
        frame.setVisible(true);
    }

    protected void clearInformation() {
        informationContainer.removeAll();
    }

    protected void fillInformation(Field[] fields, Object obj) throws IllegalAccessException {
        for (Field field : fields) {
            field.setAccessible(true);

            Container labelContainer = new Container();
            labelContainer.setLayout(new FlowLayout());

            Object fieldValue = field.get(obj);
            if (fieldValue != null) {
                labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
                labelContainer.add(new JLabel((field.getName()) + ": "));
                labelContainer.add(new JLabel(String.valueOf(fieldValue)));
            }

            informationContainer.add(labelContainer);
        }
        frame.revalidate();
        frame.repaint();
    }

    protected void onMouseClick(MouseEvent mouseEvent) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        clearInformation();
        TreePath tp = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
        if (tp != null) {
            Class<?> class1 = tp.getLastPathComponent().getClass();
            Method method = class1.getMethod("getUserObject");
            Object obj = method.invoke(tp.getLastPathComponent());
            Field[] fields = obj.getClass().getDeclaredFields();

            // TODO: testing connection between debugger ui & canvas
            if (Text.class.isAssignableFrom(obj.getClass())) {
                Text txtobj = (Text) obj;
                txtobj.setTextString("new Text!");
            }

            fillInformation(fields, obj);
            canvas.scheduleRepaint();
        }
    }
}
