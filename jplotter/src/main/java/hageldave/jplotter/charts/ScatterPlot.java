package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.PointsRenderer;
import hageldave.jplotter.util.PickingRegistry;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The ScatterPlot class provides an easy way to quickly create a ScatterPlot.
 * It includes a JPlotterCanvas, CoordSysRenderer and a PointsRenderer,
 * which are all set up automatically.
 * To edit those, they can be returned with their respective getter methods.
 * When data points are added to the ScatterPlot, they are stored in the pointMap.
 * <p>
 * To add a Dataset to the pointMap, an ID has to be defined as a key.
 * With this ID the Dataset can be removed later on.
 * <p>
 *
 * @author lucareichmann
 */
public class ScatterPlot {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected PointsRenderer content;
    final protected ArrayList<double[][]> dataAdded;
    final protected HashMap<Integer, Points> pointsInRenderer;
    final protected PickingRegistry<Points.PointDetails> registry = new PickingRegistry<>();

    public ScatterPlot(final boolean useOpenGL) {
        this.pointsInRenderer = new HashMap<Integer, Points>();
        this.dataAdded = new ArrayList<double[][]>();
        this.canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        setupScatterPlot();
    }

    public ScatterPlot(final boolean useOpenGL, final JPlotterCanvas canvas) {
        this.pointsInRenderer = new HashMap<Integer, Points>();
        this.dataAdded = new ArrayList<double[][]>();
        this.canvas = canvas;
        setupScatterPlot();
    }

