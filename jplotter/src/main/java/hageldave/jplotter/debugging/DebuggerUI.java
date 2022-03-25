package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.RendererHandler;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.GenericRenderer;
import hageldave.jplotter.renderers.Renderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.DoubleSupplier;

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected JPanel informationContainer = new JPanel();
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

        informationContainer.setLayout(new BoxLayout(informationContainer, BoxLayout.PAGE_AXIS));
        informationContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        informationContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel("Control Area");
        header.setFont(new Font(header.getFont().getName(), Font.PLAIN, 14));

        JPanel controlArea = new JPanel();
        controlArea.setLayout(new BoxLayout(controlArea, BoxLayout.PAGE_AXIS));
        controlArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlArea.add(header);
        controlArea.add(informationContainer);

        JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitpane.setBottomComponent(new JScrollPane(controlArea));
        splitpane.setTopComponent(new JScrollPane(tree));

        frame.getContentPane().add(splitpane);
        frame.pack();
        frame.setVisible(true);
    }

    protected void clearInformation() {
        informationContainer.removeAll();
    }


    protected void fillInformation(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (Renderer.class.isAssignableFrom(obj.getClass())) {
            handleRenderer(obj);
        } else if (Renderable.class.isAssignableFrom(obj.getClass())) {
            handleRenderable(obj);
        }
    }

    protected void handleRenderer(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        HashSet<Field> set = new HashSet<>();

        // superclass generic Renderer?
        if (GenericRenderer.class.isAssignableFrom(obj.getClass())) {
            set.addAll(Arrays.asList(obj.getClass().getDeclaredFields()));
            set.addAll(Arrays.asList(obj.getClass().getSuperclass().getDeclaredFields()));
        } else {
            set.addAll(Arrays.asList(obj.getClass().getFields()));
            set.addAll(Arrays.asList(obj.getClass().getDeclaredFields()));
        }

        Field[] fields = set.toArray(new Field[0]);

        for (Field field : fields) {
            field.setAccessible(true);
            JPanel panel = RendererHandler.handleRendererField(canvas, obj, field);
            informationContainer.add(panel);
        }
        frame.revalidate();
        frame.repaint();
    }

    protected void handleRenderable(Object obj) throws IllegalAccessException {
        HashSet<Field> set = new HashSet<>();
        set.addAll(Arrays.asList(obj.getClass().getFields()));
        set.addAll(Arrays.asList(obj.getClass().getDeclaredFields()));
        Field[] fields = set.toArray(new Field[0]);

        for (Field field : fields) {
            field.setAccessible(true);
            JPanel labelContainer = new JPanel();
            labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.LINE_AXIS));
            labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

            Object fieldValue = field.get(obj);
            if (fieldValue != null) {
                labelContainer.add(new JLabel(("(" + field.getType()) + ") "));
                labelContainer.add(new JLabel((field.getName()) + ": "));
                if (DoubleSupplier.class.isAssignableFrom(fieldValue.getClass())) {
                    DoubleSupplier dSup = (DoubleSupplier) fieldValue;
                    labelContainer.add(new JLabel(String.valueOf(dSup.getAsDouble())));
                } else {
                    labelContainer.add(new JLabel(String.valueOf(fieldValue)));
                }
            }
            informationContainer.add(labelContainer);
        }
        frame.revalidate();
        frame.repaint();
    }

    protected void onMouseClick(MouseEvent mouseEvent) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        clearInformation();
        TreePath tp = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());

        Method method;
        if (tp != null) {
            Class<?> class1 = tp.getLastPathComponent().getClass();
            if (Arrays.stream(class1.getMethods()).anyMatch(e->e.getName().equals("getBackObject"))) {
                method = class1.getMethod("getBackObject");
            } else {
                method = class1.getMethod("getUserObject");
            }
            Object obj = method.invoke(tp.getLastPathComponent());

            if (Renderer.class.isAssignableFrom(obj.getClass()) || Renderable.class.isAssignableFrom(obj.getClass())) {
                fillInformation(obj);
                canvas.scheduleRepaint();
            }
        }
    }
}
