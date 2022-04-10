package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.RenderableFieldHandler;
import hageldave.jplotter.debugging.controlHandler.RendererFieldHandler;
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

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected JPanel controlContainer = new JPanel();
    final protected JPanel infoContainer = new JPanel();
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

        // start control container
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.PAGE_AXIS));
        controlContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel controlHeader = new JLabel("Control Area");
        controlHeader.setFont(new Font(controlHeader.getFont().getName(), Font.PLAIN, 14));

        JPanel controlArea = new JPanel();
        controlArea.setLayout(new BoxLayout(controlArea, BoxLayout.PAGE_AXIS));
        controlArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlArea.add(controlHeader);
        controlArea.add(controlContainer);

        // start info container
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.PAGE_AXIS));
        infoContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        infoContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoHeader = new JLabel("Information Area");
        infoHeader.setFont(new Font(infoHeader.getFont().getName(), Font.PLAIN, 14));

        JPanel infoArea = new JPanel();
        infoArea.setLayout(new BoxLayout(infoArea, BoxLayout.PAGE_AXIS));
        infoArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoArea.add(infoHeader);
        infoArea.add(infoContainer);

        JPanel infoControlWrap = new JPanel();
        infoControlWrap.setLayout(new BoxLayout(infoControlWrap, BoxLayout.PAGE_AXIS));
        infoControlWrap.add(controlArea);
        infoControlWrap.add(infoArea);

        JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setRightComponent(new JScrollPane(infoControlWrap));
        splitpane.setLeftComponent(new JScrollPane(tree));

        frame.getContentPane().add(splitpane);
        frame.pack();
        frame.setVisible(true);
    }

    protected void clearInformation() {
        controlContainer.removeAll();
        infoContainer.removeAll();
    }

    protected void handleRenderer(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        HashSet<Field> set = new HashSet<>();
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
            JPanel panel = RendererFieldHandler.handleRendererField(canvas, obj, field);

            if (RendererFieldHandler.displayInControlArea(field.getName()))
                controlContainer.add(panel);
            else
                infoContainer.add(panel);
        }
        frame.revalidate();
        frame.repaint();
    }

    protected void handleRenderable(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        HashSet<Field> set = new HashSet<>();
        set.addAll(Arrays.asList(obj.getClass().getFields()));
        set.addAll(Arrays.asList(obj.getClass().getDeclaredFields()));
        Field[] fields = set.toArray(new Field[0]);

        for (Field field : fields) {
            field.setAccessible(true);
            JPanel panel = RenderableFieldHandler.handleRenderableField(canvas, obj, field);

            if (RenderableFieldHandler.displayInControlArea(field.getName()))
                controlContainer.add(panel);
            else
                infoContainer.add(panel);
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
            if (Arrays.stream(class1.getMethods()).anyMatch(e->e.getName().equals("getHiddenObject"))) {
                method = class1.getMethod("getHiddenObject");
            } else {
                method = class1.getMethod("getUserObject");
            }
            Object obj = method.invoke(tp.getLastPathComponent());

            if (Renderer.class.isAssignableFrom(obj.getClass())) {
                handleRenderer(obj);
            } else if (Renderable.class.isAssignableFrom(obj.getClass())) {
                handleRenderable(obj);
            }
            canvas.scheduleRepaint();
        }
    }
}
