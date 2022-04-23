package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.FieldHandler;
import hageldave.jplotter.debugging.controlHandler.renderableFields.RenderableFields;
import hageldave.jplotter.debugging.controlHandler.rendererFields.RendererFields;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

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
import java.util.Objects;

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected JPanel controlContainer = new JPanel();
    final protected JPanel infoContainer = new JPanel();

    final protected JLabel title = new JLabel();
    final protected JLabel controlHeader = new JLabel();
    final protected JLabel infoHeader = new JLabel();

    final protected FieldHandler renFHandler = new FieldHandler();

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

    public void display() throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set constructed tree as tree model
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));

        // register components here
        registerInternalPanelCreators();

        // start title container
        JPanel titleArea = new JPanel();
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, 17));
        titleArea.add(title);

        // start control container
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.Y_AXIS));
        controlContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlHeader.setFont(new Font(controlHeader.getFont().getName(), Font.PLAIN, 14));

        JPanel controlArea = new JPanel();
        controlArea.setLayout(new BoxLayout(controlArea, BoxLayout.Y_AXIS));
        controlArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlArea.add(controlHeader);
        controlArea.add(controlContainer);

        // start info container
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.Y_AXIS));
        infoContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        infoHeader.setFont(new Font(infoHeader.getFont().getName(), Font.PLAIN, 14));

        JPanel infoArea = new JPanel();
        infoArea.setLayout(new BoxLayout(infoArea, BoxLayout.Y_AXIS));
        infoArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoArea.add(infoHeader);
        infoArea.add(infoContainer);

        JPanel infoControlWrap = new JPanel();
        infoControlWrap.setLayout(new BoxLayout(infoControlWrap, BoxLayout.PAGE_AXIS));
        infoControlWrap.add(titleArea);
        infoControlWrap.add(controlArea);
        infoControlWrap.add(infoArea);

        JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setRightComponent(new JScrollPane(infoControlWrap));
        splitpane.setLeftComponent(new JScrollPane(tree));

        frame.getContentPane().add(splitpane);
        frame.setPreferredSize(new Dimension(700, 450));
        frame.pack();
        frame.setVisible(true);

        splitpane.setDividerLocation(1.0/3);
    }

    protected void createEmptyMessage() {
        title.setText("No renderer or renderable selected.");
    }

    protected void clearGUIContents() {
        title.setText("");
        controlHeader.setText("");
        infoHeader.setText("");
        controlContainer.removeAll();
        infoContainer.removeAll();
    }

    protected void handleObjectFields(Object obj) throws IllegalAccessException {
        HashSet<Field> fieldSet = new HashSet<>(Utils.getReflectionFields(obj.getClass()));

        title.setText(obj.getClass().getSimpleName());

        controlHeader.setText("Control Area");
        infoHeader.setText("Other Information");

        for (Field field : fieldSet) {
            field.setAccessible(true);
            JPanel panel = renFHandler.handleField(canvas, obj, field);

            if (FieldHandler.displayInControlArea(field.getName())) {
                controlContainer.add(panel);
            } else if (FieldHandler.displayInInformationArea(field.getName())) {
                infoContainer.add(panel);
            }
        }
        frame.repaint();
    }

    private void registerInternalPanelCreators() {
        this.renFHandler.registerPanelCreator(
                "isEnabled", RendererFields::createIsEnabledUIElements);
        this.renFHandler.registerPanelCreator(
                "hidden", RenderableFields::createHideUIRenderableElements);
        this.renFHandler.registerPanelCreator(
                "globalThicknessMultiplier", RenderableFields::createGlobalThicknessMultiplierUIElements);
        this.renFHandler.registerPanelCreator(
                "globalSaturationMultiplier", RenderableFields::createGlobalSaturationMultiplierUIElements);
        this.renFHandler.registerPanelCreator(
                "globalAlphaMultiplier", RenderableFields::createGlobalAlphaMultiplierUIElements);
        this.renFHandler.registerPanelCreator(
                "angle", RenderableFields::createAngleUIRenderableElements);
        this.renFHandler.registerPanelCreator(
                "txtStr", RenderableFields::createTextStrUIElements);
    }

    public void registerPanelCreator(String fieldName, FieldHandler.PanelCreator c) {
        this.renFHandler.registerPanelCreator(fieldName, c);
    }

    protected void onMouseClick(MouseEvent mouseEvent) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TreePath tp = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());

        if (Objects.nonNull(tp)) {
            Class<?> clickedClass = tp.getLastPathComponent().getClass();

            Method method;
            if (Arrays.stream(clickedClass.getMethods()).anyMatch(e->e.getName().equals("getHiddenObject"))) {
                method = clickedClass.getMethod("getHiddenObject");
            } else {
                method = clickedClass.getMethod("getUserObject");
            }
            Object obj = method.invoke(tp.getLastPathComponent());

            clearGUIContents();
            if (Renderer.class.isAssignableFrom(obj.getClass()) || Renderable.class.isAssignableFrom(obj.getClass())) {
                this.handleObjectFields(obj);
                canvas.scheduleRepaint();
            } else {
                createEmptyMessage();
            }
        }
    }

    public void refresh() throws IllegalAccessException {
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));
    }
}
