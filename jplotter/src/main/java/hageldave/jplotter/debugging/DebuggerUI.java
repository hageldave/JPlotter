package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.FieldHandler;
import hageldave.jplotter.debugging.controlHandler.renderableFields.RenderableFields;
import hageldave.jplotter.debugging.controlHandler.rendererFields.RendererFields;
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

    // TODO: improve fieldHandler management, how can the developer register its own components?
    FieldHandler renFHandler = new FieldHandler();

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
        registerInternalComponents();

        // start title container
        JPanel titleArea = new JPanel();
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.PAGE_AXIS));
        titleArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, 17));
        titleArea.add(title);

        // start control container
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.PAGE_AXIS));
        controlContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        infoHeader.setFont(new Font(infoHeader.getFont().getName(), Font.PLAIN, 14));

        JPanel infoArea = new JPanel();
        infoArea.setLayout(new BoxLayout(infoArea, BoxLayout.PAGE_AXIS));
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
        frame.pack();
        frame.setVisible(true);
    }

    protected void clearInformation() {
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
        infoHeader.setText("Information Area");

        for (Field field : fieldSet) {
            field.setAccessible(true);
            JPanel panel = renFHandler.handleField(canvas, obj, field);

            if (FieldHandler.displayInControlArea(field.getName())) {
                controlContainer.add(panel);
            } else if (FieldHandler.displayInInformationArea(field.getName())) {
                infoContainer.add(panel);
            }
        }
        frame.revalidate();
        frame.repaint();
    }

    protected void registerInternalComponents() {
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

    protected void onMouseClick(MouseEvent mouseEvent) throws NoSuchFieldException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        clearInformation();
        TreePath tp = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());

        Method method;
        if (Objects.nonNull(tp)) {
            Class<?> class1 = tp.getLastPathComponent().getClass();
            if (Arrays.stream(class1.getMethods()).anyMatch(e->e.getName().equals("getHiddenObject"))) {
                method = class1.getMethod("getHiddenObject");
            } else {
                method = class1.getMethod("getUserObject");
            }
            Object obj = method.invoke(tp.getLastPathComponent());

            this.handleObjectFields(obj);
            canvas.scheduleRepaint();
        }
    }

    public void refresh() throws IllegalAccessException {
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));
    }
}
