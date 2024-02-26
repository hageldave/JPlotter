package hageldave.jplotter.debugging.ui;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The DebuggerUI class is responsible for displaying and constructing all the GUI elements of the debugger.
 * The goal of the Debugger is to provide an easy way to view and control the properties
 * of the elements (renderables &amp; renderers) in a canvas.
 * An instance of the DebuggerUI can hold multiple {@link DebuggerPanel} which each correspond to a specific {@link JPlotterCanvas}.
 * As there is only one panel shown at the same time, these panels can be switched in the GUI.
 */
public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected List<DebuggerPanel> debuggerList = new ArrayList<>();

    /**
     * Creates an instance of the DebuggerUI and displays the
     * JFrame by calling the display() method.
     *
     * @param canvases a {@link DebuggerPanel} will be created for each canvas
     */
    public DebuggerUI(JPlotterCanvas... canvases) {
        for (JPlotterCanvas c: canvases) {
            this.debuggerList.add(new DebuggerPanel(c));

            c.asComponent().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    refresh();
                }
            });
        }
        display();
    }

    /**
     * Creates an instance of the DebuggerUI and displays the
     * JFrame by calling the display() method.
     *
     * @param canvas a {@link DebuggerPanel} will be created for this canvas
     */
    public DebuggerUI(JPlotterCanvas canvas) {
        this.debuggerList.add(new DebuggerPanel(canvas));

        canvas.asComponent().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                refresh();
            }
        });
        display();
    }

    public void display() {
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        AtomicReference<DebuggerPanel> prevDebugUI = new AtomicReference<>();
        Integer[] canvasArr = debuggerList.stream().map(Object::hashCode).toArray(Integer[]::new);
        JComboBox<Integer> canvasSelection = new JComboBox<>(canvasArr);
        canvasSelection.addItemListener(e -> prevDebugUI.set(changeDebuggerPanel(frame, debuggerPanel, debuggerList.get(canvasSelection.getSelectedIndex()), prevDebugUI.get())));

        JPanel selectionWrapper = new JPanel(new FlowLayout());
        selectionWrapper.setBorder(new EmptyBorder(3, 5, 3, 5));
        selectionWrapper.setBackground(Color.WHITE);

        JLabel debuggerPanelSelectionLabel = new JLabel("Select debugger panel:");
        debuggerPanelSelectionLabel.setToolTipText("Select debugger panel by its object hashcode.");
        selectionWrapper.add(debuggerPanelSelectionLabel);
        selectionWrapper.add(canvasSelection);


        prevDebugUI.set(changeDebuggerPanel(frame, debuggerPanel, this.debuggerList.get(0)));
        debuggerPanel.add(selectionWrapper, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.getContentPane().add(debuggerPanel);
        frame.setPreferredSize(new Dimension(950, 550));
        SwingUtilities.invokeLater(()->{
        	frame.pack();
        	frame.setVisible(true);
        });
    }

    protected DebuggerPanel changeDebuggerPanel(JFrame frame, JPanel debuggerPanel, DebuggerPanel debuggerUIPanel, DebuggerPanel prevDebugUI) {
        debuggerPanel.remove(prevDebugUI);
        return changeDebuggerPanel(frame, debuggerPanel, debuggerUIPanel);
    }

    protected DebuggerPanel changeDebuggerPanel(JFrame frame, JPanel debuggerPanel, DebuggerPanel debuggerUIPanel) {
        debuggerPanel.add(debuggerUIPanel, BorderLayout.CENTER);
        frame.repaint();
        frame.validate();
        return debuggerUIPanel;
    }

    /**
     * Refreshes each {@link DebuggerPanel} renderer/renderable tree.
     * Can be used, if items are being added to (or removed from) a canvas after instantiating the debugger.
     */
    public void refresh() {
        for (DebuggerPanel p: debuggerList) {
            p.refresh();
        }
    }
}
