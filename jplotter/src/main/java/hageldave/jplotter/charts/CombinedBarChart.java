package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderers.CombinedBarRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.AlignmentConstants;
import hageldave.jplotter.util.PickingRegistry;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.TreeSet;

public class CombinedBarChart {
    protected TrianglesRenderer content;
    protected JPlotterCanvas canvas;
    protected CombinedBarRenderer barRenderer;
    protected LinkedList<BarGroup> barsInRenderer = new LinkedList<>();
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();

    final protected Legend legend = new Legend();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();

    private int legendRightWidth = 100;
    private int legendBottomHeight = 60;

    public CombinedBarChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public CombinedBarChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setBounds(new Rectangle(400, 400));
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.black);
        this.barRenderer = new CombinedBarRenderer(AlignmentConstants.HORIZONTAL, DefaultColorScheme.DARK.get());
        this.content = new TrianglesRenderer();
        this.barRenderer.setCoordinateView(-1, -1, 1, 1);
        this.barRenderer.setContent(content);
        this.canvas.setRenderer(barRenderer);
        this.barRenderer.setxAxisLabel(xLabel);
        this.barRenderer.setyAxisLabel(yLabel);
    }

    public CombinedBarChart addData(BarGroup group) {
        this.barsInRenderer.add(group);
        this.barRenderer.addBarGroup(group);


        for (BarGroup.BarStruct struct : group.getGroupedBars().values()) {
            for (BarGroup.Stack stack : struct.stacks) {
                stack.setPickColor(registerInPickingRegistry(stack));
            }
        }
        return this;
    }

    public Legend placeLegendOnRight() {
        if(this.barRenderer.getLegendBottom() == legend) {
            this.barRenderer.setLegendBottom(null);
            this.barRenderer.setLegendBottomHeight(0);
        }
        this.barRenderer.setLegendRight(legend);
        this.barRenderer.setLegendRightWidth(this.legendRightWidth);
        return legend;
    }

    public Legend placeLegendBottom() {
        if(this.barRenderer.getLegendRight() == legend) {
            this.barRenderer.setLegendRight(null);
            this.barRenderer.setLegendRightWidth(0);
        }
        this.barRenderer.setLegendBottom(legend);
        this.barRenderer.setLegendBottomHeight(this.legendBottomHeight);
        return legend;
    }

    public void placeLegendNowhere() {
        if(this.barRenderer.getLegendRight() == legend) {
            this.barRenderer.setLegendRight(null);
            this.barRenderer.setLegendRightWidth(0);
        }
        if(this.barRenderer.getLegendBottom() == legend) {
            this.barRenderer.setLegendBottom(null);
            this.barRenderer.setLegendBottomHeight(0);
        }
    }

    protected synchronized int registerInPickingRegistry(Object obj) {
        int id = freedPickIds.isEmpty() ? pickingRegistry.getNewID() : freedPickIds.pollFirst();
        pickingRegistry.register(obj, id);
        return id;
    }

    protected synchronized Object deregisterFromPickingRegistry(int id) {
        Object old = pickingRegistry.lookup(id);
        pickingRegistry.register(null, id);
        freedPickIds.add(id);
        return old;
    }

    public TrianglesRenderer getContent() {
        return content;
    }

    public JPlotterCanvas getCanvas() {
        System.out.println(this.canvas.asComponent().getWidth());
        return this.canvas;
    }

    public CombinedBarRenderer getBarRenderer() {
        return barRenderer;
    }
}
