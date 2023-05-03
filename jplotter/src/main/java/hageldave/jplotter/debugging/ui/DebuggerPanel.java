package hageldave.jplotter.debugging.ui;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.annotations.DebugGetter;
import hageldave.jplotter.debugging.annotations.DebugSetter;
import hageldave.jplotter.debugging.customPrinter.CustomPrinterInterface;
import hageldave.jplotter.debugging.panelcreators.control.ControlPanelCreator;
import hageldave.jplotter.debugging.panelcreators.display.DisplayPanelCreator;
import hageldave.jplotter.debugging.treemodel.TreeConstructor;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
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
 * The DebuggerPanel class is responsible for displaying and constructing the "creator" GUI elements of the debugger.
 * The DebuggerPanel interface is split into a sidebar and a main area.
 * Each DebuggerPanel is connected to a {@link JPlotterCanvas} and displays the renderer/renderable tree structure (see {@link TreeConstructor} for more information)
 * of the canvas in the sidebar.
 * Each element shown in the canvas can be selected (from the sidebar) and the elements' so-called "panels"
 * (see {@link ControlPanelCreator} and {@link DisplayPanelCreator}) will then be shown in the main area.
 * These panels are split into "control panels" (created by a {@link ControlPanelCreator}) and "display panels" (created by a {@link DisplayPanelCreator}).
 * A control panel typically contains multiple gui elements (buttons, sliders, ...) which can be used to manipulate the elements' properties,
 * whereas a display panel only can be used to display certain information about the property of element.
 * Currently, the debugger supports most of the important properties of JPlotter, but it is designed to be extendable.
 * See the documentation of the corresponding creator interfaces ({@link ControlPanelCreator} and {@link DisplayPanelCreator}) for more information.
 */
class DebuggerPanel extends JPanel {
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

    final public JPlotterCanvas canvas;

    /**
     * Standard DebuggerPanel constructor.
     *
     * @param canvas the content of this canvas will be displayed by the debugger
     */
    protected DebuggerPanel(JPlotterCanvas canvas) {
        this.canvas = canvas;
        registerTreeListener();
        display();
    }

    /**
     * Creates and displays the DebuggerPanel.
     * The debugger shows all the renderer and renderable items in the given canvas.
     * If there are any renderers or renderables added at a later time, the {@link DebuggerPanel#refresh()}
     * method can be called to update the underlying model.
     * The debugger also displays the registered panels (see {@link ControlPanelCreator} and {@link DisplayPanelCreator})
     * for each renderer/renderable.
     */
    protected void display() {
        // set constructed tree as tree model
        tree.setModel(new DefaultTreeModel(TreeConstructor.getAllRenderersOnCanvas(canvas)));
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
        refreshBtnContainer.setBackground(bgColor);
        JButton refreshTree = new JButton("Refresh rendering tree");
        refreshTree.setToolTipText("<html>"
                        + "Updates the underlying TreeModel. "
                        +"<br>"
                        + "Could be used if one or more objects expire (e.g. tickmarkLabels of CoordSysRenderer when resizing the window.)"
                        + "</html>");
        refreshTree.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshTree.addActionListener(e -> refresh());

        refreshTree.setMaximumSize(refreshTree.getPreferredSize());
        refreshBtnContainer.add(refreshTree, BorderLayout.EAST);

        leftContainer.add(refreshBtnContainer, BorderLayout.SOUTH);
        leftContainer.add(treeScrollPane, BorderLayout.CENTER);

        JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitpane.setRightComponent(rightScrollPane);
        splitpane.setLeftComponent(leftContainer);
        splitpane.setBorder(new EmptyBorder(0, 0, 0, 0));
        splitpane.setDividerLocation(2.0 / 5);

        this.setLayout(new BorderLayout());
        this.add(splitpane, BorderLayout.CENTER);
        this.setBorder(new MatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        this.repaint();
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
            AtomicReference<String> annotationID = new AtomicReference<>();
            AtomicReference<Method> getter = new AtomicReference<>();
            AtomicReference<Method> setter = new AtomicReference<>();
            AtomicReference<Class<? extends ControlPanelCreator>> ctrlCreator = new AtomicReference<>();
            AtomicReference<Class<? extends DisplayPanelCreator>> dsplyCreator = new AtomicReference<>();
            AtomicReference<Class<? extends CustomPrinterInterface>> customPrinter = new AtomicReference<>();

            for (DebugGetter debugGetter : searchGetter.getAnnotationsByType(DebugGetter.class)) {
                annotationID.set(debugGetter.ID());
                getter.set(searchGetter);
                dsplyCreator.set(debugGetter.creator());
                customPrinter.set(debugGetter.objectPrinter());

                allMethods.forEach(searchSetter -> {
                    for (DebugSetter debugSetter : searchSetter.getAnnotationsByType(DebugSetter.class)) {
                        if (Objects.equals(debugSetter.ID(), annotationID.get())) {
                            if (Objects.nonNull(setter.get())) {
                                System.out.println(setter.get());
                                throw new RuntimeException("Annotation annotationID for @DebugSetter used more than once");
                            }

                            setter.set(searchSetter);
                            ctrlCreator.set(debugSetter.creator());

                        }
                    }
                });
            }

            if (Objects.nonNull(annotationID.get()) && Objects.nonNull(getter.get()) && Objects.nonNull(setter.get()) && Objects.nonNull(ctrlCreator.get())) {
                JPanel panel = controlField(canvas, obj, annotationID.get(), getter, setter, ctrlCreator);
                controlContainer.add(panel);
                controlFieldFound.set(true);
            } else if (Objects.nonNull(annotationID.get()) && Objects.nonNull(getter.get()) && Objects.nonNull(dsplyCreator.get()) && Objects.nonNull(customPrinter.get())) {
                JPanel panel = displayField(canvas, obj, annotationID.get(), getter, dsplyCreator, customPrinter);
                infoContainer.add(panel);
                infoFieldFound.set(true);
            }
        });

