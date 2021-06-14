package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderers.CombinedBarRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.AlignmentConstants;

import java.awt.*;

public class CombinedBarChart {

    protected TrianglesRenderer content;
    protected JPlotterCanvas canvas;
    protected CombinedBarRenderer barRenderer;

    public CombinedBarChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public CombinedBarChart(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.barRenderer = new CombinedBarRenderer(AlignmentConstants.VERTICAL);
        this.content = new TrianglesRenderer();
        this.barRenderer.setCoordinateView(-1, -1, 1, 1);
        this.barRenderer.setContent(content);
        this.canvas.setRenderer(barRenderer);
        this.barRenderer.setxAxisLabel(xLabel);
        this.barRenderer.setyAxisLabel(yLabel);
    }

    public CombinedBarChart addData(BarGroup group) {
        this.barRenderer.addBarGroup(group);
        return this;
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
