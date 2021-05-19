package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.coordsys.TickMarkGenerator;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Pair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


// TODO currently Triangles are used to display the rectangles - this creates overhead (e.g. when changing color of a bar)
public class BarChart {
    protected CompleteRenderer content;
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected Alignment chartAlignment;
    final protected double barSize = 0.8;
    protected int barCount;
    // is now public for easier access, maybe in ScatterPlot too?
    final public HashMap<Integer, BarStruct> trianglesInRenderer = new HashMap<>();
    final protected ArrayList<double[]> dataAdded = new ArrayList<>();

    public enum Alignment {
        VERTICAL,
        HORIZONTAL
    }

    public static class BarStruct {
        public Triangles tri;
        public String descr;

        public BarStruct(final Triangles tri, final String descr) {
            this.tri = tri;
            this.descr = descr;
        }
    }

    public BarChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), Alignment.HORIZONTAL, "X", "Y");
    }

    public BarChart(final boolean useOpenGL, final Alignment chartAlignment) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), chartAlignment, "X", "Y");
    }

    public BarChart(final JPlotterCanvas canvas, final Alignment chartAlignment, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.coordsys = new CoordSysRenderer();
        this.content = new CompleteRenderer();
        this.coordsys.setCoordinateView(-1, -1, 1, 1);
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);
        this.coordsys.setxAxisLabel(xLabel);
        this.coordsys.setyAxisLabel(yLabel);
        this.chartAlignment = chartAlignment;
    }

    public BarChart addBar(final int ID, final double[] data, final Color color, final String descr) {
        double val = Arrays.stream(data).sum();
        return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{descr});
    }

    public BarChart addBar(final int ID, final double val, final Color color, final String descr) {
        return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{descr});
    }

    // TODO ensure all arrays have equal size
    public BarChart addData(final int[] IDs, final double[] data, final Color[] color, final String[] descr) {
        for (int i = 0; i < data.length; i++) {
            Triangles triangleRend = makeBar(i, data[i], color[i]);
            this.trianglesInRenderer.put(IDs[i], new BarStruct(triangleRend, descr[i]));
            this.content.addItemToRender(triangleRend);
        }
        dataAdded.add(data);
        setTickmarks();
        setBarContentBoundaries();
        return this;
    }

    // needs further thoughts
    /*public BarChart addData(final int[] IDs, final double[][] data, final Color[] color, final String[] descr) {
        for (int i = 0; i < data.length; i++) {
            Triangles triangleRend = makeBar(i, data[i], color[i]);
            this.trianglesInRenderer.put(IDs[i], new BarStruct(triangleRend, descr[i]));
            this.content.addItemToRender(triangleRend);
        }
        dataAdded.add(data);
        setTickmarks();
        setBarContentBoundaries();
        return this;
    }*/


    // if new gets added, all the old stuff gets overwritten - fixed
    protected TickMarkGenerator setTickmarks() {
        String[] descr = new String[trianglesInRenderer.size()];
        int j = 0;
        for (BarStruct val: trianglesInRenderer.values()) {
            descr[j++] = val.descr;
        }
        TickMarkGenerator oldTickGen = coordsys.getTickMarkGenerator();
        if (this.chartAlignment == Alignment.HORIZONTAL) {
            coordsys.setTickMarkGenerator((min, max, desired, vert) -> {
                if (!vert)
                    return oldTickGen.genTicksAndLabels(min, max, desired, vert);
                double[] ticks = IntStream.range(0, descr.length).mapToDouble(i -> (double) i).toArray();
                return Pair.of(ticks, descr);
            });
        } else {
            coordsys.setTickMarkGenerator((min, max, desired, vert) -> {
                if (vert)
                    return oldTickGen.genTicksAndLabels(min, max, desired, vert);
                double[] ticks = IntStream.range(0, descr.length).mapToDouble(i -> (double) i).toArray();
                return Pair.of(ticks, descr);
            });
        }
        return coordsys.getTickMarkGenerator();
    }

    protected Triangles makeBar(final int row, final double val, final Color color) {
        Triangles bar = new Triangles();
        if (this.chartAlignment == Alignment.HORIZONTAL) {
            bar.addQuad(new Rectangle2D.Double(0, row-(barSize/2), val, barSize));
            bar.getTriangleDetails().forEach(tri -> tri.setColor(color));
        } else {
            bar.addQuad(new Rectangle2D.Double(row-(barSize/2), 0, barSize, val));
            bar.getTriangleDetails().forEach(tri -> tri.setColor(color));
        }
        barCount++;
        return bar;
    }

    protected void setBarContentBoundaries() {
        int maxVal = Integer.MIN_VALUE; int minVal = Integer.MAX_VALUE;
        if (this.chartAlignment == Alignment.HORIZONTAL) {
            for (BarStruct value: trianglesInRenderer.values()) {
                if (value.tri.getBounds().getMaxX() > maxVal)
                    maxVal = (int) Math.ceil(value.tri.getBounds().getMaxX());
                if (value.tri.getBounds().getMinX() < minVal)
                    minVal = (int) Math.ceil(value.tri.getBounds().getMinX());
            }
            if (minVal < 0) {
                System.out.println(minVal);
                this.coordsys.setCoordinateView(minVal - 0.5, -0.8, maxVal + 0.5, barCount);
            } else {
                this.coordsys.setCoordinateView(0, -0.8, maxVal + 0.5, barCount);
            }
        } else {
            for (BarStruct value: trianglesInRenderer.values()) {
                if (value.tri.getBounds().getMaxY() > maxVal)
                    maxVal = (int) Math.ceil(value.tri.getBounds().getMaxY());
                if (value.tri.getBounds().getMinY() < minVal)
                    minVal = (int) Math.ceil(value.tri.getBounds().getMinY());
            }
            if (minVal < 0) {
                this.coordsys.setCoordinateView(-0.8, minVal - 0.5, barCount, maxVal + 0.5);
            } else {
                this.coordsys.setCoordinateView(-0.8, 0, barCount, maxVal + 0.5);
            }
        }
    }

    // orders by height
    public void orderBars() {
        for (BarStruct val: trianglesInRenderer.values()) {
            this.content.triangles.removeItemToRender(val.tri);
        }
        AtomicInteger index = new AtomicInteger();
        if (this.chartAlignment == Alignment.HORIZONTAL) {
            trianglesInRenderer.values().stream()
                    .sorted(Comparator.comparingDouble(e -> -e.tri.getBounds().getMinX()))
                    .sorted(Comparator.comparingDouble(e -> e.tri.getBounds().getMaxX()))
                    .forEach(e -> {
                        Triangles tri;
                        if (e.tri.getBounds().getMaxX() == 0)
                            tri = makeBar(index.getAndIncrement(), e.tri.getBounds().getMinX(), new Color(e.tri.getTriangleDetails().get(0).c0.getAsInt()));
                        else
                            tri = makeBar(index.getAndIncrement(), e.tri.getBounds().getMaxX(), new Color(e.tri.getTriangleDetails().get(0).c0.getAsInt()));
                        this.content.addItemToRender(tri);
                    });
        } else {
            trianglesInRenderer.values().stream()
                    .sorted(Comparator.comparingDouble(e -> -e.tri.getBounds().getMinY()))
                    .sorted(Comparator.comparingDouble(e -> e.tri.getBounds().getMaxY()))
                    .forEach(e -> {
                        Triangles tri;
                        if (e.tri.getBounds().getMaxY() == 0)
                            tri = makeBar(index.getAndIncrement(), e.tri.getBounds().getMinY(), new Color(e.tri.getTriangleDetails().get(0).c0.getAsInt()));
                        else
                            tri = makeBar(index.getAndIncrement(), e.tri.getBounds().getMaxY(), new Color(e.tri.getTriangleDetails().get(0).c0.getAsInt()));
                        this.content.addItemToRender(tri);
            });
        }
    }

    // TODO necessary?
    public BarChart changeRotation(final Alignment chartAlignment) {
        this.chartAlignment = chartAlignment;

        return this;
    }

    public CompleteRenderer getContent() {
        return content;
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public CoordSysRenderer getCoordsys() {
        return coordsys;
    }

    public Alignment getChartAlignment() {
        return chartAlignment;
    }

    public double getBarSize() {
        return barSize;
    }
}