        if (!infoFieldFound.get())
            createEmptyInfoContMessage();

        if (!controlFieldFound.get())
            createEmptyControlContMessage();

        this.repaint();
    }

    protected void createCanvasContent(JPlotterCanvas canvas) {
        changeControlAreaColor(UIManager.getColor("Panel.background"), Color.LIGHT_GRAY);
        changeInfoAreaColor(infoControlWrap.getBackground(), infoControlWrap.getBackground());

        JPanel exportPanel = new JPanel();
        JButton exportSvgBtn = new JButton("Export SVG");
        exportSvgBtn.setToolTipText("Exports the canvas as a .svg file to the root of the project.");

        JButton exportPdfBtn = new JButton("Export PDF");
        exportPdfBtn.setToolTipText("Exports the canvas as a .pdf file to the root of the project.");

        JButton exportPngBtn = new JButton("Export PNG");
        exportPngBtn.setToolTipText("Exports the canvas as a .png file to the root of the project.");

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

        exportPngBtn.addActionListener(e -> {
            String timeSubstring = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace(':', '-');
            Img img = new Img(canvas.asComponent().getSize());
            img.paint(g -> canvas.asComponent().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "export-" + timeSubstring + ".png");
        });

        title.setText(canvas.getClass().getSimpleName());
        controlHeader.setText("Export canvas");

        exportPanel.setLayout(new BoxLayout(exportPanel, BoxLayout.X_AXIS));
        exportPanel.add(exportSvgBtn);
        exportPanel.add(exportPdfBtn);
        exportPanel.add(exportPngBtn);

        exportPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlContainer.add(exportPanel);

        this.repaint();
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
     * Refreshes the renderer and renderable tree in the DebuggerPanel.
     * Can be used, if items are being added to (or removed from) the canvas after instantiating the debugger.
     */
    protected void refresh() {
        tree.setModel(new DefaultTreeModel(TreeConstructor.getAllRenderersOnCanvas(canvas)));
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

    protected static JPanel controlField(JPlotterCanvas canvas,
                                         Object obj,
                                         String field,
                                         AtomicReference<Method> getter,
                                         AtomicReference<Method> setter,
                                         AtomicReference<Class<? extends ControlPanelCreator>> creator) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.setBorder(new EmptyBorder(0, 0, 7, 0));

        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.X_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(0, 0, 3, 0));

        JLabel fieldType = new JLabel(("" + getter.get().getReturnType().getSimpleName()) + " ");
        JLabel fieldName = new JLabel((field) + " ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        fieldName.setToolTipText("Annotation ID of the property");
        fieldType.setFont(new Font(fieldType.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()-2));
        fieldType.setForeground(Color.GRAY);
        fieldType.setToolTipText("Type of the property");

        labelContainer.add(fieldType);
        labelContainer.add(fieldName);

        JPanel controlContainer = new JPanel();
        controlContainer.setLayout(new BoxLayout(controlContainer, BoxLayout.X_AXIS));
        controlContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        controlContainer.setBorder(new EmptyBorder(0, 0, 7, 0));

        container.add(labelContainer);
        container.add(controlContainer);

        try {
            ControlPanelCreator pc = creator.get().getDeclaredConstructor().newInstance();
            pc.createUnchecked(canvas, obj, controlContainer, setter.get(), getter.get());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        JSeparator sep = new JSeparator();
        container.add(sep);

        return container;
    }

    protected static JPanel displayField(JPlotterCanvas canvas,
                                         Object obj,
                                         String field,
                                         AtomicReference<Method> getter,
                                         AtomicReference<Class<? extends DisplayPanelCreator>> creator,
                                         AtomicReference<Class<? extends CustomPrinterInterface>> objectPrinter) {
        JPanel labelContainer = new JPanel();
        labelContainer.setLayout(new BoxLayout(labelContainer, BoxLayout.X_AXIS));
        labelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelContainer.setBorder(new EmptyBorder(9, 0, 9, 0));

        JLabel fieldType = new JLabel(("" + getter.get().getReturnType().getSimpleName()) + " ");
        JLabel fieldName = new JLabel((field) + ": ");
        fieldName.setFont(new Font(fieldName.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()));
        fieldName.setToolTipText("Annotation ID of the property");
        fieldType.setFont(new Font(fieldType.getFont().getName(), Font.BOLD, fieldName.getFont().getSize()-2));
        fieldType.setForeground(Color.GRAY);
        fieldType.setToolTipText("Type of the property");

        labelContainer.add(fieldType);
        labelContainer.add(fieldName);

        try {
            DisplayPanelCreator pc = creator.get().getDeclaredConstructor().newInstance();
            CustomPrinterInterface cpi = objectPrinter.get().getDeclaredConstructor().newInstance();
            pc.createUnchecked(canvas, obj, labelContainer, getter.get(), cpi);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return labelContainer;
    }
}