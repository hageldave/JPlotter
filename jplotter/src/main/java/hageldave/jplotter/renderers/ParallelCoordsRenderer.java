package hageldave.jplotter.renderers;


import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.TickMarkGenerator;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * This ParallelCoordsRenderer is a {@link Renderer} that is used to display a parallel coordinate graph.
 * To provide a way to define the vertical axes of the graph, there is the concept of "features" (see {@link Feature}).
 * <p>
 * Each feature has a top and a bottom axis label, which show the feature boundary values.
 * <p>
 * Optionally a {@link Renderer} for drawing a legend (such as the {@link Legend} class)
 * can be set to either the bottom or right hand side of the coordinate system (can also
 * use both areas at once).
 * Use {@link #setLegendBottom(Renderer)} or {@link #setLegendRight(Renderer)} to do so.
 * The legend area size can be partially controlled by {@link #setLegendBottomHeight(int)}
 * and {@link #setLegendRightWidth(int)} if this is needed.
 * <p>
 * The overlay renderer ({@link #setOverlay(Renderer)}) can be used to finally draw over all
 * the renderer viewport.
 */
public class ParallelCoordsRenderer implements Renderer {
    protected LinesRenderer preContentLinesR = new LinesRenderer();
    protected TextRenderer preContentTextR = new TextRenderer();
    protected LinesRenderer postContentLinesR = new LinesRenderer();
    protected TextRenderer postContentTextR = new TextRenderer();
    protected Renderer overlay;
    protected Renderer content=null;
    protected Renderer legendRight=null;
    protected Renderer legendBottom=null;

    protected int legendRightWidth = 70;
    protected int legendBottomHeight = 20;

    @Annotations.GLCoordinates
    protected Rectangle legendRightViewPort = new Rectangle();
    @Annotations.GLCoordinates
    protected Rectangle legendBottomViewPort = new Rectangle();

    @Annotations.GLCoordinates
    protected Rectangle currentViewPort = new Rectangle();

    protected Rectangle2D coordinateView = new Rectangle2D.Double(0,0,1,1);

    protected TickMarkGenerator tickMarkGenerator = new ExtendedWilkinson();

    protected Lines ticks = new Lines().setVertexRoundingEnabled(true);
    protected Lines guides = new Lines().setVertexRoundingEnabled(true);
    protected LinkedList<Text> tickMarkLabels = new LinkedList<>();

    protected LinkedList<Feature> features = new LinkedList<>();
    protected Pair<Integer, Feature> highlightedFeature = null;

    protected int viewportwidth=0;
    protected int viewportheight=0;
    protected boolean isDirty = true;

    protected IntSupplier tickColor;
    protected IntSupplier guideColor;
    protected IntSupplier textColor;

    protected int paddingLeft = 10;
    protected int paddingRight = 10;
    protected int paddingTop = 10;
    protected int paddingBot = 10;

    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaLB = new PointeredPoint2D();
    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaRT = Utils.copy(coordsysAreaLB);
    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaLT = new PointeredPoint2D(coordsysAreaLB.x, coordsysAreaRT.y);
    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaRB = new PointeredPoint2D(coordsysAreaRT.x, coordsysAreaLB.y);

    protected boolean isEnabled=true;

    protected ColorScheme colorScheme;

    /**
     * Sets up a ParallelCoordsRenderer with the default color scheme
     */
    public ParallelCoordsRenderer() {
        this.colorScheme = DefaultColorScheme.LIGHT.get();
        setupParallelCoordsRenderer();
    }

    /**
     * Sets up a ParallelCoordsRenderer with a custom color scheme
     *
     * @param colorScheme the custom color scheme
     */
    public ParallelCoordsRenderer(final ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
        setupParallelCoordsRenderer();
    }

    /**
     * Helper method to setup the ParallelCoordsRenderer
     */
    protected void setupParallelCoordsRenderer() {
        this.postContentLinesR
                .addItemToRender(guides)
                .addItemToRender(ticks);

        this.guideColor = ()->getColorScheme().getColor3();
        this.tickColor = ()->getColorScheme().getColor3();
        this.textColor = ()->getColorScheme().getColorText();

        updateColors();
    }

    /**
     * Helper method to update the colors if the color scheme is changed.
     * @return this for chaining
     */
    protected ParallelCoordsRenderer updateColors() {
        updateLegendColorScheme(legendBottom);
        updateLegendColorScheme(legendRight);

        setDirty();
        return this;
    }

    /**
     * Sets the {@link #isDirty} state of this ParallelCoordsRenderer to true.
     * This indicates that axis locations, tick marks, labels and guides
     * have to be recomputed.
     * The recomputing will be done during {@link #render(int, int, int, int)} which
     * will set the isDirty state back to false.
     */
    public void setDirty() {
        this.isDirty = true;
    }

    /**
     * Sets the content renderer that will draw into the area of the coordinate system.
     * @param content the content renderer
     * @return the previous content renderer (which may need to be closed to free GL resources),
     * null if none was set
     */
    public Renderer setContent(Renderer content) {
        Renderer old = this.content;
        this.content = content;
        return old;
    }

    /**
     * Sets the renderer that will draw the legend to the right of the coordinate system.
     * The view port area for this renderer will start at the top edge of the coordinate system,
     * be {@link #getLegendRightWidth()} wide and extend to the bottom (-padding).
     * @param legend renderer for right side of coordinate system
     * @return the previous legend renderer (which may need to be closed to free GL resources),
     * null if none was set
     */
    public Renderer setLegendRight(Renderer legend) {
        Renderer old = this.legendRight;
        this.legendRight = legend;

        // if the legend is of type Legend, a color scheme is automatically set
        updateLegendColorScheme(legend);
        return old;
    }

    /**
     * Sets the renderer that will draw the legend below the coordinate system.
     * The view port area for this renderer will start at the left edge of the coordinate system below
     * the axis tick labels, will be as wide as the x-axis and {@link #getLegendBottomHeight()} high.
     * @param legend renderer for bottom side of coordinate system
     * @return the previous legend renderer (which may need to be closed to free GL resources),
     * null if none was set
     */
    public Renderer setLegendBottom(Renderer legend) {
        Renderer old = this.legendBottom;
        this.legendBottom = legend;

        // if the legend is of type Legend, a color scheme is automatically set
        updateLegendColorScheme(legend);
        return old;
    }

    /**
     * @return all features (see {@link Feature}) currently displayed in the {@link ParallelCoordsRenderer}.
     */
    public LinkedList<Feature> getFeatures() {
        return features;
    }

    /**
     * Adds a {@link Feature} to the {@link ParallelCoordsRenderer}.
     *
     * @param feature to be added
     * @return this for chaining
     */
    public ParallelCoordsRenderer addFeature(Feature feature) {
        this.features.add(feature);
        return this;
    }

    /**
     * Adds multiple features (see {@link Feature}) to the {@link ParallelCoordsRenderer}.
     *
     * @param features to be added
     * @return this for chaining
     */
    public ParallelCoordsRenderer addFeature(Feature... features) {
        this.features.addAll(Arrays.asList(features));
        return this;
    }

    /**
     * Adds a {@link Feature} with the given parameters to the {@link ParallelCoordsRenderer}.
     *
     * @param min of the feature
     * @param max of the feature
     * @param label of the feature
     * @return this for chaining
     */
    public ParallelCoordsRenderer addFeature(double min, double max, String label) {
        return this.addFeature(new Feature(min, max, label));
    }

    /**
     * @return the currently highlighted feature of the {@link ParallelCoordsRenderer} and its index.
     */
    public Pair<Integer, Feature> getHighlightedFeature() {
        return highlightedFeature;
    }

    /**
     * Sets a highlighted {@link Feature} in the {@link ParallelCoordsRenderer} at the given index.
     * The highlighted feature acts as an overlay of the underlying feature at the given index.
     * As such it can't stick out the underlying feature and will be cropped if needed.
     *
     * @param highlightedFeature which pairs the highlighted feature and the index where it will be drawn
     */
    public void setHighlightedFeature(Pair<Integer, Feature> highlightedFeature) {
        this.highlightedFeature = highlightedFeature;
        this.setDirty();
    }

    /**
     * Removes the highlighted {@link Feature}.
     */
    public void setHighlightedFeature() {
        this.highlightedFeature = null;
    }

    /**
     * @param legend color scheme of the legend will be updated if it is from type {@link Legend}
     */
    protected void updateLegendColorScheme(final Renderer legend) {
        if (legend instanceof Legend) {
            ((Legend) legend).setColorScheme(getColorScheme());
        }
    }

    /**
     * Sets the overlay renderer that will draw at last in the sequence and thus
     * overlays the whole rendering.
     * @param overlayRenderer renderer responsible for the overlay
     * @return the previous overlay renderer (which may need to be closed to free GL resources),
     * null if none was set
     */
    public Renderer setOverlay(Renderer overlayRenderer) {
        Renderer old = overlayRenderer;
        this.overlay = overlayRenderer;
        return old;
    }

    /**
     * @return the {@link ColorScheme} of the ParallelCoordsRenderer.
     */
    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    /**
     * Sets a new color scheme on the ParallelCoordsRenderer.
     *
     * @param colorScheme new {@link ColorScheme} used by the ParallelCoordsRenderer.
     * @return new ParallelCoordsRenderer
     */
    public ParallelCoordsRenderer setColorScheme(final ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
        updateColors();
        return this;
    }

    /**
     * @return overlay. see {@link #setOverlay(Renderer)}
     */
    public Renderer getOverlay() {
        return overlay;
    }

    /**
     * @return the padding on the left side
     */
    public int getPaddingLeft() {
        return paddingLeft;
    }

    /**
     * @return the padding on the right side
     */
    public int getPaddingRight() {
        return paddingRight;
    }

    /**
     * @return the padding on the top side
     */
    public int getPaddingTop() {
        return paddingTop;
    }

    /**
     * @return the padding on the bottom side
     */
    public int getPaddingBot() {
        return paddingBot;
    }

    /**
     * Sets the padding to the left side, which is the size of the blank area
     * before any visible elements of the ParallelCoordsRenderer are drawn
     * @param padding amount of blank area
     * @return this for chaining
     */
    public ParallelCoordsRenderer setPaddingLeft(int padding) {
        this.paddingLeft = padding;
        this.setDirty();
        return this;
    }

    /**
     * Sets the padding to the right side, which is the size of the blank area
     * before any visible elements of the ParallelCoordsRenderer are drawn
     * @param padding amount of blank area
     * @return this for chaining
     */
    public ParallelCoordsRenderer setPaddingRight(int padding) {
        this.paddingRight = padding;
        this.setDirty();
        return this;
    }

    /**
     * Sets the padding to the top side, which is the size of the blank area
     * before any visible elements of the ParallelCoordsRenderer are drawn
     * @param padding amount of blank area
     * @return this for chaining
     */
    public ParallelCoordsRenderer setPaddingTop(int padding) {
        this.paddingTop = padding;
        this.setDirty();
        return this;
    }

    /**
     * Sets the padding to the bottom side, which is the size of the blank area
     * before any visible elements of the ParallelCoordsRenderer are drawn
     * @param padding amount of blank area
     * @return this for chaining
     */
    public ParallelCoordsRenderer setPaddingBot(int padding) {
        this.paddingBot = padding;
        this.setDirty();
        return this;
    }

    /**
     * @return bottom legend. see {@link #setLegendBottom(Renderer)}
     */
    public Renderer getLegendBottom() {
        return legendBottom;
    }

    /**
     * @return right legend. see {@link #setLegendRight(Renderer)}
     */
    public Renderer getLegendRight() {
        return legendRight;
    }

    /**
     * @return content. see {@link #setContent(Renderer)}
     */
    public Renderer getContent() {
        return content;
    }

    /**
     * Sets the height of the legend area below the coordinate system.
     * (width is determined by x-axis width)
     * @param legendBottomHeight height of the bottom legend area.
     * (default is 20px)
     * @return this for chaining
     */
    public ParallelCoordsRenderer setLegendBottomHeight(int legendBottomHeight) {
        this.legendBottomHeight = legendBottomHeight;
        return this;
    }

    /**
     * Sets the width of the legend area right to the coordinate system.
     * (height is determined by the space available until the bottom of the renderer's viewport)
     * @param legendRightWidth width of the right legend area.
     * (default is 70 px)
     * @return this for chaining
     */
    public ParallelCoordsRenderer setLegendRightWidth(int legendRightWidth) {
        this.legendRightWidth = legendRightWidth;
        return this;
    }

    /**
     * @return width of the width of the right hand side legend area.
     */
    public int getLegendRightWidth() {
        return legendRightWidth;
    }

    /**
     * @return height of the bottom side legend area.
     */
    public int getLegendBottomHeight() {
        return legendBottomHeight;
    }

    /**
     * Sets up pretty much everything.
     * <ul>
     * <li>the bounds of the coordinate system frame ({@link #coordsysAreaLB}, {@link #coordsysAreaRT})</li>
     * <li>the tick mark values and labels</li>
     * <li>the tick mark guides</li>
     * <li>the location of the axis labels</li>
     * <li>the areas for the legends (right and bottom legend)</li>
     * </ul>
     */
    protected void setupAndLayout() {
        final int tickfontSize = 11;
        final int yLabelfontSize = 12;
        final int style = Font.PLAIN;

        // find maximum length of y axis labels of first feature
        int maxYTickLabelWidth = 0;
        if (!features.isEmpty()) {
            Pair<double[], String[]> firstFeatTicksAndLabels = new Pair<>(new double[]{features.getFirst().bottom, features.getFirst().top},
                    new String[]{String.valueOf(features.getFirst().bottom), String.valueOf(features.getFirst().top)});

            for(String label : firstFeatTicksAndLabels.second) {
                int labelW = CharacterAtlas.boundsForText(label.length(), tickfontSize, style).getBounds().width;
                maxYTickLabelWidth = Math.max(maxYTickLabelWidth, labelW);
            }

            // get x dimension of first and last label to check if the coordsysarea has to be moved to make room for them
            int firstXLabelLength = CharacterAtlas.boundsForText(features.getFirst().label.length(), tickfontSize, Font.BOLD).getBounds().width;
            int lastXLabelLength = CharacterAtlas.boundsForText(features.getLast().label.length(), tickfontSize, Font.BOLD).getBounds().width;

            int maxXTickLabelHeight = CharacterAtlas.boundsForText(1, tickfontSize, style).getBounds().height;
            int maxLabelHeight = CharacterAtlas.boundsForText(1, yLabelfontSize, style).getBounds().height;

            int legendRightW = Objects.nonNull(legendRight) ? legendRightWidth + 4 : 0;
            int legendBotH = Objects.nonNull(legendBottom) ? legendBottomHeight + 4 : 0;

            // move coordwindow origin so that labels have enough display space
            coordsysAreaLB.x[0] = Math.max(firstXLabelLength / 2.0, maxYTickLabelWidth) + paddingLeft + 10;
            coordsysAreaLB.y[0] = maxXTickLabelHeight + paddingBot + legendBotH + 20;
            // move opposing corner of coordwindow to have enough display space
            coordsysAreaRT.x[0] = viewportwidth - paddingRight - Math.max(lastXLabelLength / 2.0, legendRightW)  - 10;
            coordsysAreaRT.y[0] = viewportheight - paddingTop - maxLabelHeight - 4;

            // dispose of old stuff
            clearCoordSysBuildingBlocks();

            // create new stuff
            Rectangle2D axisDimensions = preventCoordSysInversion();

            // should labels be shifted?
            boolean shiftLabels = false;
            double lastMaxX = Integer.MIN_VALUE;

            // xaxis feature guides
            int index = 0;
            for (Feature feature : features) {
                // tick
                double m = (double) index / (features.size() - 1);
                double x = coordsysAreaLB.getX() + m * axisDimensions.getWidth();
                Point2D onaxis = new Point2D.Double(Math.round(x), coordsysAreaLB.getY());

                // label
                Text label = new Text(feature.label, tickfontSize, Font.BOLD, this.textColor.getAsInt());
                Dimension textSize = label.getTextSize();
                label.setOrigin(new Point2D.Double(
                        (int) (onaxis.getX() - textSize.getWidth() / 2.0),
                        (int) (onaxis.getY() - 12 - textSize.getHeight()) + 0.5));

                if (lastMaxX > label.getBounds().getMinX()) {
                    shiftLabels = true;
                    break;
                }

                lastMaxX = label.getBounds().getMaxX();
                tickMarkLabels.add(label);

                // feature guide
                guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, axisDimensions.getHeight())).setColor(guideColor);

                Pair<Double[], String[]> tickLabels = new Pair<>(new Double[]{feature.bottom, feature.top},
                        new String[]{String.valueOf(feature.bottom), String.valueOf(feature.top)});

                Double[] yticks = tickLabels.first;
                String[] yticklabels = tickLabels.second;

                // yaxis ticks
                for (int i = 0; i < yticks.length; i++) {
                    // tick
                    double y_m = (yticks[i] - feature.bottom) / (feature.top - feature.bottom);

                    double y = y_m * axisDimensions.getHeight();
                    Point2D onYaxis = new TranslatedPoint2D(new PointeredPoint2D(0, coordsysAreaLB.getY()), x, Math.round(y));
                    ticks.addSegment(onYaxis, new TranslatedPoint2D(onYaxis, -4, 0)).setColor(tickColor);
                    // label
                    Text yLabel = new Text(yticklabels[i], tickfontSize, style, this.textColor.getAsInt());
                    Dimension yTextSize = yLabel.getTextSize();
                    yLabel.setOrigin(new TranslatedPoint2D(onYaxis, -7 - yTextSize.getWidth(), -Math.round(yTextSize.getHeight() / 2.0) + 0.5));

                    // we need to use a different one here, as tickMarkLabels get cleared when labels have to be shifted
                    tickMarkLabels.add(yLabel);
                }

                index++;
            }

            if (shiftLabels) {
                clearCoordSysBuildingBlocks();
                index = 0;
                for (Feature feature : features) {

                    // calculate first the new coord view bounds
                    String[] allLabels = new String[features.size()];
                    for (int i = 0; i < allLabels.length; i++)
                        allLabels[i] = features.get(i).label;

                    // calculate first the new coord view bounds
                    Rectangle2D rotatedXLblBounds = getMaxBoundsOfAllLabels(allLabels, Math.PI / 4, tickfontSize, style);

                    Text firstLabel = new Text(features.getFirst().label, tickfontSize, style, this.textColor.getAsInt());
                    firstLabel.setAngle(Math.PI / 4);
                    double labelWidth = firstLabel.getBoundsWithRotation().getWidth();

                    // add rotated label height to the y padding
                    coordsysAreaLB.y[0] = paddingBot + legendBotH + rotatedXLblBounds.getHeight() + 20;
                    // check if y or x label is larger and add that size to the padding
                    coordsysAreaLB.x[0] = Math.max(maxYTickLabelWidth, (int) labelWidth) + paddingLeft + 8;
                    coordsysAreaRT.x[0] = viewportwidth - paddingRight - maxLabelHeight - legendRightW - 8;

                    // correct the coordsysArea so that it doesn't get inverted
                    axisDimensions = preventCoordSysInversion();

                    // now really put it into renderer
                    double m = (double) index / (features.size() - 1);
                    double x = coordsysAreaLB.getX() + m * axisDimensions.getWidth();
                    Point2D onaxis = new Point2D.Double(Math.round(x), coordsysAreaLB.getY());
                    Text label = new Text(feature.label, tickfontSize, Font.BOLD, this.textColor.getAsInt());
                    label.setAngle(Math.PI / 4);
                    label.setOrigin(new Point2D.Double(
                            (int) onaxis.getX() - label.getBoundsWithRotation().getWidth() + 12,
                            (int) onaxis.getY() - label.getBoundsWithRotation().getHeight() - 7));

                    tickMarkLabels.add(label);
                    // guide
                    guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, axisDimensions.getHeight())).setColor(guideColor);

                    // yaxis ticks
                    Pair<Double[], String[]> tickLabels = new Pair<>(new Double[]{feature.bottom, feature.top},
                            new String[]{String.valueOf(feature.bottom), String.valueOf(feature.top)});

                    Double[] yticks = tickLabels.first;
                    String[] yticklabels = tickLabels.second;

                    // get max of the ticks to normalize them to the 0-1 area
                    for (int i = 0; i < yticks.length; i++) {
                        // tick
                        double y_m = (yticks[i] - feature.bottom) / (feature.top - feature.bottom);
                        double y = y_m * axisDimensions.getHeight();
                        Point2D onYaxis = new TranslatedPoint2D(new PointeredPoint2D(0, coordsysAreaLB.getY()), x, Math.round(y));
                        ticks.addSegment(onYaxis, new TranslatedPoint2D(onYaxis, -4, 0)).setColor(tickColor);
                        // label
                        Text yLabel = new Text(yticklabels[i], tickfontSize, style, this.textColor.getAsInt());
                        Dimension yTextSize = yLabel.getTextSize();
                        yLabel.setOrigin(new TranslatedPoint2D(onYaxis, -7 - yTextSize.getWidth(), -Math.round(yTextSize.getHeight() / 2.0) + 0.5));

                        // we need to use a different one here, as tickMarkLabels get cleared when labels have to be shifted
                        tickMarkLabels.add(yLabel);
                    }
                    index++;
                }
            }

            for (Text txt : tickMarkLabels) {
                postContentTextR.addItemToRender(txt);
            }

            if (Objects.nonNull(highlightedFeature)) {
                axisDimensions = preventCoordSysInversion();

                double minValue = 0;
                double maxValue = 0;

                double y = (axisDimensions.getHeight()) /
                                (Math.max(features.get(highlightedFeature.first).top, features.get(highlightedFeature.first).bottom) -
                                        Math.min(features.get(highlightedFeature.first).bottom, features.get(highlightedFeature.first).top));

                if (features.get(highlightedFeature.first).bottom < features.get(highlightedFeature.first).top) {
                    double clipMin = Math.max(highlightedFeature.second.bottom, features.get(highlightedFeature.first).bottom);
                    double clipMax = Math.min(highlightedFeature.second.top, features.get(highlightedFeature.first).top);

                    minValue = y * (clipMin - features.get(highlightedFeature.first).bottom) + coordsysAreaLB.getY();
                    maxValue = y * (clipMax - features.get(highlightedFeature.first).bottom) + coordsysAreaLB.getY();
                } else {
                    double clipMin = Math.min(highlightedFeature.second.bottom, features.get(highlightedFeature.first).bottom);
                    double clipMax = Math.max(highlightedFeature.second.top, features.get(highlightedFeature.first).top);
                    double difference = features.get(highlightedFeature.first).bottom - features.get(highlightedFeature.first).top;

                    minValue = y * (difference-(clipMax - features.get(highlightedFeature.first).top)) + coordsysAreaLB.getY();
                    maxValue = y * (difference-(clipMin - features.get(highlightedFeature.first).top)) + coordsysAreaLB.getY();
                }

                double m = (double) highlightedFeature.first / (features.size() - 1);
                double x = coordsysAreaLB.getX() + m * axisDimensions.getWidth();

                // feature guide
                guides.addSegment(new Point2D.Double(Math.round(x), minValue), new Point2D.Double(Math.round(x), maxValue)).setColor(colorScheme.getColor1()).setThickness(1.1);
                guides.addSegment(new Point2D.Double(Math.round(x)+3, minValue), new Point2D.Double(Math.round(x)-3, minValue)).setColor(colorScheme.getColor1()).setThickness(2);
                guides.addSegment(new Point2D.Double(Math.round(x)+3, maxValue), new Point2D.Double(Math.round(x)-3, maxValue)).setColor(colorScheme.getColor1()).setThickness(2);
            }

            // setup legend areas (this will stay the same)
            if (Objects.nonNull(legendRight)) {
                legendRightViewPort.setBounds(
                        (int) coordsysAreaRT.x[0]+paddingRight+4,
                        paddingBot,
                        legendRightWidth,
                        (int) (coordsysAreaRT.getY() - paddingBot)
                );
            } else {
                legendRightViewPort.setBounds(0, 0, 0, 0);
            }
            if (Objects.nonNull(legendBottom)) {
                legendBottomViewPort.setBounds(
                        (int) coordsysAreaLB.getX(),
                        paddingBot,
                        (int) (coordsysAreaLB.distance(coordsysAreaRB)),
                        legendBottomHeight
                );
            } else {
                legendBottomViewPort.setBounds(0, 0, 0, 0);
            }
        }
    }

    private Rectangle2D preventCoordSysInversion() {
        coordsysAreaLB.y[0] = Math.min(coordsysAreaRT.y[0] + 1, coordsysAreaLB.y[0]);
        coordsysAreaRT.x[0] = Math.max(coordsysAreaLB.x[0] + 1, coordsysAreaRT.x[0]);
        return new Rectangle2D.Double(0, 0, coordsysAreaLB.distance(coordsysAreaRB), coordsysAreaLB.distance(coordsysAreaLT));
    }

    private void clearCoordSysBuildingBlocks() {
        for(Text txt:tickMarkLabels){
            postContentTextR.removeItemToRender(txt);
            txt.close();
        }
        tickMarkLabels.clear();
        guides.removeAllSegments();
        ticks.removeAllSegments();
    }

    private Rectangle2D getMaxBoundsOfAllLabels(String[] labels, double angle, int tickfontSize, int style) {
        double maxRotatedLabelHeight = 0; double maxRotatedLabelWidth = 0;
        for(int i=0; i<labels.length; i++){
            Text label = new Text(labels[i], tickfontSize, style, this.textColor.getAsInt());
            label.setAngle(angle);
            maxRotatedLabelHeight = Math.max(maxRotatedLabelHeight, label.getBoundsWithRotation().getHeight());
            maxRotatedLabelWidth = Math.max(maxRotatedLabelWidth, label.getBoundsWithRotation().getWidth());
        }
        return new Rectangle2D.Double(0, 0, maxRotatedLabelWidth, maxRotatedLabelHeight);
    }

    /**
     * @return the current {@link TickMarkGenerator} which is {@link ExtendedWilkinson} by default.
     */
    public TickMarkGenerator getTickMarkGenerator() {
        return tickMarkGenerator;
    }

    /**
     * Sets the specified {@link TickMarkGenerator} for this {@link ParallelCoordsRenderer}.
     * Sets the {@link #isDirty} state of this {@link ParallelCoordsRenderer} to true.
     * @param tickMarkGenerator to be used for determining tick locations 
     * and corresponding labels
     * @return this for chaining
     */
    public ParallelCoordsRenderer setTickMarkGenerator(TickMarkGenerator tickMarkGenerator) {
        this.tickMarkGenerator = tickMarkGenerator;
        setDirty();
        return this;
    }

    @Override
    public void glInit() {
        preContentLinesR.glInit();
        preContentTextR.glInit();
        postContentLinesR.glInit();
        postContentTextR.glInit();
        if(content != null)
            content.glInit();
        if(legendRight != null)
            legendRight.glInit();
        if(legendBottom != null)
            legendBottom.glInit();
    }

    @Override
    public void render(int vpx, int vpy, int w, int h) {
        if(!isEnabled()){
            return;
        }

        currentViewPort.setRect(vpx, vpy, w, h);
        if(isDirty || viewportwidth != w || viewportheight != h){
            viewportwidth = w;
            viewportheight = h;
            setupAndLayout();
            isDirty = false;
        }
        preContentLinesR.render(vpx, vpy, w, h);
        preContentTextR.render( vpx, vpy, w, h);
        // draw into the coord system
        if(content != null){
            content.glInit();
            int viewPortX = (int)coordsysAreaLB.getX() + vpx;
            int viewPortY = (int)coordsysAreaLB.getY() + vpy;
            int viewPortW = (int)coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int)coordsysAreaLB.distance(coordsysAreaLT);
            GL11.glViewport(viewPortX,viewPortY,viewPortW,viewPortH);
            if(content instanceof AdaptableView){
                ((AdaptableView) content).setView(coordinateView);
            }
            content.render(viewPortX,viewPortY,viewPortW, viewPortH);
            GL11.glViewport(vpx, vpy, w, h);
        }
        postContentLinesR.render(vpx, vpy, w, h);
        postContentTextR.render( vpx, vpy, w, h);

        // draw legends
        if(Objects.nonNull(legendRight)){
            legendRight.glInit();
            GL11.glViewport(   vpx+legendRightViewPort.x, vpy+legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            legendRight.render(vpx+legendRightViewPort.x, vpy+legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            GL11.glViewport(vpx, vpy, w, h);
        }
        if(Objects.nonNull(legendBottom)){
            legendBottom.glInit();
            GL11.glViewport(    vpx+legendBottomViewPort.x, vpy+legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            legendBottom.render(vpx+legendBottomViewPort.x, vpy+legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            GL11.glViewport(vpx, vpy, w, h);
        }

        // draw overlay
        if(Objects.nonNull(overlay)){
            overlay.glInit();
            overlay.render(vpx,vpy,w,h);
        }
    }

    @Override
    public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
        if(!isEnabled()){
            return;
        }
        currentViewPort.setRect(0, 0, w, h);
        if(isDirty || viewportwidth != w || viewportheight != h){
            viewportwidth = w;
            viewportheight = h;
            setupAndLayout();
            isDirty = false;
        }
        preContentLinesR.renderFallback(g, p, w, h);
        preContentTextR.renderFallback(g, p, w, h);
        if(content != null){
            int viewPortX = (int)coordsysAreaLB.getX();
            int viewPortY = (int)coordsysAreaLB.getY();
            int viewPortW = (int)coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int)coordsysAreaLB.distance(coordsysAreaLT);
            if(content instanceof AdaptableView){
                ((AdaptableView) content).setView(coordinateView);
            }
            // create viewport graphics
            Graphics2D g_ = (Graphics2D)g.create(viewPortX, viewPortY, viewPortW, viewPortH);
            Graphics2D p_ = (Graphics2D)p.create(viewPortX, viewPortY, viewPortW, viewPortH);
            content.renderFallback(g_, p_, viewPortW, viewPortH);
        }
        postContentLinesR.renderFallback(g, p, w, h);
        postContentTextR.renderFallback(g, p, w, h);
        // draw legends
        if(Objects.nonNull(legendRight)){
            // create viewport graphics
            Graphics2D g_ = (Graphics2D)g.create(legendRightViewPort.x, legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            Graphics2D p_ = (Graphics2D)p.create(legendRightViewPort.x, legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            legendRight.renderFallback(g_, p_, legendRightViewPort.width, legendRightViewPort.height);
        }
        if(Objects.nonNull(legendBottom)){
            // create viewport graphics
            Graphics2D g_ = (Graphics2D)g.create(legendBottomViewPort.x, legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            Graphics2D p_ = (Graphics2D)p.create(legendBottomViewPort.x, legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            legendBottom.renderFallback(g_, p_, legendBottomViewPort.width, legendBottomViewPort.height);
        }

        // draw overlay
        if(Objects.nonNull(overlay)){
            overlay.renderFallback(g, p, w, h);
        }
    }

    @Override
    public void renderSVG(Document doc, Element parent, int w, int h) {
        if(!isEnabled()){
            return;
        }
        preContentLinesR.renderSVG(doc, parent, w, h);
        preContentTextR.renderSVG(doc, parent, w, h);
        if(content != null){
            int viewPortX = (int)coordsysAreaLB.getX();
            int viewPortY = (int)coordsysAreaLB.getY();
            int viewPortW = (int)coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int)coordsysAreaLB.distance(coordsysAreaLT);
            if(content instanceof AdaptableView){
                ((AdaptableView) content).setView(coordinateView);
            }
            // create a new group for the content
            Element contentGroup = SVGUtils.createSVGElement(doc, "g");
            parent.appendChild(contentGroup);
            // define the clipping rectangle for the content (rect of vieport size)
            Node defs = SVGUtils.getDefs(doc);
            Element clip = SVGUtils.createSVGElement(doc, "clipPath");
            String clipDefID = SVGUtils.newDefId();
            clip.setAttributeNS(null, "id", clipDefID);
            clip.appendChild(SVGUtils.createSVGRect(doc, 0, 0, viewPortW, viewPortH));
            defs.appendChild(clip);
            // transform the group according to the viewport position and clip it
            contentGroup.setAttributeNS(null, "transform", "translate("+(viewPortX)+","+(viewPortY)+")");
            contentGroup.setAttributeNS(null, "clip-path", "url(#"+clipDefID+")");
            // render the content into the group
            content.renderSVG(doc, contentGroup, viewPortW, viewPortH);
        }
        postContentLinesR.renderSVG(doc, parent, w, h);
        postContentTextR.renderSVG(doc, parent, w, h);
        // draw legends
        if(Objects.nonNull(legendRight)){
            // create a new group for the content
            Element legendGroup = SVGUtils.createSVGElement(doc, "g");
            parent.appendChild(legendGroup);
            // define the clipping rectangle for the content (rect of vieport size)
            Node defs = SVGUtils.getDefs(doc);
            Element clip = SVGUtils.createSVGElement(doc, "clipPath");
            String clipDefID = SVGUtils.newDefId();
            clip.setAttributeNS(null, "id", clipDefID);
            clip.appendChild(SVGUtils.createSVGRect(doc, 0, 0, legendRightViewPort.width, legendRightViewPort.height));
            defs.appendChild(clip);
            // transform the group according to the viewport position and clip it
            legendGroup.setAttributeNS(null, "transform", "translate("+(legendRightViewPort.x)+","+(legendRightViewPort.y)+")");
            legendGroup.setAttributeNS(null, "clip-path", "url(#"+clipDefID+")");
            // render the content into the group
            legendRight.renderSVG(doc, legendGroup, legendRightViewPort.width, legendRightViewPort.height);
        }
        if(Objects.nonNull(legendBottom)){
            // create a new group for the content
            Element legendGroup = SVGUtils.createSVGElement(doc, "g");
            parent.appendChild(legendGroup);
            // define the clipping rectangle for the content (rect of vieport size)
            Node defs = SVGUtils.getDefs(doc);
            Element clip = SVGUtils.createSVGElement(doc, "clipPath");
            String clipDefID = SVGUtils.newDefId();
            clip.setAttributeNS(null, "id", clipDefID);
            clip.appendChild(SVGUtils.createSVGRect(doc, 0, 0, legendBottomViewPort.width, legendBottomViewPort.height));
            defs.appendChild(clip);
            // transform the group according to the viewport position and clip it
            legendGroup.setAttributeNS(null, "transform", "translate("+(legendBottomViewPort.x)+","+(legendBottomViewPort.y)+")");
            legendGroup.setAttributeNS(null, "clip-path", "url(#"+clipDefID+")");
            // render the content into the group
            legendBottom.renderSVG(doc, legendGroup, legendBottomViewPort.width, legendBottomViewPort.height);
        }
    }

    @Override
    public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
        if(!isEnabled()){
            return;
        }
        preContentLinesR.renderPDF(doc, page, x, y, w, h);
        preContentTextR.renderPDF(doc, page, x,y, w, h);
        if(content != null){
            int viewPortX = (int)(coordsysAreaLB.getX()+x);
            int viewPortY = (int)(coordsysAreaLB.getY()+y);
            int viewPortW = (int)coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int)coordsysAreaLB.distance(coordsysAreaLT);
            if(content instanceof AdaptableView){
                ((AdaptableView) content).setView(coordinateView);
            }
            // render the content into the group
            content.renderPDF(doc, page, viewPortX, viewPortY, viewPortW, viewPortH);
        }
        postContentLinesR.renderPDF(doc, page, x, y, w, h);
        postContentTextR.renderPDF(doc, page, x, y, w, h);
        if(Objects.nonNull(legendRight)){
            legendRight.renderPDF(doc, page, legendRightViewPort.x, legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
        }
        if(Objects.nonNull(legendBottom)){
            legendBottom.renderPDF(doc, page, legendBottomViewPort.x, legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
        }
    }

    @Override
    public void close() {
        if(Objects.nonNull(preContentTextR))
            preContentTextR.close();
        if(Objects.nonNull(preContentLinesR))
            preContentLinesR.close();
        if(Objects.nonNull(preContentTextR))
            postContentTextR.close();
        if(Objects.nonNull(preContentLinesR))
            postContentLinesR.close();
        if(Objects.nonNull(content))
            content.close();
        if(Objects.nonNull(legendRight))
            legendRight.close();
        if(Objects.nonNull(legendBottom))
            legendBottom.close();
        if(Objects.nonNull(overlay))
            overlay.close();
    }

    /**
     * @return the area of this renderer in which the coordinate system contents are rendered.
     * It is the viewPort for the {@link #content} renderer which is enclosed by
     * the coordinate system axes.
     */
    @Annotations.GLCoordinates
    public Rectangle2D getCoordSysArea() {
        return new Rectangle2D.Double(
                coordsysAreaLB.getX()+currentViewPort.x,
                coordsysAreaLB.getY()+currentViewPort.y,
                coordsysAreaLB.distance(coordsysAreaRB),
                coordsysAreaLB.distance(coordsysAreaLT)
        );
    }

    /**
     * @return the viewport which this CoordSysRenderer was last rendered into
     */
    @Annotations.GLCoordinates
    public Rectangle getCurrentViewPort() {
        return currentViewPort;
    }

    /**
     * Transforms a location in AWT coordinates (y axis extends to bottom)
     * on this renderer to the corresponding coordinates in the coordinate
     * system view (in GL coords).
     * @param awtPoint to be transformed
     * @param canvasheight height of the canvas this {@link CoordSysRenderer} is drawn to
     * @return transformed location
     */
    public Point2D transformAWT2CoordSys(Point2D awtPoint, int canvasheight){
        Point2D glp = Utils.swapYAxis(awtPoint, canvasheight);
        return transformGL2CoordSys(glp);
    }

    /**
     * Transforms a location in GL coordinates on this renderer to the
     * corresponding coordinates in the coordinate system view.
     * @param point to be transformed
     * @return transformed location
     */
    @Annotations.GLCoordinates
    public Point2D transformGL2CoordSys(Point2D point){
        Rectangle2D coordSysArea = getCoordSysArea();
        Rectangle2D coordinateView = new Rectangle2D.Double(0, 0, 1, 1);
        double x = point.getX()-coordSysArea.getMinX();
        double y = point.getY()-coordSysArea.getMinY();
        x /= coordSysArea.getWidth()-1;
        y /= coordSysArea.getHeight()-1;
        x = x*coordinateView.getWidth()+coordinateView.getMinX();
        y = y*coordinateView.getHeight()+coordinateView.getMinY();
        return new Point2D.Double(x, y);
    }

    /**
     * Transforms a location in coordinates of the current coordinate view
     * to corresponding coordinates of this renderer (in GL coords).
     * @param point to be transformed
     * @return transformed location
     */
    @Annotations.GLCoordinates
    public Point2D transformCoordSys2GL(Point2D point){
        Rectangle2D coordSysArea = getCoordSysArea();
        Rectangle2D coordSysView = new Rectangle2D.Double(0, 0, 1, 1);
        double x = point.getX()-coordSysView.getMinX();
        double y = point.getY()-coordSysView.getMinY();
        x /= coordSysView.getWidth();
        y /= coordSysView.getHeight();
        x = x*(coordSysArea.getWidth()-1)+coordSysArea.getMinX();
        y = y*(coordSysArea.getHeight()-1)+coordSysArea.getMinY();
        return new Point2D.Double(x, y);
    }

    /**
     * Transforms a location in coordinates of the current coordinate view
     * to corresponding coordinates of this renderer's canvas in AWT coordinates
     * (where y axis extends to bottom).
     * @param point to be transformed
     * @param canvasheight height of the canvas this {@link CoordSysRenderer} is drawn to
     * @return transformed location
     */
    public Point2D transformCoordSys2AWT(Point2D point, int canvasheight){
        Point2D glPoint = transformCoordSys2GL(point);
        return Utils.swapYAxis(glPoint, canvasheight);
    }

    @Override
    public void setEnabled(boolean enable) {
        this.isEnabled = enable;
    }
    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    /**
     * A feature represents an axis in the ParallelCoords renderer, where an index of a datachunk will be mapped to.
     * It contains a descriptor label, which will be displayed below the axis
     * and top/bottom labels which indicate the feature boundaries.
     */
    public static class Feature {
        public final double bottom;
        public final double top;
        public final String label;

        /**
         * Creates a feature with the given parameters.
         *
         * @param bottom boundary of the feature
         * @param top boundary of the feature
         * @param label which describes the feature
         */
        public Feature(double bottom, double top, String label) {
            this.bottom = bottom;
            this.top = top;
            this.label = label;
        }
    }
}
