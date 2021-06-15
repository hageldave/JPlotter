package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.kml.CoordSysPanning;
import hageldave.jplotter.interaction.kml.CoordSysScrollZoom;
import hageldave.jplotter.interaction.kml.CoordSysViewSelector;
import hageldave.jplotter.interaction.kml.DynamicCoordsysScrollZoom;
import hageldave.jplotter.interaction.kml.KeyMaskListener;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.DataModel;
import hageldave.jplotter.util.PickingRegistry;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
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
 * CHANGELOG
 * - legend now also has a interaction interface (+ its elements are added to registry, etc. pp)
 * - KeylistenerMask has now its own class and is implemented in all interaction interfaces
 * - KeylistenerMask now can store > 1 keys
 * - ExtendedPointDetails stores now its label and Points
 * - update legend automatically if data is added
 * - Continued working on Scatterplot demo class
 *
 * Changelog
 *  - Barchart Implemenation
 *  - Bug mit Legenden items und focus gefixt - Registry reduziert
 *  - neue DataAdd Methode
 *  - Verbesserungen bei InteractionInterface
 *
 * @author lucareichmann
 */
public class ScatterPlot {
    protected JPlotterCanvas canvas;
    protected CoordSysRenderer coordsys;
    protected CompleteRenderer content;
    final protected HashMap<Integer, RenderedPoints> pointsInRenderer = new HashMap<>();
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();
    final protected ScatterPlotModel<Object> selectedItem = new ScatterPlotModel<>();
    final protected ScatterPlotModel<Object> hoveredItem = new ScatterPlotModel<>();

