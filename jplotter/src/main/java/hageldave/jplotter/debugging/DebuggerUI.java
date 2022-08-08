package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugGetter;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugSetter;
import hageldave.jplotter.debugging.controlHandler.customPrint.CustomPrinterInterface;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The DebuggerUI class is responsible for displaying and constructing all of the GUI elements of the debugger.
 * The goal of the Debugger is to provide an easy way to view and control the properties
 * of the elements (renderables & renderers) in a canvas.
 *
 * The Debugger interface is split into a sidebar and a main area.
 * Each element shown in the canvas can be selected from the sidebar and its so-called "panels"
 * (see {@link ControlPanelCreator} and {@link DisplayPanelCreator}) will then be shown in the main area.
 * These panels are split into "control panels" (created by a {@link ControlPanelCreator}) and "display panels" (created by a {@link DisplayPanelCreator}).
 * A control panel typically contains multiple gui elements (buttons, ...) which can be used to manipulate the elements' properties,
 * whereas a display panel only can be used to display certain information about the property of element.
 *
 * Currently, the debugger supports most of the important properties, but it is designed to be extendable.
 * See the documentation of the corresponding creater interfaces ({@link ControlPanelCreator} and {@link DisplayPanelCreator}) for more information.
 *
 */
public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected JTree tree = new JTree();
    final protected Color bgColor = new Color(246, 246, 246);
    final protected JPanel controlBorderWrap = new JPanel(new BorderLayout());
    final protected JPanel controlArea = new JPanel(new BorderLayout());
    final protected JPanel controlContainer = new JPanel();
    final protected JPanel infoBorderWrap = new JPanel(new BorderLayout());

    final protected JPanel infoArea = new JPanel(new BorderLayout());
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

    /**
     * Creates and displays the debugger window.
     * The debugger shows all the renderer and renderable items in the given canvas.
     * If there are any renderers or renderables added at a later time, the {@link DebuggerUI#refresh()}
     * method can be called to update the underlying model.
     *
     * The debugger also displays the registered panels (see {@link ControlPanelCreator} and {@link DisplayPanelCreator})
     * for each renderer/renderable.
     */
    public void display() {
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // set constructed tree as tree model
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));
        tree.setBorder(new EmptyBorder(2, 5, 2, 5));

        // start title container
        JPanel titleArea = new JPanel(new GridLayout(1, 0));
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

        controlArea.setBorder(new EmptyBorder(10, 10, 0, 10));
        controlArea.add(controlHeader, BorderLayout.NORTH);
        controlArea.add(controlContainer, BorderLayout.CENTER);
        controlBorderWrap.add(controlArea, BorderLayout.CENTER);

        // start info container
        infoContainer.setLayout(new BoxLayout(infoContainer, BoxLayout.Y_AXIS));
        infoContainer.setBorder(new EmptyBorder(10, 0, 0, 0));
        infoHeader.setFont(new Font(infoHeader.getFont().getName(), Font.PLAIN, 16));

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
        changeControlAreaColor(infoControlWrap.getBackground(), infoControlWrap.getBackground());
        changeInfoAreaColor(infoControlWrap.getBackground(), infoControlWrap.getBackground());

        JScrollPane rightScrollPane = new JScrollPane(infoControlWrap);
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        rightScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel leftContainer = new JPanel(new BorderLayout());

        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(12);
        treeScrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel refreshBtnContainer = new JPanel(new BorderLayout());
        JButton refreshTree = new JButton("Refresh objects");
        refreshTree.setToolTipText("<html>"
                        + "Updates the underlying TreeModel. "
                        +"<br>"
                        + "Could be used if one or more objects expire (e.g. tickmarkLabels of CoordSysRenderer when resizing the window.)"
                        + "</html>");
        refreshTree.addActionListener(e -> refresh());

        refreshTree.setMaximumSize(refreshTree.getPreferredSize());
        refreshBtnContainer.add(refreshTree, BorderLayout.EAST);

        leftContainer.add(refreshBtnContainer, BorderLayout.SOUTH);
        leftContainer.add(treeScrollPane, BorderLayout.CENTER);

        JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setRightComponent(rightScrollPane);
        splitpane.setLeftComponent(leftContainer);
        splitpane.setBorder(new EmptyBorder(0, 0, 0, 0));

        frame.getContentPane().add(splitpane);
        frame.setPreferredSize(new Dimension(950, 550));
        frame.pack();
        frame.setVisible(true);

        splitpane.setDividerLocation(2.0 / 5);
    }

    protected void createEmptyMessage() {
        changeControlAreaColor(infoControlWrap.getBackground(), infoControlWrap.getBackground());
        changeInfoAreaColor(infoControlWrap.getBackground(), infoControlWrap.getBackground());

        title.setText("No renderer or renderable selected.");
    }

    protected void createEmptyInfoContMessage() {
        infoContainer.add(new JLabel("No annotated fields found."));
    }

    protected void createEmptyControlContMessage() {
        controlContainer.add(new JLabel("No manipulable fields found."));
    }

    protected void fillContent(Object obj) {
        title.setText(obj.getClass().getSimpleName());

        controlHeader.setText("Manipulate object properties");
        infoHeader.setText("Additional information");

        changeControlAreaColor(UIManager.getColor("Panel.background"), Color.LIGHT_GRAY);
        changeInfoAreaColor(UIManager.getColor("Panel.background"), Color.LIGHT_GRAY);

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
            AtomicReference<Class<? extends CustomPrinterInterface>> customPrinter = new AtomicReference<>();

            for (DebugGetter debugGetter : searchGetter.getAnnotationsByType(DebugGetter.class)) {
                key.set(debugGetter.key());
                getter.set(searchGetter);
                dsplyCreator.set(debugGetter.creator());
                customPrinter.set(debugGetter.objectPrinter());

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
            } else if (Objects.nonNull(key.get()) && Objects.nonNull(getter.get()) && Objects.nonNull(dsplyCreator.get()) && Objects.nonNull(customPrinter.get())) {
                JPanel panel = FieldHandler.displayField(canvas, obj, key.get(), getter, dsplyCreator, customPrinter);
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
        changeControlAreaColor(UIManager.getColor("Panel.background"), Color.LIGHT_GRAY);
        changeInfoAreaColor(infoControlWrap.getBackground(), infoControlWrap.getBackground());

        JPanel exportPanel = new JPanel();
        JButton exportSvgBtn = new JButton("Export SVG");
        exportSvgBtn.setToolTipText("Exports the canvas as a .svg file to the root of the project.");

        JButton exportPdfBtn = new JButton("Export PDF");
        exportPdfBtn.setToolTipText("Exports the canvas as a .pdf file to the root of the project.");

        exportSvgBtn.addActionListener(e -> {
            String timeSubstring = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace(':', '-');
            String fileName = "export-" + timeSubstring + ".svg";
            Document doc = canvas.paintSVG();
            SVGUtils.documentToXMLFile(doc, new File(fileName));
        });

        exportPdfBtn.addActionListener(e -> {
            try {
                String timeSubstring = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace(':', '-');
                String fileName = "export-" + timeSubstring + ".pdf";
                PDDocument doc = canvas.paintPDF();
                doc.save(fileName);
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
            Object lastSelectedComponent = tree.getLastSelectedPathComponent();
            if (Objects.nonNull(lastSelectedComponent)) {
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
                    fillContent(obj);
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

    /**
     * Refreshes the renderer and renderable tree in the debugger window.
     * This can be used, if items are being added to (or removed from) the canvas after instantiating the debugger.
     */
    public void refresh() {
        tree.setModel(new DefaultTreeModel(Debugger.getAllRenderersOnCanvas(canvas)));
    }

    private void changeControlAreaColor(Color backgroundColor, Color borderColor) {
        controlContainer.setBackground(backgroundColor);
        controlArea.setBackground(backgroundColor);
        controlBorderWrap.setBorder(new LineBorder(borderColor));
    }

    private void changeInfoAreaColor(Color backgroundColor, Color borderColor) {
        infoContainer.setBackground(backgroundColor);
        infoArea.setBackground(backgroundColor);
        infoBorderWrap.setBorder(new LineBorder(borderColor));
    }
}