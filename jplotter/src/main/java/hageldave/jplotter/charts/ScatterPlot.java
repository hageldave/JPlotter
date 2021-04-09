package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.PointsRenderer;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ScatterPlot {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected PointsRenderer content;
    final protected HashMap<Integer, Dataset> pointMap;

    private class Dataset {
        protected double[][] pointsCoordinates;
        protected DefaultGlyph glyph;
        protected Color color;
        protected Points points;

        Dataset(final double[][] pointsCoordinates, final DefaultGlyph glyph, final Color color, final Points points) {
            this.pointsCoordinates = pointsCoordinates;
            this.glyph = glyph;
            this.color = color;
            this.points = points;
        }

        public double[][] getPointsCoordinates () {
            return pointsCoordinates;
        }

        public DefaultGlyph getGlyph () {
            return glyph;
        }

        public Color getColor () {
            return color;
        }

        public Points getPoints () {
            return points;
        }
    }

    public ScatterPlot(final boolean useOpenGL) {
        this.pointMap = new HashMap<Integer, Dataset>();
        this.canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        setupScatterPlot();
    }

    public ScatterPlot(final JPlotterCanvas canvas) {
        this.pointMap = new HashMap<Integer, Dataset>();
        this.canvas = canvas;
        setupScatterPlot();
    }

    protected void setupScatterPlot() {
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.coordsys = new CoordSysRenderer();
        this.content = new PointsRenderer();
        this.coordsys.setCoordinateView(-1,-1,1,1);
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);
    }

    public ScatterPlot addPoints(final int ID, final double[][] points, final DefaultGlyph glyph, final Color color) {
        ScatterPlot old = this;
        Points tempPoints = new Points(glyph);
        for(double[] entry: points) {
            double x = entry[0], y = entry[1];
            Points.PointDetails point = tempPoints.addPoint(x, y);
            point.setColor(color);
        }
        Dataset newSet = new Dataset(points, glyph, color, tempPoints);
        this.pointMap.put(ID, newSet);
        this.content.addItemToRender(tempPoints);
        return old;
    }

    public ScatterPlot removePoints(final int ID) {
        ScatterPlot old = this;
        this.pointMap.remove(ID);
        return old;
    }

    public CoordSysScrollZoom addScrollZoom() {
        return new CoordSysScrollZoom(this.canvas, this.coordsys).register();
    }

    public CoordSysPanning addPanning() {
        return new CoordSysPanning(this.canvas, this.coordsys).register();
    }

    public ScatterPlot useOpenGL(final boolean useOpenGL) {
        this.canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        setupScatterPlot();
        restoreDatapoints();
        return this;
    }

    protected void restoreDatapoints() {
        for (Map.Entry<Integer, Dataset> dataset: this.pointMap.entrySet()) {
            Points tempPoints = new Points(dataset.getValue().getGlyph());
            for(double[] entry: dataset.getValue().getPointsCoordinates()) {
                double x = entry[0], y = entry[1];
                Points.PointDetails point = tempPoints.addPoint(x, y);
                point.setColor(dataset.getValue().getColor());
            }
            content.addItemToRender(tempPoints);
        }
    }

    public JPlotterCanvas getCanvas () {
        return canvas;
    }

    public CoordSysRenderer getCoordsys () {
        return coordsys;
    }

    public PointsRenderer getContent () {
        return content;
    }
}