    /**
     * Helper method to set the initial scatter plot.
     */
    protected void setupScatterPlot() {
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.coordsys = new CoordSysRenderer();
        this.content = new PointsRenderer();
        this.coordsys.setCoordinateView(-1, -1, 1, 1);
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);
    }

    /**
     * adds a set of points to the scatter plot.
     *
     * @param ID     the ID is the key with which the Dataset will be stored in the pointMap
     * @param points a double array containing the coordinates of the points TODO Spezifikation hier verbessern zu x,y
     * @param glyph  the data points will be visualized by that glyph
     * @param color  the color of the glyph
     * @return the old Scatterplot for chaining
     */
    public Points addPoints(final int ID, final double[][] points, final DefaultGlyph glyph, final Color color) {
        Points tempPoints = new Points(glyph);
        for (double[] entry : points) {
            double x = entry[0], y = entry[1];
            Points.PointDetails point = tempPoints.addPoint(x, y);
            point.setColor(color);
            addItemToRegistry(point);
        }
        this.pointsInRenderer.put(ID, tempPoints);
        this.dataAdded.add(points);
        this.content.addItemToRender(tempPoints);
        return tempPoints;
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

            @Override
            public void areaSelected(double minX, double minY, double maxX, double maxY) {
                coordsys.setCoordinateView(minX, minY, maxX, maxY);
            }
        }.register();
    }

    /**
     * Adds a (already implemented) mouse movement listener,
     * which is notified, when the mouse moves over a point.
     *
     * @return listener class
     */
    public MouseOverInterface printPointMouseOver() {
        return (MouseOverInterface) new MouseOverInterface() {
            @Override
            public void mouseOverPoint(Point mouseLocation, Point2D pointLocation, double[][] data, int dataIndex) {
                System.out.println("Mouse location: " + mouseLocation);
                System.out.println("Point location: " + pointLocation);
                System.out.println("Data array: " + Arrays.deepToString(data));
                System.out.println("Data index: " + dataIndex);
            }
        }.register();
    }

    /**
     * Adds a (already implemented) click listener,
     * which is notified, when a point is clicked.
     *
     * @return listener class
     */
    public PointClickedInterface printPointClicked() {
        return (PointClickedInterface) new PointClickedInterface() {
            @Override
            public void pointClicked(Point mouseLocation, Point2D pointLocation, double[][] data, int dataIndex) {
                System.out.println("Mouse location: " + mouseLocation);
                System.out.println("Point location: " + pointLocation);
                System.out.println("Data array: " + Arrays.deepToString(data));
                System.out.println("Data index: " + dataIndex);
            }
        }.register();
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public CoordSysRenderer getCoordsys() {
        return coordsys;
    }

    public PointsRenderer getContent() {
        return content;
    }

    public HashMap<Integer, Points> getPointsInRenderer() {
        return pointsInRenderer;
    }

    /**
     * Adds point to picking registry
     *
     * @param point to be added
     */
    protected void addItemToRegistry(Points.PointDetails point) {
        int tempID = this.registry.getNewID();
        point.setPickColor(tempID);
        this.registry.register(point, tempID);
    }

    protected abstract class InteractionInterface extends MouseAdapter {
        protected int index = 0;
        protected double[][] dataSet;

        // TODO Optimieren bei Hinzufügen zu Registry Index + Liste speichern
        /**
         * Searches for a data point similar to the location the developer clicked on.
         *
         * @param e MouseEvent when clicking
         * @return true if a point was found, false if no point was found in the dataSet
         */
        protected boolean findPoints(final MouseEvent e) {
            Points.PointDetails details = registry.lookup(canvas.getPixel(e.getX(), e.getY(), true, 5));
            if (details != null) {
                Point2D location = details.location;
                this.dataSet = getListAndSetIndex(location);
                if (this.dataSet != null) {
                    triggerInterfaceMethod(e.getPoint(), location, this.dataSet, this.index);
                    return true;
                }
            }
            return false;
        }

        /**
         * Finds the data array which contains the clicked-on data point.
         *
         * @param location clicked in the coordinate system
         * @return list where the data point was found
         */
        protected double[][] getListAndSetIndex(final Point2D location) {
            double[][] tempList;
            for (final double[][] pointList : dataAdded) {
                tempList = pointList;
                for (double[] entry : pointList) {
                    double x = entry[0], y = entry[1];
                    if (x == location.getX() && y == location.getY()) {
                        return tempList;
                    }
                    this.index++;
                }
                this.index = 0;
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

        /**
         * Triggers the specific interface method in each interface, when implemented.
         *
         * @param mouseLocation location that was clicked
         * @param pointLocation location of the clicked point in the coordinate system
         * @param data          the data array where the data point was found
         * @param dataIndex     the index of the data point in the returned array
         */
        protected abstract void triggerInterfaceMethod(final Point mouseLocation, final Point2D pointLocation,
                                                       final double[][] data, final int dataIndex);

    }


    /**
     * Mouse over interface, which triggers its pointClicked method,
     * when clicking on a point in the coordsys.
     *
     */
    public abstract class PointClickedInterface extends InteractionInterface {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!findPoints(e)) {
                System.out.println("No data point found in your dataset");
            }
            this.index = 0;
        }

        @Override
        protected void triggerInterfaceMethod(Point mouseLocation, Point2D pointLocation, double[][] data, int dataIndex) {
            pointClicked(mouseLocation, pointLocation, data, dataIndex);
        }

        /**
         * Will be called, when a data point is clicked on.
         *
         * @param mouseLocation location that was clicked
         * @param pointLocation location of the clicked point in the coordinate system
         * @param data          the data array where the data point was found
         * @param dataIndex     the index of the data point in the returned array
         */
        public abstract void pointClicked(final Point mouseLocation, final Point2D pointLocation,
                                          final double[][] data, final int dataIndex);
    }

    /**
     * Mouse over interface, which triggers its mouseOverPoint method,
     * when hovering over a point in the coordsys.
     *
     */
    public abstract class MouseOverInterface extends InteractionInterface {
        @Override
        public void mouseMoved(MouseEvent e) {
            findPoints(e);
            this.index = 0;
        }

        @Override
        protected void triggerInterfaceMethod(Point mouseLocation, Point2D pointLocation, double[][] data, int dataIndex) {
            mouseOverPoint(mouseLocation, pointLocation, data, dataIndex);
        }

        /**
         * Will be called, when a data point is clicked on.
         *
         * @param mouseLocation location that was clicked
         * @param pointLocation location of the clicked point in the coordinate system
         * @param data          the data array where the data point was found
         * @param dataIndex     the index of the data point in the returned array
         */
        public abstract void mouseOverPoint(final Point mouseLocation, final Point2D pointLocation,
                                            final double[][] data, final int dataIndex);
    }

    public abstract class PointsSelectedInterface {
        protected ArrayList<double[][]> data = new ArrayList<double[][]>();
        protected ArrayList<Integer> dataIndices = new ArrayList<Integer>();

        public PointsSelectedInterface() {
            new CoordSysViewSelector(canvas, coordsys) {
                {extModifierMask=InputEvent.ALT_DOWN_MASK;}
                @Override
                public void areaSelected(double minX, double minY, double maxX, double maxY) {
                    calcPoints(minX, minY, maxX, maxY);
                    pointsSelected(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY), data, dataIndices);
                    data.clear(); dataIndices.clear();
                }
            }.register();
        }


        protected void calcPoints(final double minX, final double minY, final double maxX, final double maxY) {
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

