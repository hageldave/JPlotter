package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DebuggerUI {
    final protected JFrame frame = new JFrame("Debugger UI");
    final protected List<DebuggerUIPanel> debuggerList = new ArrayList<>();

    public DebuggerUI(JPlotterCanvas... canvases) {
        for (JPlotterCanvas c: canvases) {
            this.debuggerList.add(new DebuggerUIPanel(c));
        }
        display();
    }

    public DebuggerUI(JPlotterCanvas canvas) {
        this.debuggerList.add(new DebuggerUIPanel(canvas));
        display();
    }

    protected void display() {
        JPanel debuggerPanel = new JPanel(new BorderLayout());
        AtomicReference<DebuggerUIPanel> prevDebugUI = new AtomicReference<>();
        Integer[] canvasArr = debuggerList.stream().map(Object::hashCode).toArray(Integer[]::new);
        JComboBox<Integer> canvasSelection = new JComboBox<>(canvasArr);
        canvasSelection.addItemListener(e -> prevDebugUI.set(changeDebuggerPanel(frame, debuggerPanel, debuggerList.get(canvasSelection.getSelectedIndex()), prevDebugUI.get())));

        JPanel selectionWrapper = new JPanel(new FlowLayout());
        selectionWrapper.setBorder(new EmptyBorder(3, 5, 3, 5));
        selectionWrapper.setBackground(Color.WHITE);
        selectionWrapper.add(new JLabel("Select debugger tab:"));
        selectionWrapper.add(canvasSelection);


        prevDebugUI.set(changeDebuggerPanel(frame, debuggerPanel, this.debuggerList.get(0)));
        debuggerPanel.add(selectionWrapper, BorderLayout.SOUTH);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(debuggerPanel);
        frame.setPreferredSize(new Dimension(950, 550));
        frame.pack();
        frame.setVisible(true);
    }

    protected DebuggerUIPanel changeDebuggerPanel(JFrame frame, JPanel debuggerPanel, DebuggerUIPanel debuggerUIPanel, DebuggerUIPanel prevDebugUI) {
        debuggerPanel.remove(prevDebugUI);
        return changeDebuggerPanel(frame, debuggerPanel, debuggerUIPanel);
    }

    protected DebuggerUIPanel changeDebuggerPanel(JFrame frame, JPanel debuggerPanel, DebuggerUIPanel debuggerUIPanel) {
        debuggerPanel.add(debuggerUIPanel, BorderLayout.CENTER);
        frame.repaint();
        frame.pack();
        return debuggerUIPanel;
    }

    protected void refresh() {
        for (DebuggerUIPanel p: debuggerList) {
            p.refresh();
        }
    }
}
