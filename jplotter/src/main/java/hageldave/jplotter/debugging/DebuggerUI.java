package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.FieldHandler;
import hageldave.jplotter.debugging.controlHandler.renderableFields.LinesFields;
import hageldave.jplotter.debugging.controlHandler.renderableFields.RenderableFields;
import hageldave.jplotter.debugging.controlHandler.rendererFields.CoordSysRendererFields;
import hageldave.jplotter.debugging.controlHandler.rendererFields.RendererFields;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
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
        registerTreeListener();
    }

    public void display() throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        Color bgColor = new Color(225, 225, 225);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set constructed tree as tree model
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));

        // register components here
        registerInternalPanelCreators();

        // start title container
        JPanel titleArea = new JPanel();
        titleArea.setLayout(new GridLayout(1, 0));
        titleArea.setBorder(new EmptyBorder(10, 0, 10, 0));
        //titleArea.setBackground(secondBGColor);
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, 18));
        titleArea.add(title);

        // start control container
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.Y_AXIS));
        controlContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlContainer.setBackground(bgColor);
        controlHeader.setFont(new Font(controlHeader.getFont().getName(), Font.PLAIN, 16));

        JPanel controlBorderWrap = new JPanel();
        controlBorderWrap.setLayout(new BorderLayout());
        JPanel controlArea = new JPanel();
        controlArea.setLayout(new BorderLayout());
        controlArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlArea.setBackground(bgColor);
        controlArea.add(controlHeader, BorderLayout.NORTH);
        controlArea.add(controlContainer, BorderLayout.CENTER);
        controlBorderWrap.add(controlArea, BorderLayout.CENTER);
        controlBorderWrap.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // start info container
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.Y_AXIS));
        infoContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        infoContainer.setBackground(bgColor);
        infoHeader.setFont(new Font(infoHeader.getFont().getName(), Font.PLAIN, 16));

        JPanel infoBorderWrap = new JPanel();
        infoBorderWrap.setLayout(new BorderLayout());
        JPanel infoArea = new JPanel();
        infoArea.setLayout(new BorderLayout());
        infoArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoArea.setBackground(bgColor);
        infoArea.add(infoHeader, BorderLayout.NORTH);
        infoArea.add(infoContainer, BorderLayout.CENTER);
        infoBorderWrap.add(infoArea, BorderLayout.CENTER);
        infoBorderWrap.setBorder(new LineBorder(Color.LIGHT_GRAY));

        // create separator
        JPanel separator = new JPanel();

        JPanel infoControlWrap = new JPanel();
        infoControlWrap.setLayout(new BoxLayout(infoControlWrap, BoxLayout.PAGE_AXIS));
        infoControlWrap.setBorder(new EmptyBorder(0, 10, 10, 10));
        infoControlWrap.add(titleArea);
        infoControlWrap.add(controlBorderWrap);
        infoControlWrap.add(separator);
        infoControlWrap.add(infoBorderWrap);

        JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setRightComponent(new JScrollPane(infoControlWrap));
        splitpane.setLeftComponent(new JScrollPane(tree));

        frame.getContentPane().add(splitpane);
        frame.setPreferredSize(new Dimension(900, 500));
        frame.pack();
        frame.setVisible(true);

        splitpane.setDividerLocation(2.0/5);
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
        this.renFHandler.addPanelCreator(
                "isEnabled", RendererFields::createIsEnabledUIElements);
        this.renFHandler.addPanelCreator(
                "paddingLeft", CoordSysRendererFields::createPaddingLeftUIElements);
        this.renFHandler.addPanelCreator(
               "paddingRight", CoordSysRendererFields::createPaddingRightUIElements);
        this.renFHandler.addPanelCreator(
                "paddingTop", CoordSysRendererFields::createPaddingTopUIElements);
        this.renFHandler.addPanelCreator(
              "paddingBot", CoordSysRendererFields::createPaddingBotUIElements);
        this.renFHandler.addPanelCreator(
            "hidden", RenderableFields::createHideUIRenderableElements);
        this.renFHandler.addPanelCreator(
               "globalScaling", RenderableFields::createGlobalScalingMultiplierUIElements);
        this.renFHandler.addPanelCreator(
               "globalThicknessMultiplier", RenderableFields::createGlobalThicknessMultiplierUIElements);
        this.renFHandler.addPanelCreator(
                "globalSaturationMultiplier", RenderableFields::createGlobalSaturationMultiplierUIElements);
        this.renFHandler.addPanelCreator(
                "globalAlphaMultiplier", RenderableFields::createGlobalAlphaMultiplierUIElements);
        this.renFHandler.addPanelCreator(
                "angle", RenderableFields::createAngleUIRenderableElements);
        this.renFHandler.addPanelCreator(
                "strokeLength", LinesFields::createStrokeLengthUIElements);
        this.renFHandler.addPanelCreator(
               "strokePattern", LinesFields::createStrokePatternUIElements);
        this.renFHandler.addPanelCreator(
                "txtStr", RenderableFields::createTextStrUIElements);
    }

    // TODO: also let the developer specify the class name not just the field name
    public void registerPanelCreator(String fieldName, FieldHandler.PanelCreator c) {
        this.renFHandler.addPanelCreator(fieldName, c);
    }

    public void deregisterPanelCreator(String fieldName) {
        this.renFHandler.removePanelCreator(fieldName);
    }

    private void registerTreeListener() {
        tree.addTreeSelectionListener(e -> {
            if (Objects.nonNull(e)) {
                Object lastSelectedComponent = tree.getLastSelectedPathComponent();
                Class<?> lastSelectedComponentClass = lastSelectedComponent.getClass();

                Method method;
                if (Arrays.stream(lastSelectedComponentClass.getMethods()).anyMatch(toSearch->toSearch.getName().equals("getHiddenObject"))) {
                    try {
                        method = lastSelectedComponentClass.getMethod("getHiddenObject");
                    } catch (NoSuchMethodException ex) {
                        throw new RuntimeException(ex);
                    }
                } else {
                    try {
                        method = lastSelectedComponentClass.getMethod("getUserObject");
                    } catch (NoSuchMethodException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                Object obj;
                try {
                    obj = method.invoke(lastSelectedComponent);
                } catch (IllegalAccessException | InvocationTargetException ex) {
                    throw new RuntimeException(ex);
                }

                clearGUIContents();
                if (Renderer.class.isAssignableFrom(obj.getClass()) || Renderable.class.isAssignableFrom(obj.getClass())) {
                    try {
                        handleObjectFields(obj);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                    canvas.scheduleRepaint();
                } else {
                    createEmptyMessage();
                }
            }
        });
    }

    public void refresh() throws IllegalAccessException {
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));
    }
}
