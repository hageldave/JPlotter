package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.PickingRegistry;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class LineChart {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected CompleteRenderer content;
    final protected ArrayList<double[][]> dataAdded = new ArrayList<>();
    final protected HashMap<Integer, Lines> linesAdded = new HashMap<>();
    final protected PickingRegistry<Lines.SegmentDetails> lineRegistry = new PickingRegistry<>();
    final protected PickingRegistry<Points.PointDetails> pointRegistry = new PickingRegistry<>();

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
        this.content = new CompleteRenderer();
        this.coordsys.setCoordinateView(-1, -1, 1, 1);
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);

    }

    private class Dataset {
        protected Points.PointDetails point;
        protected double[][] array;
        protected double startIndex;
        protected double endIndex;

        Dataset(final Points.PointDetails point, final double[][] array, final double startIndex, final double endIndex) {
            this.point = point;
            this.array = array;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    /**
     * adds a set of points to the scatter plot.
     *
     */
    public Lines addLineSegment(final Integer ID, final double[][] data, final Color color) {
        int biggestX = 0; int smallestX = Integer.MAX_VALUE; int biggestY = 0; int smallestY = Integer.MAX_VALUE;
        Lines tempLine = new Lines();
        for (int i = 0; i < data.length-1; i++) {
            double x1 = data[i][0], x2 = data[i+1][0];
            double y1 = data[i][1], y2 = data[i+1][1];
            Lines.SegmentDetails segment = tempLine.addSegment(x1, y1, x2, y2);
            segment.setColor(color);
            addSegmentToRegistry(segment);
        }
        this.getCoordsys().setCoordinateView(smallestX, smallestY, biggestX, biggestY);
        this.dataAdded.add(data);
        this.content.addItemToRender(tempLine);
        this.linesAdded.put(ID, tempLine);
        return tempLine;
    }

    public Points addPoints(final double[][] data, final DefaultGlyph glyph, final Color color) {
        Points tempPoints = new Points(glyph);
        for (double[] entry : data) {
            double x = entry[0], y = entry[1];
            Points.PointDetails point = tempPoints.addPoint(x, y);
            point.setColor(color);
            addPointToRegistry(point);
        }
        this.content.addItemToRender(tempPoints);
        return tempPoints;
    }

    public Points highlightDatapoints(final DefaultGlyph glyph, final Color color) {
        Points tempPoints = new Points(glyph);
        for (double[][] data : dataAdded) {
            for (double[] entry: data) {
                double x = entry[0], y = entry[1];
                Points.PointDetails point = tempPoints.addPoint(x, y);
                point.setColor(color);
                addPointToRegistry(point);
            }
        }
        this.content.addItemToRender(tempPoints);
        return tempPoints;
    }

    public LineChart alignCoordsys() {
        LineChart old = this;
        double minX = Integer.MAX_VALUE; double maxX = 0; double minY = Integer.MAX_VALUE; double maxY = 0;
        for (Lines line: linesAdded.values()) {
            if (minX > line.getBounds().getMinX()) {
                minX = line.getBounds().getMinX();
            }
            if (maxX < line.getBounds().getMaxX()) {
                maxX = line.getBounds().getMaxX();
            }
            if (minY > line.getBounds().getMinY()) {
                minY = line.getBounds().getMinY();
            }
            if (maxY < line.getBounds().getMaxY()) {
                maxY = line.getBounds().getMaxY();
            }
        }
        this.coordsys.setCoordinateView(minX, minY, maxX, maxY);
        return old;
    }

    /**
     * Adds point to picking registry
     *
     * @param line to be added
     */
    protected void addSegmentToRegistry(Lines.SegmentDetails line) {
        int tempID = this.lineRegistry.getNewID();
        line.setPickColor(tempID);
        this.lineRegistry.register(line, tempID);
    }

    /**
     * Adds point to picking registry
     *
     * @param point to be added
     */
    protected void addPointToRegistry(Points.PointDetails point) {
        int tempID = this.pointRegistry.getNewID();
        point.setPickColor(tempID);
        this.pointRegistry.register(point, tempID);
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

    public CompleteRenderer getContent() {
        return content;
    }


    protected abstract class InteractionInterface extends MouseAdapter implements KeyListener {
        protected int startIndex = 0;
        protected int endIndex = 0;
        protected double[][] dataSet;
        protected int extModifierMask = 0;
        protected boolean keyTyped = false;

        protected boolean findSegment(final MouseEvent e) {
            Lines.SegmentDetails details = lineRegistry.lookup(canvas.getPixel(e.getX(), e.getY(), true, 5));
            if (details != null) {
                this.dataSet = setListAndIndices(details.p0, details.p1);
                if (this.dataSet != null) {
                    triggerInterfaceMethod(e.getPoint(), details, this.dataSet, this.startIndex, this.endIndex);
                    return true;
                }
            }
            return false;
        }

        protected double[][] setListAndIndices(final Point2D start, final Point2D end) {
            double[][] tempList;
            for (final double[][] pointList : dataAdded) {
                tempList = pointList;
                for (int i = 0; i < pointList.length; i++) {
                    double x = pointList[i][0], y = pointList[i][1];
                    if (pointList[i][0] == start.getX() && pointList[i][1] == start.getY()) {
                        if (pointList[i+1][0] == end.getX() && pointList[i+1][1] == end.getY()) {
                            this.endIndex++;
                            return tempList;
                        }
                    }
                    this.endIndex++; this.startIndex++;
                }
                this.startIndex = 0; this.endIndex = 0;
            }
            return null;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == extModifierMask && !keyTyped) {
                keyTyped = true;
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            keyTyped = false;
        }

        @Override
        public void keyTyped(KeyEvent e) {

        }

        /**
         * Adds this {@link CoordSysViewSelector} as {@link MouseListener} and
         * {@link MouseMotionListener} to the associated canvas.
         *
         * @return this for chaining
         */
        public InteractionInterface register() {
            if (!Arrays.asList(canvas.asComponent().getMouseListeners()).contains(this))
                canvas.asComponent().addMouseListener(this);
            if (!Arrays.asList(canvas.asComponent().getMouseMotionListeners()).contains(this))
                canvas.asComponent().addMouseMotionListener(this);
            if (!Arrays.asList(canvas.asComponent().getKeyListeners()).contains(this))
                canvas.asComponent().addKeyListener(this);
            return this;
        }

        /**
         * Removes this {@link CoordSysViewSelector} from the associated canvas'
         * mouse and mouse motion listeners.
         *
         * @return this for chaining
         */
        public InteractionInterface deRegister() {
            canvas.asComponent().removeMouseListener(this);
            canvas.asComponent().removeMouseMotionListener(this);
            canvas.asComponent().removeKeyListener(this);
            return this;
        }


        protected abstract void triggerInterfaceMethod(final Point mouseLocation, final Lines.SegmentDetails line,
                                                       final double[][] data, final int startIndex, final int endIndex);

    }

    public abstract class LineClickedInterface extends InteractionInterface {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (keyTyped || extModifierMask == 0) {
                if (!findSegment(e)) {
                    System.out.println("No data point found in your dataset");
                }
                this.startIndex = 0;
                this.endIndex = 0;
            }
        }

        @Override
        protected void triggerInterfaceMethod(Point mouseLocation, Lines.SegmentDetails line, double[][] data, int startIndex, int endIndex) {
            segmentClicked(mouseLocation, line, data, startIndex, endIndex);
        }

        public abstract void segmentClicked(final Point mouseLocation, final Lines.SegmentDetails line,
                                            final double[][] data, final int startIndex, final int endIndex);
    }

    public abstract class LineHoveredInterface extends InteractionInterface {
        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
            findSegment(e);
            this.startIndex = 0;
            this.endIndex = 0;
        }

        @Override
        protected void triggerInterfaceMethod(Point mouseLocation, Lines.SegmentDetails line, double[][] data, int startIndex, int endIndex) {
            segmentHovered(mouseLocation, line, data, startIndex, endIndex);
        }

        public abstract void segmentHovered(final Point mouseLocation, final Lines.SegmentDetails line,
                                            final double[][] data, final int startIndex, final int endIndex);
    }

    public abstract class PointsSelectedInterface {
        protected ArrayList<double[][]> data = new ArrayList<>();
        protected ArrayList<Integer> dataIndices = new ArrayList<>();

        public PointsSelectedInterface() {
            new CoordSysViewSelector(canvas, coordsys) {
                {extModifierMask= InputEvent.ALT_DOWN_MASK;}
                @Override
                public void areaSelected(double minX, double minY, double maxX, double maxY) {
                    calcSegments(minX, minY, maxX, maxY);
                    pointsSelected(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY), data, dataIndices);
                    data.clear(); dataIndices.clear();
                }
            }.register();
        }

        protected void calcSegments(final double minX, final double minY, final double maxX, final double maxY) {
            int index = 0;
            for (final double[][] pointList : dataAdded) {
                for (double[] entry : pointList) {
                    double x = entry[0], y = entry[1];
                    if (x > minX && x < maxX && y > minY && y < maxY) {
                        this.dataIndices.add(index);
                        this.data.add(pointList);
                    }
                    index++;
                }
                index = 0;
            }
        }

        public abstract void pointsSelected(Rectangle2D bounds, ArrayList<double[][]> data, ArrayList<Integer> dataIndices);
    }
}
