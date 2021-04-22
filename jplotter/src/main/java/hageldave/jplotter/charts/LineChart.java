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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class LineChart {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected CompleteRenderer content;
    final protected ArrayList<double[][]> dataAdded = new ArrayList<>();
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

    /**
     * adds a set of points to the scatter plot.
     *TODO set correct coordinate view soze after adding data
     */
    public Lines addLineSegment(final double[][] data, final Color color) {
        int biggestX = 0; int smallestX = Integer.MAX_VALUE; int biggestY = 0; int smallestY = Integer.MAX_VALUE;
        Arrays.sort(data, Comparator.comparingDouble(o -> o[0]));
        Lines tempLine = new Lines();
        for (int i = 0; i < data.length-1; i++) {
            double x1 = data[i][0], x2 = data[i+1][0];
            double y1 = data[i][1], y2 = data[i+1][1];
            Lines.SegmentDetails segment = tempLine.addSegment(x1, y1, x2, y2);
            segment.setColor(color);
            addSegmentToRegistry(segment);

            if (x1 > biggestX) {
                biggestX = (int) x1;
            }
            if (x1 < smallestX) {
                smallestX = (int) x1;
            }

            if (y1 > biggestY) {
                biggestY = (int) y1;
            }
            if (smallestY > y1) {
                smallestY = (int) y1;
            }

            if (x2 > biggestX) {
                biggestX = (int) x2;
            }
            if (x2 < smallestX) {
                smallestX = (int) x2;
            }

            if (y2 > biggestY) {
                biggestY = (int) y2;
            }
            if (smallestY > y2) {
                smallestY = (int) y2;
            }
        }
        this.getCoordsys().setCoordinateView(smallestX, smallestY, biggestX, biggestY);
        this.dataAdded.add(data);
        this.content.addItemToRender(tempLine);
        return tempLine;
    }

    // TODO is this needed? maybe just set points from already added data for lines
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


    protected abstract class InteractionInterface extends MouseAdapter {
        protected int startIndex = 0;
        protected int endIndex = 0;
        protected double[][] dataSet;

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
            return this;
        }


        protected abstract void triggerInterfaceMethod(final Point mouseLocation, final Lines.SegmentDetails line,
                                                       final double[][] data, final int startIndex, final int endIndex);

    }

    public abstract class LineClickedInterface extends InteractionInterface {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!findSegment(e)) {
                System.out.println("No data point found in your dataset");
            }
            this.startIndex = 0;
            this.endIndex = 0;
        }

        @Override
        protected void triggerInterfaceMethod(Point mouseLocation, Lines.SegmentDetails line, double[][] data, int startIndex, int endIndex) {
            segmentClicked(mouseLocation, line, data, startIndex, endIndex);
        }

        public abstract void segmentClicked(final Point mouseLocation, final Lines.SegmentDetails line,
                                            final double[][] data, final int startIndex, final int endIndex);
    }
}
