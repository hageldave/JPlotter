package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.coordsys.TickMarkGenerator;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.BarRenderer;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.util.AlignmentConstants;
import hageldave.jplotter.util.Pair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


public class BarChart {
    final public TreeMap<Integer, BarStruct> trianglesInRenderer = new TreeMap<>();
    final protected double barSize = 0.8;
    protected CompleteRenderer content;
    protected JPlotterCanvas canvas;
    protected BarRenderer barRenderer;
    protected int chartAlignment;
    protected int barCount = 0;

    public static class BarStruct {
        final public LinkedList<Triangles> stacks = new LinkedList<>();
        final public LinkedList<Double> dataSets = new LinkedList<>();
        public String descr;
        public int position;
        public double barLength;

        public BarStruct(final Triangles stacks, final int position, final String descr, final double dataSet) {
            this.stacks.add(stacks);
            this.descr = descr;
            this.position = position;
            this.dataSets.add(dataSet);
        }

        public BarStruct(final Triangles stacks, final int position, final String descr, final double dataSet, final double barLength) {
            this.stacks.add(stacks);
            this.descr = descr;
            this.position = position;
            this.barLength = barLength;
            this.dataSets.add(dataSet);
        }
    }

    public BarChart(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), AlignmentConstants.HORIZONTAL, "X", "Y");
    }

    public BarChart(final boolean useOpenGL, final int chartAlignment) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), chartAlignment, "X", "Y");
    }

    public BarChart(final JPlotterCanvas canvas, final int chartAlignment, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.barRenderer = new BarRenderer(chartAlignment);
        this.content = new CompleteRenderer();
        this.barRenderer.setCoordinateView(-1, -1, 1, 1);
        this.barRenderer.setContent(content);
        this.canvas.setRenderer(barRenderer);
        this.barRenderer.setxAxisLabel(xLabel);
        this.barRenderer.setyAxisLabel(yLabel);
        this.chartAlignment = chartAlignment;
    }

    public BarChart addBar(final int ID, final double[] data, final Color color, final String descr) {
        double val = Arrays.stream(data).sum();
        return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{descr});
    }

    public BarChart addBar(final int ID, final double val, final Color color, final String descr) {
        return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{descr});
    }

    public BarChart addBar(final int ID, final double val, final Color color) {
        return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{""});
    }

    public BarChart addData(final int[] IDs, final double[] data, final Color[] color, final String[] descr) {
        if (!(IDs.length == data.length && data.length == color.length && color.length == descr.length))
            throw new IllegalArgumentException("All arrays have to have equal size!");
        for (int i = 0; i < data.length; i++) {
            if (this.trianglesInRenderer.containsKey(IDs[i])) {
                addStack(IDs[i], data[i], color[i]);
            } else {
                Triangles triangleRend = makeBar(barCount++, data[i], color[i]);
                this.trianglesInRenderer.put(IDs[i],
                            new BarStruct(triangleRend, barCount, descr[i], data[i], data[i]));
                this.content.addItemToRender(triangleRend);
            }
        }
        return this;
    }

    // [i][0: val; 1: index] - oder umgekehrt, needs testing
    public BarChart addData(final int[] IDs, final double[][] data, final Color[] color, final String[] descr) {
        // recalc data
        HashMap<Double, Double> vals = new HashMap<>();
        for (int i = 0; i < data.length; i++) {
            if (!vals.containsKey(data[i][1])) {
                vals.put(data[i][1], data[i][0]);
            } else {
                Double currentHeight = vals.get(data[i][1]) + data[i][0];
                vals.put(data[i][1], currentHeight);
            }
        }

        int j = 0;
        for (Double val: vals.values()) {
            Triangles triangleRend = makeBar(j, val, color[j]);
            //this.trianglesInRenderer.put(IDs[j], new BarStruct(triangleRend, j, descr[j]));
            this.content.addItemToRender(triangleRend);
            j++;
        }
        return this;
    }

    // stack will be added on already existing stack
    protected BarChart addStack(final Integer ID, final double data, final Color color) {
        Triangles newStack = makeBar(this.trianglesInRenderer.get(ID).barLength,
                this.trianglesInRenderer.get(ID).position - 1, data, color);
        this.trianglesInRenderer.get(ID).stacks.add(newStack);
        this.trianglesInRenderer.get(ID).dataSets.add(data);
        this.content.addItemToRender(newStack);
        this.trianglesInRenderer.get(ID).barLength += data;
        return this;
    }

    public TickMarkGenerator setTickmarks() {
        String[] description = new String[trianglesInRenderer.size()];
        for (int key: trianglesInRenderer.keySet()) {
            int pos = trianglesInRenderer.get(key).position - 1;
            description[pos] = trianglesInRenderer.get(key).descr;
        }

        TickMarkGenerator oldTickGen = barRenderer.getTickMarkGenerator();
        barRenderer.setTickMarkGenerator((min, max, desired, vert) -> {
            if (!vert && this.chartAlignment == AlignmentConstants.HORIZONTAL)
                return oldTickGen.genTicksAndLabels(min, max, desired, vert);
            if (vert && this.chartAlignment == AlignmentConstants.VERTICAL)
                return oldTickGen.genTicksAndLabels(min, max, desired, vert);
            double[] ticks = IntStream.range(0, description.length).mapToDouble(i -> (double) i).toArray();
            return Pair.of(ticks, description);
        });
        return barRenderer.getTickMarkGenerator();
    }

    protected Triangles makeBar(final int row, final double val, final Color color) {
        return makeBar(0, row, val, color);
    }

    protected Triangles makeBar(final double start, final int row, final double val, final Color color) {
        Triangles bar = new Triangles();
        if (this.chartAlignment == AlignmentConstants.HORIZONTAL) {
            bar.addQuad(new Rectangle2D.Double(start, row-(barSize/2), val, barSize));
        } else {
            bar.addQuad(new Rectangle2D.Double(row-(barSize/2), start, barSize, val));
        }
        bar.getTriangleDetails().forEach(tri -> tri.setColor(color));
        return bar;
    }

    public BarChart setBarContentBoundaries() {
        double maxVal = Integer.MIN_VALUE; double minVal = Integer.MAX_VALUE;
        for (BarStruct value: trianglesInRenderer.values()) {
            maxVal = Math.max(value.barLength, maxVal);
            minVal = Math.min(value.barLength, minVal);
        }
        if (minVal >= 0) {
            minVal = 0.5;
        }
        if (this.chartAlignment == AlignmentConstants.HORIZONTAL) {
            this.barRenderer.setCoordinateView(minVal - 0.5, -0.8, maxVal + 0.5, barCount);
        } else if (this.chartAlignment == AlignmentConstants.VERTICAL) {
            this.barRenderer.setCoordinateView(-0.8, minVal - 0.5, barCount, maxVal + 0.5);
        }
        return this;
    }

    // orders by height
    public BarChart sortBars() {
        sortBars((o1, o2) -> {
            if (o1 < o2) return -1;
            if (o1 > o2) return 1;
            return 0;
        });
        return this;
    }

    public BarChart sortBars(final Comparator<Double> comparator) {
        removeAllBars();
        AtomicInteger index = new AtomicInteger();
        trianglesInRenderer.values().stream()
                .sorted((a, b) -> comparator.compare(a.barLength, b.barLength))
                .forEach(e -> reconstructBars(index, e));
        setTickmarks();
        return this;
    }

    public BarChart sortBarsIDs() {
        removeAllBars();
        AtomicInteger index = new AtomicInteger();
        trianglesInRenderer.values().forEach(e -> reconstructBars(index, e));
        setTickmarks();
        return this;
    }

    protected BarChart reconstructBars(AtomicInteger index, BarStruct struct) {
        double missingVals = struct.barLength;
        struct.position = index.incrementAndGet();
        for (Triangles element: struct.stacks) {
            double length = 0;
            if (chartAlignment == AlignmentConstants.HORIZONTAL) {
                if (element.getBounds().getMaxX() == 0)
                    length = element.getBounds().getMinX();
                else
                    length = element.getBounds().getMaxX();
            } else if (chartAlignment == AlignmentConstants.VERTICAL) {
                if (element.getBounds().getMaxY() == 0)
                    length = element.getBounds().getMinY();
                else
                    length = element.getBounds().getMaxY();
            }
            Triangles tri = makeBar((int) (struct.barLength - missingVals), struct.position - 1, length, new Color(element.getTriangleDetails().get(0).c0.getAsInt()));
            missingVals -= length;
            this.content.addItemToRender(tri);
        }
        return this;
    }

    public BarChart removeAllBars() {
        LinkedList<Triangles> items = new LinkedList<>(this.content.triangles.getItemsToRender());
        for (Triangles tri : items) {
            this.content.triangles.removeItemToRender(tri);
        }
        return this;
    }

    public BarChart changeRotation(final int chartAlignment) {
        this.chartAlignment = chartAlignment;
        return this;
    }

    // add stacks

    // TODO discuss interaction methods - when ScatterPlot is finished
    /*    - panning
          - scrolling - e.g. https://observablehq.com/@d3/zoomable-bar-chart, 0 is always 0 - panning only via x/y dir
          - clicking on bar
          - "focus" on bar
          -
    */

    // TODO zwei y bzw. x Achsen für Beschreibungen
    // TODO bars stacking on top of each other
    // TODO gruppierungen - idea: https://kb.tableau.com/articles/howto/creation-of-a-grouped-bar-chart

    // TODO text wraparound, when too long

    public CompleteRenderer getContent() {
        return content;
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public BarRenderer getBarRenderer() {
        return barRenderer;
    }

    public int getChartAlignment() {
        return chartAlignment;
    }

    public double getBarSize() {
        return barSize;
    }
}
