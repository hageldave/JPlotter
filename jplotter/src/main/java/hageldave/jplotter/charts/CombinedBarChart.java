package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderers.CombinedBarRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.AlignmentConstants;

import java.awt.*;
import java.util.LinkedList;

public class CombinedBarChart {
    protected TrianglesRenderer content;
    protected JPlotterCanvas canvas;
    protected CombinedBarRenderer barRenderer;
    protected LinkedList<BarGroup> barsInRenderer = new LinkedList<>();

    public CombinedBarChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public CombinedBarChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.barRenderer = new CombinedBarRenderer(AlignmentConstants.HORIZONTAL);
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
        return this;
    }

    public Legend addRightLegend(final int width, final boolean autoAddItems) {
        Legend lgd = new Legend();
        this.barRenderer.setLegendRight(lgd);
        this.barRenderer.setLegendRightWidth(width);
        for (BarGroup group: barsInRenderer) {
            for (BarGroup.BarStruct struct: group.getGroupedBars().values()) {
                for (BarGroup.Stack stack: struct.stacks) {
                    lgd.addBarLabel(stack.stackColor.getRGB(), struct.description, stack.pickColor);
                }
            }
        }
        return lgd;
    }

    public Legend addRightLegend() {
        return addRightLegend(70, false);
    }

    public Legend addBottomLegend(final int height, final boolean autoAddItems) {
        Legend lgd = new Legend();
        this.barRenderer.setLegendBottom(lgd);
        this.barRenderer.setLegendBottomHeight(height);
        for (BarGroup group: barsInRenderer) {
            for (BarGroup.BarStruct struct: group.getGroupedBars().values()) {
                for (BarGroup.Stack stack: struct.stacks) {
                    lgd.addBarLabel(stack.stackColor.getRGB(), struct.description, stack.pickColor);
                }
            }
        }
        return lgd;
    }

    public Legend addBottomLegend() {
        return addBottomLegend(30, false);
    }

    public TrianglesRenderer getContent() {
        return content;
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public CombinedBarRenderer getBarRenderer() {
        return barRenderer;
    }
}
