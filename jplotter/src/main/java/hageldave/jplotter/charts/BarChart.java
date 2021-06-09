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
    // renderedGroups has to be passed to renderer somehow, so that it knows when to add lines

    // add legend

    // renderer und barchart vereinen
    final public TreeMap<Integer, BarGroup> renderedGroups = new TreeMap<>();
    final protected double barSize = 0.8;
    protected CompleteRenderer content;
    protected JPlotterCanvas canvas;
    protected BarRenderer barRenderer;
    protected int chartAlignment;

    protected int currentRow = 0;

    public static class BarStruct {
        final public LinkedList<Triangles> stacks = new LinkedList<>();
        final public LinkedList<Double> dataSets = new LinkedList<>();
        public String descr;
        public int position;
        public double barLength;
        public int ID;

        public BarStruct(final Triangles stacks, final int position, final String descr, final double dataSet, final int ID) {
            this.stacks.add(stacks);
            this.descr = descr;
            this.position = position;
            this.dataSets.add(dataSet);
            this.ID = ID;
        }

        public BarStruct(final Triangles stacks, final int position, final String descr, final double dataSet, final double barLength, final int ID) {
            this.stacks.add(stacks);
            this.descr = descr;
            this.position = position;
            this.barLength = barLength;
            this.dataSets.add(dataSet);
            this.ID = ID;
        }
    }

    // TODO soll renderable sein, aber wie Lines, etc
    public class BarGroup {
        // use hashmap as model and add listener -> everytime it is changed, update sortedBars
        final protected TreeMap<Integer, BarStruct> groupedBars = new TreeMap<>();
        protected SortedSet<BarStruct> sortedBars = new TreeSet<>(Comparator.comparingDouble(o -> o.barLength));
        protected int startingRow;

        public BarGroup() {
            this.startingRow = ++currentRow;
        }

        public BarGroup(final TreeMap<Integer, BarStruct> groupedBars) {
            this.groupedBars.putAll(groupedBars);
        }

        public BarGroup addBar(final int ID, final double[] data, final Color color, final String descr) {
            double val = Arrays.stream(data).sum();
            return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{descr});
        }

        public BarGroup addBar(final int ID, final double val, final Color color, final String descr) {
            return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{descr});
        }

        public BarGroup addBar(final int ID, final double val, final Color color) {
            return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{""});
        }

        public BarGroup addData(final int[] IDs, final double[] data, final Color[] color, final String[] descr) {
            if (!(IDs.length == data.length && data.length == color.length && color.length == descr.length))
                throw new IllegalArgumentException("All arrays have to have equal size!");
            for (int i = 0; i < data.length; i++) {
                if (this.groupedBars.containsKey(IDs[i])) {
                    addStack(IDs[i], data[i], color[i]);
                } else {
                    Triangles triangleRend = makeBar(currentRow++, data[i], color[i]);
                    this.groupedBars.put(IDs[i],
                            new BarStruct(triangleRend, currentRow, descr[i], data[i], data[i], IDs[i]));
                    copyContent(sortedBars, groupedBars.values());
                    renderBars();
                }
            }
            return this;
        }

        public BarGroup addData(final int[] IDs, final double[][] data, final Color[] color, final String[] descr) {
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
                content.addItemToRender(triangleRend);
                j++;
            }
            return this;
        }

        protected BarGroup addStack(final Integer ID, final double data, final Color color) {
            Triangles newStack = makeBar(this.groupedBars.get(ID).barLength,
                    this.groupedBars.get(ID).position - 1, data, color);
            this.groupedBars.get(ID).stacks.add(newStack);
            this.groupedBars.get(ID).dataSets.add(data);
            this.groupedBars.get(ID).barLength += data;
            copyContent(sortedBars, groupedBars.values());
            renderBars();
            return this;
        }

        public BarGroup removeBars(final int... IDs) {
            clearRenderedTriangles();
            for (int ID: IDs) {
                this.groupedBars.remove(ID);
            }
            currentRow -= IDs.length;
            copyContent(sortedBars, groupedBars.values());
            renderBars();
            return this;
        }

        // orders by height
        public BarGroup sortBars() {
            sortBars((o1, o2) -> {
                if (o1.barLength < o2.barLength) return -1;
                if (o1.barLength > o2.barLength) return 1;
                return 0;
            });
            return this;
        }

        public BarGroup sortBars(final Comparator<BarStruct> comparator) {
            clearRenderedTriangles();
            this.sortedBars = new TreeSet<>(comparator);
            copyContent(this.sortedBars, groupedBars.values());
            renderBars();
            return this;
        }

        public BarGroup sortBarsIDs() {
            clearRenderedTriangles();
            this.sortedBars = new TreeSet<>((o1, o2) -> {
                if (o1.ID > o2.ID) return 1;
                if (o1.ID < o2.ID) return -1;
                return 0;
            });
            copyContent(this.sortedBars, groupedBars.values());
            renderBars();
            return this;
        }

        protected void copyContent(final Collection<BarStruct> c1,
                                   final Collection<BarStruct> c2) {
            c1.clear(); c1.addAll(c2);
        }

        public TreeMap<Integer, BarStruct> getBarsInGroup() {
            return groupedBars;
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

    public BarGroup createGroup(final int ID) {
        BarGroup barGroup = new BarGroup();
        this.renderedGroups.put(ID, barGroup);
        return barGroup;
    }

    protected TickMarkGenerator setTickmarks() {
        String[] description = new String[currentRow];
        for (BarGroup group: renderedGroups.values()) {
            for (int key: group.groupedBars.keySet()) {
                int pos = group.groupedBars.get(key).position - 1;
                description[pos] = group.groupedBars.get(key).descr;
            }
        }

        for (int j = 0; j < description.length; j++) {
            if (description[j] == null)
                description[j] = "";
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
        } else if (this.chartAlignment == AlignmentConstants.VERTICAL) {
            bar.addQuad(new Rectangle2D.Double(row-(barSize/2), start, barSize, val));
        }
        bar.getTriangleDetails().forEach(tri -> tri.setColor(color));
        return bar;
    }

    // sets everything together
    public BarChart renderBars() {
        AtomicInteger index = new AtomicInteger();
        for (BarGroup group: renderedGroups.values()) {
            for (BarStruct struct: group.sortedBars) {
                reconstructBars(index, struct);
            }
            index.incrementAndGet();
        }
        setTickmarks();
        return this;
    }

    public BarChart setBarContentBoundaries() {
        double maxVal = Integer.MIN_VALUE; double minVal = Integer.MAX_VALUE;
        for (BarGroup value: renderedGroups.values()) {
            for (BarStruct struct: value.groupedBars.values()) {
                maxVal = Math.max(struct.barLength, maxVal);
                minVal = Math.min(struct.barLength, minVal);
            }
        }
        if (minVal >= 0) {
            minVal = 0.5;
        }
        if (this.chartAlignment == AlignmentConstants.HORIZONTAL) {
            this.barRenderer.setCoordinateView(minVal - 0.5, -0.8, maxVal + 0.5, currentRow);
        } else if (this.chartAlignment == AlignmentConstants.VERTICAL) {
            this.barRenderer.setCoordinateView(-0.8, minVal - 0.5, currentRow, maxVal + 0.5);
        }
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

    protected BarChart clearRenderedTriangles() {
        LinkedList<Triangles> items = new LinkedList<>(this.content.triangles.getItemsToRender());
        for (Triangles tri : items) {
            this.content.triangles.removeItemToRender(tri);
        }
        return this;
    }

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
