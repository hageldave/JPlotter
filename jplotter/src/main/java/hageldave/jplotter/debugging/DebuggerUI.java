package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.FieldHandler;
import hageldave.jplotter.debugging.controlHandler.PanelCreator;
import hageldave.jplotter.debugging.controlHandler.annotations.CreateElement;
import hageldave.jplotter.debugging.controlHandler.annotations.CreateElementGet;
import hageldave.jplotter.debugging.controlHandler.annotations.CreateElementSet;
import hageldave.jplotter.debugging.controlHandler.annotations.DisplayField;
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
import java.util.concurrent.atomic.AtomicReference;

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected Color bgColor = new Color(225, 225, 225);
    final protected JPanel controlBorderWrap = new JPanel();
    final protected JPanel controlArea = new JPanel();
    final protected JPanel controlContainer = new JPanel();
    final protected JPanel infoBorderWrap = new JPanel();

    final protected JPanel infoArea = new JPanel();
    final protected JPanel infoContainer = new JPanel();
    final protected JPanel infoControlWrap = new JPanel();

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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set constructed tree as tree model
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));

        // register components here
        // registerInternalPanelCreators();

        // start title container
        JPanel titleArea = new JPanel();
        titleArea.setLayout(new GridLayout(1, 0));
        titleArea.setBorder(new EmptyBorder(10, 0, 10, 0));
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, 18));
        titleArea.add(title);

        // start control container
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.Y_AXIS));
        controlContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlHeader.setFont(new Font(controlHeader.getFont().getName(), Font.PLAIN, 16));

        controlBorderWrap.setLayout(new BorderLayout());
        controlArea.setLayout(new BorderLayout());
        controlArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        controlArea.add(controlHeader, BorderLayout.NORTH);
        controlArea.add(controlContainer, BorderLayout.CENTER);
        controlBorderWrap.add(controlArea, BorderLayout.CENTER);

        // start info container
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.Y_AXIS));
        infoContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        infoHeader.setFont(new Font(infoHeader.getFont().getName(), Font.PLAIN, 16));

        infoBorderWrap.setLayout(new BorderLayout());
        infoArea.setLayout(new BorderLayout());
        infoArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        infoArea.add(infoHeader, BorderLayout.NORTH);
        infoArea.add(infoContainer, BorderLayout.CENTER);
        infoBorderWrap.add(infoArea, BorderLayout.CENTER);

        // create separator
        JPanel separator = new JPanel();

        //JPanel infoControlWrap = new JPanel();
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

        splitpane.setDividerLocation(2.0 / 5);
    }

    protected void createEmptyMessage() {
        controlContainer.setBackground(infoControlWrap.getBackground());
        controlArea.setBackground(infoControlWrap.getBackground());
        controlBorderWrap.setBorder(new LineBorder(infoControlWrap.getBackground()));

        infoContainer.setBackground(infoControlWrap.getBackground());
        infoBorderWrap.setBorder(new LineBorder(infoControlWrap.getBackground()));
        infoArea.setBackground(infoControlWrap.getBackground());

        title.setText("No renderer or renderable selected.");
    }

    protected void fillContent(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        title.setText(obj.getClass().getSimpleName());

        controlHeader.setText("Control Area");
        infoHeader.setText("Other Information");

        controlContainer.setBackground(bgColor);
        controlArea.setBackground(bgColor);
        controlBorderWrap.setBorder(new LineBorder(Color.LIGHT_GRAY));

        infoContainer.setBackground(bgColor);
        infoArea.setBackground(bgColor);
        infoBorderWrap.setBorder(new LineBorder(Color.LIGHT_GRAY));

        handleObjectFields(obj);
    }

    protected void clearGUIContents() {
        title.setText("");
        controlHeader.setText("");
        infoHeader.setText("");
        controlContainer.removeAll();
        infoContainer.removeAll();
    }

    protected void handleObjectFields(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        HashSet<Field> fieldSet = new HashSet<>(Utils.getReflectionFields(obj.getClass()));

        for (Field field : fieldSet) {
            field.setAccessible(true);
            if (field.getAnnotationsByType(DisplayField.class).length > 0) {
                JPanel panel = renFHandler.handleField(canvas, obj, field);
                infoContainer.add(panel);
            }

            AtomicReference<String> key = new AtomicReference<>();
            AtomicReference<Method> getter = new AtomicReference<>();
            AtomicReference<Method> setter = new AtomicReference<>();
            AtomicReference<Class<? extends PanelCreator>> creator = new AtomicReference<>();

            CreateElement[] createBtnMethods = field.getAnnotationsByType(CreateElement.class);
            for (CreateElement btnMethod : createBtnMethods) {
                key.set(btnMethod.key());
                creator.set(btnMethod.creator());
            }

            Arrays.stream(obj.getClass().getMethods()).forEach(e -> {
                for (CreateElementGet btnMethod : e.getAnnotationsByType(CreateElementGet.class)) {
                    if (Objects.equals(btnMethod.key(), key.get())) {
                        getter.set(e);
                    }
                }

                for (CreateElementSet btnMethod : e.getAnnotationsByType(CreateElementSet.class)) {
                    if (Objects.equals(btnMethod.key(), key.get())) {
                        setter.set(e);
                    }
                }
            });

            if (key.get() != null && getter.get() != null && setter.get() != null) {
                JPanel labelContainer = new JPanel();
                labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.X_AXIS));
                labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
                labelContainer.setBorder(new EmptyBorder(9, 0, 9, 0));
                labelContainer.setBackground(new Color(225, 225, 225));

                labelContainer.add(new JLabel(("(" + field.getType().getSimpleName()) + ") "));
                JLabel fieldName = new JLabel((field.getName()) + ": ");
                fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
                labelContainer.add(fieldName);

                PanelCreator pc = creator.get().getDeclaredConstructor().newInstance();
                controlContainer.add(pc.createUnchecked(canvas, obj, labelContainer, setter.get(), getter.get()));
            }

            /*if (FieldHandler.displayInControlArea(field.getName())) {
                JPanel panel = renFHandler.handleField(canvas, obj, field);
                controlContainer.add(panel);
            }*/
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
                if (Arrays.stream(lastSelectedComponentClass.getMethods()).anyMatch(toSearch -> toSearch.getName().equals("getHiddenObject"))) {
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
                        fillContent(obj);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    } catch (InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    } catch (NoSuchMethodException | InstantiationException ex) {
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
