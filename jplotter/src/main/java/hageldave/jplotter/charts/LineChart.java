package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.LinesRenderer;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;

public class LineChart {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected LinesRenderer content;

    public LineChart(final boolean useOpenGL) {
        this.canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        setupLineChart();
    }

    public LineChart(final boolean useOpenGL, final JPlotterCanvas canvas) {
        this.canvas = canvas;
        setupLineChart();
    }

    /**
     * Helper method to set the initial scatter plot.
     */
    protected void setupLineChart() {
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.coordsys = new CoordSysRenderer();
        this.content = new LinesRenderer();
        this.coordsys.setCoordinateView(-1, -1, 1, 1);
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);
    }

    /**
     * adds a set of points to the scatter plot.
     *
     */
    public Lines addLineSegment(final double[][] data, final Color color) {
        Arrays.sort(data, Comparator.comparingDouble(o -> o[0]));
        Lines tempLine = new Lines();
        System.out.println(data[5].length);
        for (int i = 0; i < data.length-1; i++) {
            double x1 = data[i][0], x2 = data[i+1][0];
            double y1 = data[i][1], y2 = data[i+1][1];
            Lines.SegmentDetails segment = tempLine.addSegment(x1, y1, x2, y2);
            segment.setColor(color);
        }
        this.content.addItemToRender(tempLine);
        return tempLine;
    }

    /**
     * Adds a scroll zoom to the Scatterplot
     *
     * @return the {@link CoordSysScrollZoom} so that it can be further customized
     */
    public CoordSysScrollZoom addScrollZoom() {
        return new CoordSysScrollZoom(this.canvas, this.coordsys).register();
    }

    /**
     * Adds panning functionality to the Scatterplot
     *
     * @return the {@link CoordSysPanning} so that it can be further customized
     */
    public CoordSysPanning addPanning() {
        return new CoordSysPanning(this.canvas, this.coordsys).register();
    }

    /**
     * Adds a zoom functionality by selecting a rectangle.
     *
     * @return the {@link CoordSysViewSelector} so that it can be further customized
     */
    public CoordSysViewSelector addZoomViewSelector() {
        return new CoordSysViewSelector(this.canvas, this.coordsys) {
            { extModifierMask = 0;/* no need for shift to be pressed */ }

            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                coordsys.setCoordinateView(minX, minY, maxX, maxY);
            }
        }.register();
    }



    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public CoordSysRenderer getCoordsys() {
        return coordsys;
    }

    public LinesRenderer getContent() {
        return content;
    }

}
