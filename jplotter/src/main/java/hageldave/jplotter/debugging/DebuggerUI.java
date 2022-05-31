package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.FieldHandler;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugGetter;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugSetter;
import hageldave.jplotter.debugging.controlHandler.panelcreators.control.ControlPanelCreator;
import hageldave.jplotter.debugging.controlHandler.panelcreators.display.DisplayPanelCreator;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected Color bgColor = new Color(245, 245, 245);
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

    final protected JPlotterCanvas canvas;

    public DebuggerUI(JPlotterCanvas canvas) {
        this.canvas = canvas;
        registerTreeListener();
    }

    public void display() throws IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set constructed tree as tree model
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));
        tree.setBorder(new EmptyBorder(2, 5, 2, 5));

        // start title container
        JPanel titleArea = new JPanel();
        titleArea.setLayout(new GridLayout(1, 0));
        titleArea.setBorder(new EmptyBorder(10, 0, 10, 0));
        titleArea.setBackground(bgColor);

        title.setText("Select renderer or renderable.");
        title.setFont(new Font(title.getFont().getName(), Font.BOLD, 18));
        titleArea.add(title);

        // start control container
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.Y_AXIS));
        controlContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        controlHeader.setFont(new Font(controlHeader.getFont().getName(), Font.PLAIN, 16));
        controlHeader.setBorder(new EmptyBorder(0, 0, 9, 0));

        controlBorderWrap.setLayout(new BorderLayout());
        controlArea.setLayout(new BorderLayout());
        controlArea.setBorder(new EmptyBorder(10, 10, 0, 10));
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
        separator.setBackground(bgColor);

        infoControlWrap.setLayout(new BoxLayout(infoControlWrap, BoxLayout.PAGE_AXIS));
        infoControlWrap.setBackground(bgColor);
        infoControlWrap.setBorder(new EmptyBorder(0, 10, 10, 10));
        infoControlWrap.add(titleArea);
        infoControlWrap.add(controlBorderWrap);
        infoControlWrap.add(separator);
        infoControlWrap.add(infoBorderWrap);

        // "remove" background color if content is empty
        controlContainer.setBackground(infoControlWrap.getBackground());
        controlArea.setBackground(infoControlWrap.getBackground());
        controlBorderWrap.setBorder(new LineBorder(infoControlWrap.getBackground()));

        infoContainer.setBackground(infoControlWrap.getBackground());
        infoBorderWrap.setBorder(new LineBorder(infoControlWrap.getBackground()));
        infoArea.setBackground(infoControlWrap.getBackground());

        JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setRightComponent(new JScrollPane(infoControlWrap));
        splitpane.setLeftComponent(new JScrollPane(tree));

        frame.getContentPane().add(splitpane);
        frame.setPreferredSize(new Dimension(950, 550));
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

    protected void createEmptyInfoContMessage() {
        infoContainer.add(new JLabel("No annotated fields found."));
    }

    protected void createEmptyControlContMessage() {
        controlContainer.add(new JLabel("No manipulable fields found."));
    }

    protected void fillContent(Object obj) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        title.setText(obj.getClass().getSimpleName());

        controlHeader.setText("Manipulate object properties");
        infoHeader.setText("Additional information");

        controlContainer.setBackground(UIManager.getColor("Panel.background"));
        controlArea.setBackground(UIManager.getColor("Panel.background"));
        controlBorderWrap.setBorder(new LineBorder(Color.LIGHT_GRAY));

        infoArea.setBackground(UIManager.getColor("Panel.background"));
        infoContainer.setBackground(UIManager.getColor("Panel.background"));
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

    protected void handleObjectFields(Object obj) {
        List<Method> allMethods = Utils.getReflectionMethods(obj.getClass());

        AtomicBoolean infoFieldFound = new AtomicBoolean(false);
        AtomicBoolean controlFieldFound = new AtomicBoolean(false);
        allMethods.forEach(searchGetter -> {
            AtomicReference<String> key = new AtomicReference<>();
            AtomicReference<Method> getter = new AtomicReference<>();
            AtomicReference<Method> setter = new AtomicReference<>();
            AtomicReference<Class<? extends ControlPanelCreator>> ctrlCreator = new AtomicReference<>();
            AtomicReference<Class<? extends DisplayPanelCreator>> dsplyCreator = new AtomicReference<>();

            for (DebugGetter debugGetter : searchGetter.getAnnotationsByType(DebugGetter.class)) {
                key.set(debugGetter.key());
                getter.set(searchGetter);
                dsplyCreator.set(debugGetter.creator());

                allMethods.forEach(searchSetter -> {
                    for (DebugSetter debugSetter : searchSetter.getAnnotationsByType(DebugSetter.class)) {
                        if (Objects.equals(debugSetter.key(), key.get())) {
                            if (Objects.nonNull(setter.get()))
                                throw new RuntimeException("Annotation key for @DebugSetter used more than once");

                            setter.set(searchSetter);
                            ctrlCreator.set(debugSetter.creator());

                        }
                    }
                });
            }

            if (Objects.nonNull(key.get()) && Objects.nonNull(getter.get()) && Objects.nonNull(setter.get()) && Objects.nonNull(ctrlCreator.get())) {
                JPanel panel = FieldHandler.controlField(canvas, obj, key.get(), getter, setter, ctrlCreator);
                controlContainer.add(panel);
                controlFieldFound.set(true);
            } else if (Objects.nonNull(key.get()) && Objects.nonNull(getter.get()) && Objects.nonNull(dsplyCreator.get())) {
                JPanel panel = FieldHandler.displayField(canvas, obj, key.get(), getter, dsplyCreator);
                infoContainer.add(panel);
                infoFieldFound.set(true);
            }
        });

        if (!infoFieldFound.get())
            createEmptyInfoContMessage();

        if (!controlFieldFound.get())
            createEmptyControlContMessage();

        frame.repaint();
    }

    protected void createCanvasContent(JPlotterCanvas canvas) {
        controlContainer.setBackground(UIManager.getColor("Panel.background"));
        controlArea.setBackground(UIManager.getColor("Panel.background"));
        controlBorderWrap.setBorder(new LineBorder(Color.LIGHT_GRAY));

        infoContainer.setBackground(infoControlWrap.getBackground());
        infoBorderWrap.setBorder(new LineBorder(infoControlWrap.getBackground()));
        infoArea.setBackground(infoControlWrap.getBackground());

        JPanel exportPanel = new JPanel();
        JButton exportSvgBtn = new JButton("Export SVG");
        JButton exportPdfBtn = new JButton("Export PDF");

        exportSvgBtn.addActionListener(e -> {
            Document doc = canvas.paintSVG();
            SVGUtils.documentToXMLFile(doc, new File("export.svg"));
        });

        exportPdfBtn.addActionListener(e -> {
            try {
                PDDocument doc = canvas.paintPDF();
                doc.save("export.pdf");
                doc.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        title.setText(canvas.getClass().getSimpleName());
        controlHeader.setText("Export canvas");

        exportPanel.setLayout(new BoxLayout(exportPanel, BoxLayout.X_AXIS));
        exportPanel.add(exportSvgBtn);
        exportPanel.add(exportPdfBtn);

        exportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlContainer.add(exportPanel);

        frame.repaint();
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
                    } catch (IllegalAccessException | NoSuchMethodException | InstantiationException |
                             InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
                    canvas.scheduleRepaint();
                } else if (JPlotterCanvas.class.isAssignableFrom(obj.getClass())) {
                    createCanvasContent(canvas);
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