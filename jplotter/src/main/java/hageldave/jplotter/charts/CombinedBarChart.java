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

import java.awt.*;
import java.util.Arrays;
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
        this.canvas.asComponent().setBackground(new Color(20, 20, 20));
        this.barRenderer = new CombinedBarRenderer(AlignmentConstants.VERTICAL, DefaultColorScheme.DARK.get());
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

    public Legend addRightLegend(final int width, final String[] categories,
                                 final int[] colors, final int[] pickColors) {
        if (categories.length != colors.length || categories.length != pickColors.length) {
            throw new IllegalArgumentException("Arrays have to have equal length!");
        }
        Legend lgd = new Legend();
        this.barRenderer.setLegendRight(lgd);
        this.barRenderer.setLegendRightWidth(width);
        for (int i = 0; i < categories.length; i++) {
            lgd.addBarLabel(colors[i], categories[i], pickColors[i]);
        }
        return lgd;
    }

    public Legend addRightLegend(final int width, final String[] categories,
                                  final int[] colors) {
        int[] pickColors = new int[colors.length];
        Arrays.fill(pickColors, 0);
        return addRightLegend(width, categories, colors, pickColors);
    }

    public Legend addRightLegend() {
        return addRightLegend(70, new String[]{}, new int[]{}, new int[]{});
    }

    public Legend addBottomLegend(final int height, final String[] categories,
                                  final int[] colors, final int[] pickColors) {
        if (categories.length != colors.length || categories.length != pickColors.length) {
            throw new IllegalArgumentException("Arrays have to have equal length!");
        }
        Legend lgd = new Legend();
        this.barRenderer.setLegendBottom(lgd);
        this.barRenderer.setLegendBottomHeight(height);
        for (int i = 0; i < categories.length; i++) {
            lgd.addBarLabel(colors[i], categories[i], pickColors[i]);
        }
        return lgd;
    }

    public Legend addBottomLegend(final int height, final String[] categories,
                                  final int[] colors) {
        int[] pickColors = new int[colors.length];
        Arrays.fill(pickColors, 0);
        return addBottomLegend(height, categories, colors, pickColors);
    }

    public Legend addBottomLegend() {
        return addBottomLegend(30, new String[]{}, new int[]{}, new int[]{});
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