    public ScatterPlot(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public ScatterPlot(final boolean useOpenGL, final String xLabel, final String yLabel) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), xLabel, yLabel);
    }

    public ScatterPlot(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
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
    }

    public static class ArrayInformation {
        public final double[][] array;
        public final int xLoc;
        public final int yLoc;

        public ArrayInformation(final double[][] array, final int xLoc, final int yLoc) {
            this.array = array;
            this.xLoc = xLoc;
            this.yLoc = yLoc;
        }
    }

    /**
     * used for encapsulating all data interesting for the developer
     */
    public static class ExtendedPointDetails extends Points.PointDetails {
        public final Points.PointDetails point;
        public final ArrayInformation arrayInformation;
        public final double arrayIndex;
        public final Glyph glyph;
        public final Points pointSet;
        public final String description;

        ExtendedPointDetails(final Points.PointDetails point, final Glyph glyph, final Points pointSet,
                             final ArrayInformation arrayInformation, final double arrayIndex, final String description) {
            super(point.location);
            this.glyph = glyph;
            this.pointSet = pointSet;
            this.point = point;
            this.arrayInformation = arrayInformation;
            this.arrayIndex = arrayIndex;
            this.description = description;
        }
    }

    /**
     * Internal data structure to store information regarding color, glyph and description of data points.
     * This is used for displaying points (and their information) in the legend.
     *
     */
    public static class RenderedPoints {
        public Points points;
        public Color color;
        public String description;
        public ArrayInformation arrayInformation;

        RenderedPoints(final Points points, final Color color, final String description, final ArrayInformation arrayInformation) {
            this.points = points;
            this.color = color;
            this.description = description;
            this.arrayInformation = arrayInformation;
        }
    }

    /**
     * adds a set of points to the scatter plot.
     *
     * @param ID     the ID is the key with which the Dataset will be stored in the pointMap
     * @param points a double array containing the coordinates of the points
     * @param glyph  the data points will be visualized by that glyph
     * @param color  the color of the glyph
     * @return the old Scatterplot for chaining
     */
    public Points addData(final int ID, final double[][] points, final int xLoc, final int yLoc, final DefaultGlyph glyph,
                          final Color color, final String descr) {
        Points tempPoints = new Points(glyph);
        ArrayInformation arrayInformation = new ArrayInformation(points, xLoc, yLoc);
        int index = 0;
        for (double[] entry : points) {
            double x = entry[xLoc], y = entry[yLoc];
            Points.PointDetails pointDetail = tempPoints.addPoint(x, y);
            pointDetail.setColor(color);
            addItemToRegistry(new ExtendedPointDetails(pointDetail, glyph, tempPoints, arrayInformation, index, descr));
            index++;
        }
        this.pointsInRenderer.put(ID, new RenderedPoints(tempPoints, color, descr, arrayInformation));
        this.content.addItemToRender(tempPoints);
        updateLegends(glyph, color, descr);
        return tempPoints;
    }

    public Points addData(final int ID, final double[][] points, final DefaultGlyph glyph,
                          final Color color, final String descr) {
        return addData(ID, points, 0, 1, glyph, color, descr);
    }

    public Points addData(final int ID, final double[][] points, final DefaultGlyph glyph,
                          final Color color) {
        return addData(ID, points, glyph, color, "undefined");
    }

    public ScatterPlot alignCoordsys(final int padding) {
        ScatterPlot old = this;
        double minX = Integer.MAX_VALUE; double maxX = Integer.MIN_VALUE; double minY = Integer.MAX_VALUE; double maxY = Integer.MIN_VALUE;
        for (RenderedPoints points: pointsInRenderer.values()) {
            minX = Math.min(minX, points.points.getBounds().getMinX());
            maxX = Math.max(maxX, points.points.getBounds().getMaxX());
            minY = Math.min(minY, points.points.getBounds().getMinY());
            maxY = Math.max(maxY, points.points.getBounds().getMaxY());
        }
        this.coordsys.setCoordinateView(minX - padding, minY - padding,
                maxX + padding, maxY + padding);
        return old;
    }

    public ScatterPlot alignCoordsys() {
        return alignCoordsys(1);
    }

    public Legend addLegendRight(final int width, final boolean autoAddItems) {
        Legend legend = new Legend();
        coordsys.setLegendRightWidth(width);
        coordsys.setLegendRight(legend);
        if (autoAddItems) {
            for (RenderedPoints point: pointsInRenderer.values()) {
                int registryID = this.pickingRegistry.getNewID();
                Legend.GlyphLabel glyphLabel = new Legend.GlyphLabel(point.description, point.points.glyph, point.color.getRGB(), registryID);
                legend.addGlyphLabel(point.points.glyph, point.color.getRGB(), point.description, registryID);
                this.pickingRegistry.register(glyphLabel, registryID);
            }
        }
        return legend;
    }

    public Legend addLegendRight(final int width) {
        return addLegendRight(width, true);
    }

    public Legend addLegendBottom(final int height, final boolean autoAddItems) {
        Legend legend = new Legend();
        coordsys.setLegendBottomHeight(height);
        coordsys.setLegendBottom(legend);
        if (autoAddItems) {
            for (RenderedPoints point: pointsInRenderer.values()) {
                int registryID = this.pickingRegistry.getNewID();
                Legend.GlyphLabel glyphLabel = new Legend.GlyphLabel(point.description, point.points.glyph, point.color.getRGB(), registryID);
                legend.addGlyphLabel(point.points.glyph, point.color.getRGB(), point.description, registryID);
                this.pickingRegistry.register(glyphLabel, registryID);
            }
        }
        return legend;
    }

    public Legend addLegendBottom(final int height) {
        return addLegendBottom(height, true);
    }

    protected ScatterPlot updateLegends(final DefaultGlyph glyph, final Color color, final String descr) {
        Legend legendBottom = (Legend) this.getCoordsys().getLegendBottom();
        Legend legendRight = (Legend) this.getCoordsys().getLegendRight();
        int registryID = this.pickingRegistry.getNewID();
        if (legendBottom != null)
            legendBottom.addGlyphLabel(glyph, color.getRGB(), descr, registryID);
        if (legendRight != null)
            legendRight.addGlyphLabel(glyph, color.getRGB(), descr, registryID);
        this.pickingRegistry.register(new Legend.GlyphLabel(descr, glyph, color.getRGB(), registryID),
                registryID);
        return this;
    }

    /**
     * Adds a scroll zoom to the Scatterplot
     *
     * @return the {@link CoordSysScrollZoom} so that it can be further customized
     */
    public DynamicCoordsysScrollZoom addScrollZoom() {
        return new DynamicCoordsysScrollZoom(this.canvas, this.coordsys).register();
    }

    public CoordSysScrollZoom addScrollZoom(final KeyMaskListener keyListenerMask) {
        return new CoordSysScrollZoom(this.canvas, this.coordsys, keyListenerMask).register();
    }

    /**
     *
     * Adds panning functionality to the Scatterplot
     *
     * @return the {@link CoordSysPanning} so that it can be further customized
     */
    public CoordSysPanning addPanning() {
        return new CoordSysPanning(this.canvas, this.coordsys).register();
    }

    public CoordSysPanning addPanning(final KeyMaskListener keyListenerMask) {
        return new CoordSysPanning(this.canvas, this.coordsys, keyListenerMask).register();
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

    public CoordSysViewSelector addZoomViewSelector(final KeyMaskListener keyListenerMask) {
        return new CoordSysViewSelector(this.canvas, this.coordsys, keyListenerMask) {
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
    public PointClickedInterface printPointMouseOver() {
        return (PointClickedInterface) new PointClickedInterface() {
            @Override
            public void pointClicked(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {

            }

            @Override
            public void pointReleased(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {

            }

            @Override
            public void mouseOverPoint(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {
                System.out.println("Mouse location: " + mouseLocation);
                System.out.println("Point location: " + pointLocation);
                System.out.println("Data array: " + Arrays.deepToString(pointDetails.arrayInformation.array));
                System.out.println("Data index: " + pointDetails.arrayIndex);
            }

            @Override
            public void mouseLeftPoint(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {
                System.out.println("Mouse left point");
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
            public void pointClicked(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {
                System.out.println("Mouse location: " + mouseLocation);
                System.out.println("Point location: " + pointLocation);
                System.out.println("Data array: " + Arrays.deepToString(pointDetails.arrayInformation.array));
                System.out.println("Data index: " + pointDetails.arrayIndex);
            }

            @Override
            public void pointReleased(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {
                System.out.println("Mouse left point");
            }

            @Override
            public void mouseOverPoint(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {

            }

            @Override
            public void mouseLeftPoint(Point mouseLocation, Point2D pointLocation, ExtendedPointDetails pointDetails) {

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

    public HashMap<Integer, RenderedPoints> getPointsInRenderer() {
        return pointsInRenderer;
    }

    public RenderedPoints getPointInRenderer(final int index) {
        return pointsInRenderer.get(index);
    }

    /**
     * Adds point to picking registry
     *
     * @param point to be added
     */
    protected void addItemToRegistry(ExtendedPointDetails item) {
        int tempID = this.pickingRegistry.getNewID();
        item.point.setPickColor(tempID);
        this.pickingRegistry.register(item, tempID);
    }

    protected static class ScatterPlotModel<T> extends DataModel<T> {
        protected T previousValue;
        protected T notNull;

        public ScatterPlotModel() {
            super();
            this.addValueListener(e -> {
                if (e != null) {
                    if (notNull != null)
                        this.previousValue = this.notNull;
                    this.notNull = e;
                }
            });
        }
    }

    protected abstract class InteractionInterface extends MouseAdapter {
        protected KeyMaskListener keyListenerMask;

        public InteractionInterface(final KeyMaskListener keyListenerMask) {
            this.keyListenerMask = keyListenerMask;
        }

        public InteractionInterface() {
            this(new KeyMaskListener(0));
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
            if (!Arrays.asList(canvas.asComponent().getKeyListeners()).contains(keyListenerMask))
                canvas.asComponent().addKeyListener(keyListenerMask);
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
            canvas.asComponent().removeKeyListener(keyListenerMask);
            return this;
        }
    }

    /**
     * Mouse over interface, which triggers its pointClicked method,
     * when clicking on a point in the coordsys.
     */
    public abstract class PointClickedInterface extends InteractionInterface {

        public PointClickedInterface(KeyMaskListener keyListenerMask) {
            super(keyListenerMask);
        }

        public PointClickedInterface() {
            super();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (keyListenerMask.isKeysPressed()) {
                selectedItem.setSelectedItem(pickingRegistry.lookup(canvas.getPixel(e.getX(), e.getY(), true, 5)));
                selectedItem.addValueListener(newValue -> {
                    if (selectedItem.previousValue instanceof ExtendedPointDetails)
                        pointReleased(e.getPoint(), ((ExtendedPointDetails) selectedItem.previousValue).location, (ExtendedPointDetails) selectedItem.previousValue);
                    if (newValue instanceof ExtendedPointDetails)
                        pointClicked(e.getPoint(), ((ExtendedPointDetails) newValue).location, (ExtendedPointDetails) newValue);
                });
            }
        }

       @Override
        public void mouseMoved(MouseEvent e) {
           if (keyListenerMask.isKeysPressed()) {
                hoveredItem.setSelectedItem(pickingRegistry.lookup(canvas.getPixel(e.getX(), e.getY(), true, 5)));
                hoveredItem.addValueListener(newValue -> {
                    if (hoveredItem.previousValue instanceof ExtendedPointDetails)
                        mouseLeftPoint(e.getPoint(), ((ExtendedPointDetails) hoveredItem.previousValue).location, (ExtendedPointDetails) hoveredItem.previousValue);
                    if (newValue instanceof ExtendedPointDetails) {
                        mouseOverPoint(e.getPoint(), ((ExtendedPointDetails) newValue).location, (ExtendedPointDetails) newValue);
                    }
                });
           }
        }

            /**
             * Will be called, when a data point is clicked on.
             *
             * @param mouseLocation location that was clicked
             * @param pointLocation location of the clicked point in the coordinate system
             * @param data          the data array where the data point was found
             * @param dataIndex     the index of the data point in the returned array
             */
        public abstract void pointClicked(final Point mouseLocation, final Point2D pointLocation, final ExtendedPointDetails pointDetails);
        public abstract void pointReleased(final Point mouseLocation, final Point2D pointLocation, final ExtendedPointDetails pointDetails);
        public abstract void mouseOverPoint(final Point mouseLocation, final Point2D pointLocation, final ExtendedPointDetails pointDetails);
        public abstract void mouseLeftPoint(final Point mouseLocation, final Point2D pointLocation, final ExtendedPointDetails pointDetails);
    }

    public abstract class LegendSelectedInterface extends InteractionInterface {

        public LegendSelectedInterface(KeyMaskListener keyListenerMask) {
            super(keyListenerMask);
        }

        public LegendSelectedInterface() {
            super();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (keyListenerMask.isKeysPressed()) {
                selectedItem.setSelectedItem(pickingRegistry.lookup(canvas.getPixel(e.getX(), e.getY(), true, 5)));
                selectedItem.addValueListener(newValue -> {
                    if (selectedItem.previousValue instanceof Legend.GlyphLabel)
                        legendItemReleased(e.getPoint(), (Legend.GlyphLabel) selectedItem.previousValue);
                    if (newValue instanceof Legend.GlyphLabel)
                        legendItemSelected(e.getPoint(), (Legend.GlyphLabel) newValue);
                });
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (keyListenerMask.isKeysPressed()) {
                hoveredItem.setSelectedItem(pickingRegistry.lookup(canvas.getPixel(e.getX(), e.getY(), true, 5)));
                hoveredItem.addValueListener(newValue -> {
                    if (hoveredItem.previousValue instanceof Legend.GlyphLabel)
                        legendItemLeft(e.getPoint(), (Legend.GlyphLabel) hoveredItem.previousValue);
                    if (newValue instanceof Legend.GlyphLabel)
                        legendItemHovered(e.getPoint(), (Legend.GlyphLabel) newValue);
                });
            }
        }

        public abstract void legendItemSelected(final Point mouseLocation, final Legend.GlyphLabel glyphLabel);
        public abstract void legendItemReleased(final Point mouseLocation, final Legend.GlyphLabel glyphLabel);
        public abstract void legendItemHovered(final Point mouseLocation, final Legend.GlyphLabel glyphLabel);
        public abstract void legendItemLeft(final Point mouseLocation, final Legend.GlyphLabel glyphLabel);
    }


    /**
     * This interface realizes a functionality, which returns all data points that were selected before.
     * The selection of the data points is realized by the @link{CoordSysViewSelector}, with the alt-key as the modifierMask.
     *
     * If the selections is done, the abstract pointsSelected interface is called with the parameters
     * 'Rectangle2D bounds, ArrayList<double[][]> data, ArrayList<Integer> dataIndices'.
     *
     */
    public abstract class PointsSelectedInterface {
        protected ArrayList<double[][]> data = new ArrayList<>();
        protected ArrayList<Double> dataIndices = new ArrayList<>();
        protected ArrayList<ExtendedPointDetails> points = new ArrayList<>();

        public PointsSelectedInterface(final KeyMaskListener keyListenerMask) {
            new CoordSysViewSelector(canvas, coordsys, keyListenerMask) {
                @Override
                public void areaSelected(double minX, double minY, double maxX, double maxY) {
                    calcPoints(minX, minY, maxX, maxY);
                    pointsSelected(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY), data, dataIndices, points);
                    data.clear(); dataIndices.clear(); points.clear();
                }
            }.register();
        }

        public PointsSelectedInterface() {
            new CoordSysViewSelector(canvas, coordsys) {
                @Override
                public void areaSelected(double minX, double minY, double maxX, double maxY) {
                    calcPoints(minX, minY, maxX, maxY);
                    pointsSelected(new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY), data, dataIndices, points);
                    data.clear(); dataIndices.clear(); points.clear();
                }
            }.register();
        }

        private void calcPoints(final double minX, final double minY, final double maxX, final double maxY) {
            for (final RenderedPoints points: pointsInRenderer.values()) {
                for (final double[] pointList : points.arrayInformation.array) {
                    double x = pointList[points.arrayInformation.xLoc], y = pointList[points.arrayInformation.yLoc];
                    if (x > minX && x < maxX && y > minY && y < maxY) {
                        ExtendedPointDetails element = (ExtendedPointDetails) pickingRegistry.lookup(canvas.getPixel((int) coordsys.transformCoordSys2AWT(new Point2D.Double(x, y), canvas.asComponent().getHeight()).getX(),
                                (int) coordsys.transformCoordSys2AWT(new Point2D.Double(x, y), canvas.asComponent().getHeight()).getY(), true, 5));
                        if (element != null) {
                            this.points.add(element);
                            this.dataIndices.add(element.arrayIndex);
                            this.data.add(element.arrayInformation.array);
                        }
                    }
                }
            }
        }

        /**
         * This method will be called, when a rectangle was selected and the mouse was released.
         *
         * @param bounds the selected rectangle
         * @param data the data sets where points where found
         * @param dataIndices the indices of the data inside the data arrays
         */
        public abstract void pointsSelected(Rectangle2D bounds, ArrayList<double[][]> data, ArrayList<Double> dataIndices, ArrayList<ExtendedPointDetails> points);
    }
}