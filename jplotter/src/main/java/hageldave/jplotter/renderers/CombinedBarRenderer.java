package hageldave.jplotter.renderers;

import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.TickMarkGenerator;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.interaction.CoordinateViewListener;
import hageldave.jplotter.renderables.*;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.util.List;
import java.util.*;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

public class CombinedBarRenderer implements Renderer {

    protected int alignment;

    protected LinesRenderer preContentLinesR = new LinesRenderer();
    protected TextRenderer preContentTextR = new TextRenderer();
    protected LinesRenderer xyCondBoundsLinesR = new LinesRenderer();
    protected TextRenderer xyCondBoundsTextR = new TextRenderer();
    protected LinesRenderer postContentLinesR = new LinesRenderer();
    protected TextRenderer postContentTextR = new TextRenderer();

    protected Renderer overlay;
    protected TrianglesRenderer content = null;
    protected Renderer legendRight = null;
    protected Renderer legendBottom = null;

    protected int legendRightWidth = 70;
    protected int legendBottomHeight = 20;

    @Annotations.GLCoordinates
    protected Rectangle legendRightViewPort = new Rectangle();
    @Annotations.GLCoordinates
    protected Rectangle legendBottomViewPort = new Rectangle();

    @Annotations.GLCoordinates
    protected Rectangle currentViewPort = new Rectangle();

    protected Rectangle2D coordinateView = new Rectangle2D.Double(-1, -1, 2, 2);

    protected TickMarkGenerator tickMarkGenerator = new ExtendedWilkinson();

    protected Lines ticks = new Lines().setVertexRoundingEnabled(true);
    protected Lines guides = new Lines().setVertexRoundingEnabled(true);
    protected LinkedList<Text> tickMarkLabels = new LinkedList<>();

    // used for guides, ticks and labels which will not be rendered if they're outside of the coordsys
    protected Lines xyCondTicks = new Lines().setVertexRoundingEnabled(true);
    protected Lines xyCondGuides = new Lines().setVertexRoundingEnabled(true);
    protected LinkedList<Text> xyCondTickMarkLabels = new LinkedList<>();

    protected Text xAxisLabelText = new Text("", 13, Font.PLAIN);
    protected Text yAxisLabelText = new Text("", 13, Font.PLAIN);

    protected double[] xticks;
    protected double[] yticks;
    protected List<String> groupDescriptions = new LinkedList<>();

    protected int viewportwidth = 0;
    protected int viewportheight = 0;
    protected boolean isDirty = true;

    protected IntSupplier textColor;
    protected IntSupplier tickColor;
    protected IntSupplier guideColor;
    protected IntSupplier groupGuideColor;
    protected IntSupplier boundaryColor;

    final int tickfontSize = 11;
    final int labelfontSize = 12;
    final int style = Font.PLAIN;

    protected int paddingLeft = 10;
    protected int paddingRight = 10;
    protected int paddingTop = 10;
    protected int paddingBot = 10;

    // Linkedlist for now - later might be replaced by treeset
    final protected LinkedList<BarGroup> groupedBars = new LinkedList<>();
    protected double barSize;

    protected ColorScheme colorScheme;

    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaLB = new PointeredPoint2D();
    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaRT = Utils.copy(coordsysAreaLB);
    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaLT = new PointeredPoint2D(coordsysAreaLB.x, coordsysAreaRT.y);
    @Annotations.GLCoordinates
    protected PointeredPoint2D coordsysAreaRB = new PointeredPoint2D(coordsysAreaRT.x, coordsysAreaLB.y);

    protected String xAxisLabel = null;
    protected String yAxisLabel = null;

    protected ActionListener coordviewListener;
    protected boolean isEnabled = true;

    public CombinedBarRenderer(final int alignment, final double barSize, final ColorScheme cs) {
        this.alignment = alignment;
        this.barSize = barSize;
        this.colorScheme = cs;
        setupBarRenderer();
    }

    public CombinedBarRenderer(final int alignment, final ColorScheme cs) {
        this(alignment, 0.8, cs);
    }

    public CombinedBarRenderer(final int alignment) {
        this(alignment, 0.8, DefaultColorScheme.LIGHT.get());
    }

    protected void setupBarRenderer() {
        this.preContentLinesR
                .addItemToRender(ticks)
                .addItemToRender(guides);
        this.preContentTextR
                .addItemToRender(xAxisLabelText)
                .addItemToRender(yAxisLabelText);
        this.xyCondBoundsLinesR
                .addItemToRender(xyCondTicks)
                .addItemToRender(xyCondGuides);

        this.textColor = ()->getColorScheme().getColorText();
        this.tickColor = ()->getColorScheme().getColor3();
        this.guideColor = ()->getColorScheme().getColor4();
        this.groupGuideColor = ()->getColorScheme().getColor4();
        this.boundaryColor = ()->getColorScheme().getColor3();
        updateColors();
    }

    /**
     * Helper method to update the colors if the color scheme is changed.
     */
    protected CombinedBarRenderer updateColors() {
        this.xAxisLabelText.setColor(this.textColor.getAsInt());
        this.yAxisLabelText.setColor(this.textColor.getAsInt());
        updateLegendColorScheme(legendBottom);
        updateLegendColorScheme(legendRight);
        setDirty();
        return this;
    }

    /**
     * Sets the {@link #isDirty} state of this CoordSysRenderer to true.
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
     *
     * @param content the content renderer
     * @return the previous content renderer (which may need to be closed to free GL resources),
     * null if none was set
     */
    public Renderer setContent(TrianglesRenderer content) {
        Renderer old = this.content;
        this.content = content;
        return old;
    }

    /**
     * Sets the renderer that will draw the legend to the right of the coordinate system.
     * The view port area for this renderer will start at the top edge of the coordinate system,
     * be {@link #getLegendRightWidth()} wide and extend to the bottom (-padding).
     *
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
     *
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
     *
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
     * before any visible elements of the CoordSysRenderer are drawn
     *
     * @param padding amount of blank area
     * @return this for chaining
     */
    public CombinedBarRenderer setPaddingLeft(int padding) {
        this.paddingLeft = padding;
        return this;
    }

    /**
     * Sets the padding to the right side, which is the size of the blank area
     * before any visible elements of the CoordSysRenderer are drawn
     *
     * @param padding amount of blank area
     * @return this for chaining
     */
    public CombinedBarRenderer setPaddingRight(int padding) {
        this.paddingRight = padding;
        return this;
    }

    /**
     * Sets the padding to the top side, which is the size of the blank area
     * before any visible elements of the CoordSysRenderer are drawn
     *
     * @param padding amount of blank area
     * @return this for chaining
     */
    public CombinedBarRenderer setPaddingTop(int padding) {
        this.paddingTop = padding;
        return this;
    }

    /**
     * Sets the padding to the bottom side, which is the size of the blank area
     * before any visible elements of the CoordSysRenderer are drawn
     *
     * @param padding amount of blank area
     * @return this for chaining
     */
    public CombinedBarRenderer setPaddingBot(int padding) {
        this.paddingBot = padding;
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
     *
     * @param legendBottomHeight height of the bottom legend area.
     *                           (default is 20px)
     * @return this for chaining
     */
    public CombinedBarRenderer setLegendBottomHeight(int legendBottomHeight) {
        this.legendBottomHeight = legendBottomHeight;
        return this;
    }

    /**
     * Sets the width of the legend area right to the coordinate system.
     * (height is determined by the space available until the bottom of the renderer's viewport)
     *
     * @param legendRightWidth width of the right legend area.
     *                         (default is 70 px)
     * @return this for chaining
     */
    public CombinedBarRenderer setLegendRightWidth(int legendRightWidth) {
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
     * @return the {@link ColorScheme} of the BarRenderer.
     */
    public ColorScheme getColorScheme() {
        return colorScheme;
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
        if (this.alignment == AlignmentConstants.VERTICAL)
            setupLayoutVertical();
        else if (this.alignment == AlignmentConstants.HORIZONTAL)
            setupLayoutHorizontal();
    }

    /**
     *
     */
    protected void setupLayoutVertical() {
        Pair<double[], String[]> yticksAndLabels = tickMarkGenerator.genTicksAndLabels(
                coordinateView.getMinY(),
                coordinateView.getMaxY(),
                5,
                true);

        // count how many bars will be rendered
        int barcount = groupedBars.stream().mapToInt(e -> e.getGroupedBars().size()).sum();
        int groupcount = groupedBars.size();
        String[] yticklabels = new String[barcount];
        String[] xticklabels = new String[barcount];
        this.xticks = new double[barcount];
        this.yticks = new double[barcount];
        double[] groupSeparators = new double[groupcount + 1];
        groupDescriptions = groupedBars.stream().map(BarGroup::getLabel).collect(Collectors.toList());

        // fill arrays with empty strings
        Arrays.fill(xticklabels, "");
        Arrays.fill(yticklabels, "");

        // fill non description arrays with the numbers provided by the tickMarkgenerator
        this.yticks = yticksAndLabels.first;
        yticklabels = yticksAndLabels.second;

        ticks.removeAllSegments();
        guides.removeAllSegments();

        xyCondTicks.removeAllSegments();
        xyCondGuides.removeAllSegments();

        setupBars(groupSeparators, xticklabels, xticks);
        // find maximum length of y axis labels
        int maxTickLabelWidth = calcMaxTickWidth(0, yticklabels, xticklabels);
        int maxXTickLabelHeight = CharacterAtlas.boundsForText(1, tickfontSize, style).getBounds().height;
        int maxLabelHeight = CharacterAtlas.boundsForText(1, labelfontSize, style).getBounds().height;
        int legendRightW = Objects.nonNull(legendRight) ? legendRightWidth + 4 : 0;
        int legendBotH = Objects.nonNull(legendBottom) ? legendBottomHeight + 4 : 0;

        // move coordwindow origin so that labels have enough display space
        coordsysAreaLB.x[0] = maxXTickLabelHeight + paddingLeft + 15;
        coordsysAreaLB.y[0] = maxTickLabelWidth + paddingBot + legendBotH + 10;
        // check if there are any labels and add their height to padding
        if (groupDescriptions.stream().anyMatch(Objects::nonNull))
            coordsysAreaLB.y[0] = maxTickLabelWidth + paddingLeft + 14 + maxLabelHeight + legendBotH;
        // move opposing corner of coordwindow to have enough display space
        coordsysAreaRT.x[0] = viewportwidth - paddingRight - maxLabelHeight - legendRightW - 4;
        coordsysAreaRT.y[0] = viewportheight - paddingTop - maxLabelHeight - 4;

        // dispose of old stuff (moved to top)
        clearLabelRenderer();

        // create new stuff
        double xAxisWidth = coordsysAreaLB.distance(coordsysAreaRB);
        double yAxisHeight = coordsysAreaLB.distance(coordsysAreaLT);
        boolean shiftLabels = false;
        HashMap<Text, Point2D> toShift = new HashMap<>();

        // xaxis ticks
        for (int i = 0; i < xticks.length; i++) {
            // barticks
            double xLeft =
                    ((xticks[i] - (barSize / 2) - coordinateView.getMinX()) / coordinateView.getWidth()) * xAxisWidth;
            double xRight =
                    ((xticks[i < xticks.length - 1 ? i + 1 : i] + (barSize / 2) - coordinateView.getMinX()) / coordinateView.getWidth()) * xAxisWidth;

            // create double ticks
            Point2D barBorder = new Point2D.Double(Math.round((xLeft + xRight) / 2), coordsysAreaLB.getY());

            if (i < xticks.length - 1) {
                xyCondTicks.addSegment(barBorder, new TranslatedPoint2D(barBorder, 0, -6))
                        .setColor(boundaryColor);
            }

            double x = ((xticks[i] - coordinateView.getMinX()) / coordinateView.getWidth()) * xAxisWidth;
            Text label = new Text(xticklabels[i], tickfontSize, style, this.textColor.getAsInt());
            Text invisibleLabel = new Text(xticklabels[i] + "0", tickfontSize, style);
            if (invisibleLabel.getTextSize().width >
                    // + 0.2 bc size between every bar is 1 and barwidth is 0.8
                    ((barSize + 0.2) / coordinateView.getWidth()) * xAxisWidth) {
                // enable shifting
                shiftLabels = true;
            }
            Point2D onaxis = new Point2D.Double(Math.round(x), coordsysAreaLB.getY());
            Point2D labelOrigin = new Point2D.Double((int) (onaxis.getX() - label.getTextSize().getWidth() / 2.0),
                    (int) ( onaxis.getY() - 6 - label.getTextSize().getHeight() ) + 0.5);
            label.setOrigin(labelOrigin);
            toShift.put(label, labelOrigin);
            xyCondTickMarkLabels.add(label);
        }

        // yaxis ticks
        for (int i = 0; i < yticks.length; i++) {
            // tick
            double y = (( yticks[i] - coordinateView.getMinY() ) / coordinateView.getHeight()) * yAxisHeight;
            Point2D onaxis = new TranslatedPoint2D(coordsysAreaLB, 0, Math.round(y));
            ticks.addSegment(onaxis, new TranslatedPoint2D(onaxis, -4, 0)).setColor(tickColor);
            // label
            Text label = new Text(yticklabels[i], tickfontSize, style, this.textColor.getAsInt());
            Dimension textSize = label.getTextSize();
            label.setOrigin(new TranslatedPoint2D(onaxis, -7 - textSize.getWidth(), -Math.round(textSize.getHeight() / 2.0) + 0.5));
            // avoid conflict with coordsys axes in bottom left corner, as they are bigger
            if (y < textSize.height)
                label.setOrigin(new TranslatedPoint2D(onaxis, -17 - textSize.getWidth(), -Math.round(textSize.getHeight() / 2.0) + 0.5));

            tickMarkLabels.add(label);
            // guide
            guides.addSegment(onaxis, new TranslatedPoint2D(coordsysAreaRB, 0, y)).setColor(guideColor);
        }

        if (shiftLabels) {
            for (Map.Entry<Text, Point2D> entry : toShift.entrySet()) {
                entry.getKey().setAngle(0.5 * Math.PI);
                entry.getKey().setOrigin(new Point2D.Double(entry.getValue().getX() + entry.getKey().getTextSize().getWidth() / 2 + 5,
                        entry.getValue().getY() - entry.getKey().getTextSize().getWidth() + 7));
            }
        }

        for (int i = 0; i < groupSeparators.length; i++) {
            double x = (( groupSeparators[i] - coordinateView.getMinX() ) / coordinateView.getWidth()) * xAxisWidth;
            Point2D barBorder = new Point2D.Double(Math.round(x), coordsysAreaLB.getY());
            xyCondGuides.addSegment(barBorder, new TranslatedPoint2D(barBorder, 0, yAxisHeight)).setColor(groupGuideColor);
            xyCondGuides.addSegment(barBorder, new TranslatedPoint2D(barBorder, 0, -12)).setColor(boundaryColor);
            double xNext = (( groupSeparators[i == groupSeparators.length - 1 ? i : i + 1] - coordinateView.getMinX() )
                    / coordinateView.getWidth()) * xAxisWidth;
            barBorder = new Point2D.Double(Math.round((x + xNext)/2), coordsysAreaLB.getY() - 12);

            // add group labels
            if (i < groupSeparators.length - 1 && this.groupDescriptions.get(i) != null) {
                Text groupLabel = new Text(this.groupDescriptions.get(i), tickfontSize, 1, this.textColor.getAsInt());
                groupLabel.setOrigin(new TranslatedPoint2D(barBorder, -groupLabel.getTextSize().getWidth() / 2,
                        -Math.round(groupLabel.getTextSize().getHeight() * 2) - 2.5));
                // if shifting is enabled
                if (shiftLabels) {
                    groupLabel.setOrigin(new TranslatedPoint2D(barBorder, -groupLabel.getTextSize().getWidth() / 2,
                            -Math.round(groupLabel.getTextSize().getHeight() * 2) + 6 - maxTickLabelWidth));
                }
                xyCondTickMarkLabels.add(groupLabel);
            }
        }

        setupBoundaries(guides, xAxisWidth, yAxisHeight);
        for (Text txt : tickMarkLabels)
            preContentTextR.addItemToRender(txt);
        for (Text txt : xyCondTickMarkLabels)
            xyCondBoundsTextR.addItemToRender(txt);
        setupAxisLabels(xAxisWidth, yAxisHeight);

        // setup legend areas
        if (Objects.nonNull(legendRight)) {
            legendRightViewPort.setBounds(
                    (int) ( yAxisLabelText.getOrigin().getX() + yAxisLabelText.getTextSize().getHeight() + 4),
                    paddingBot,
                    (legendRightWidth - paddingRight),
                    (int) ( coordsysAreaRT.getY() - paddingBot )
            );
        } else {
            legendRightViewPort.setBounds(0, 0, 0, 0);
        }
        if (Objects.nonNull(legendBottom)) {
            legendBottomViewPort.setBounds(
                    (int) coordsysAreaLB.getX(),
                    0,
                    (int) ( coordsysAreaLB.distance(coordsysAreaRB) ),
                    legendBottomHeight
            );
        } else {
            legendBottomViewPort.setBounds(0, 0, 0, 0);
        }
    }

    /**
     *
     */
    protected void setupLayoutHorizontal() {
        // place new label positioning somewhere here
        Pair<double[],String[]> xticksAndLabels = tickMarkGenerator.genTicksAndLabels(
                coordinateView.getMinX(),
                coordinateView.getMaxX(),
                5,
                false);

        // count how many bars will be rendered
        int barcount = groupedBars.parallelStream().mapToInt(e -> e.getGroupedBars().size()).sum();
        int groupcount = groupedBars.size();
        String[] yticklabels = new String[barcount];
        String[] xticklabels = new String[barcount];
        this.xticks = new double[barcount];
        this.yticks = new double[barcount];
        double[] groupSeparators = new double[groupcount+1];

        groupDescriptions = groupedBars.stream().map(BarGroup::getLabel).collect(Collectors.toList());

        // fill arrays with empty strings
        Arrays.fill(xticklabels, "");
        Arrays.fill(yticklabels, "");
        this.xticks = xticksAndLabels.first;
        xticklabels = xticksAndLabels.second;

        ticks.removeAllSegments();
        guides.removeAllSegments();

        xyCondTicks.removeAllSegments();
        xyCondGuides.removeAllSegments();

        setupBars(groupSeparators, yticklabels, yticks);

        // find maximum length of y axis labels
        int maxTickLabelWidth = calcMaxTickWidth(0, yticklabels, xticklabels);
        int maxXTickLabelHeight = CharacterAtlas.boundsForText(1, tickfontSize, style).getBounds().height;
        int maxLabelHeight = CharacterAtlas.boundsForText(1, labelfontSize, style).getBounds().height;
        int legendRightW = Objects.nonNull(legendRight) ? legendRightWidth+4:0;
        int legendBotH = Objects.nonNull(legendBottom) ? legendBottomHeight+4:0;

        // move coordwindow origin so that labels have enough display space
        coordsysAreaLB.x[0] = maxTickLabelWidth + paddingLeft + 10;
        coordsysAreaLB.y[0] = maxXTickLabelHeight + paddingBot + legendBotH + 15;
        // check if there are any labels and add their height to padding
        if (groupDescriptions.stream().anyMatch(Objects::nonNull)) {
            coordsysAreaLB.x[0] = maxTickLabelWidth + paddingLeft + 14 + maxLabelHeight;
        }

        // move opposing corner of coordwindow to have enough display space
        coordsysAreaRT.x[0] = viewportwidth-paddingRight-maxLabelHeight-legendRightW-4;
        coordsysAreaRT.y[0] = viewportheight-paddingTop-maxLabelHeight-4;

        // dispose of old stuff (moved to top)
        clearLabelRenderer();

        // create new stuff
        double xAxisWidth = coordsysAreaLB.distance(coordsysAreaRB);
        double yAxisHeight = coordsysAreaLB.distance(coordsysAreaLT);
        // xaxis ticks
        for(int i=0; i<xticks.length; i++){
            // tick
            double m = (xticks[i]-coordinateView.getMinX())/coordinateView.getWidth();
            double x = coordsysAreaLB.getX()+m*xAxisWidth;
            Point2D onaxis = new Point2D.Double(Math.round(x),coordsysAreaLB.getY());
            ticks.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0,-4)).setColor(tickColor);
            // label
            Text label = new Text(xticklabels[i], tickfontSize, style, this.textColor.getAsInt());
            Dimension textSize = label.getTextSize();
            label.setOrigin(new Point2D.Double(
                    (int)(onaxis.getX()-textSize.getWidth()/2.0),
                    (int)(onaxis.getY()-6-textSize.getHeight())+0.5));

            if (m*xAxisWidth < textSize.width) {
                label.setOrigin(new Point2D.Double(
                        (int) ( onaxis.getX() - textSize.getWidth() / 2.0 ),
                        (int) ( onaxis.getY() - 6 - textSize.getHeight() ) - 8.5));
            }
            tickMarkLabels.add(label);
            guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, yAxisHeight)).setColor(guideColor);
        }

        // yaxis ticks
        for(int i=0; i<yticks.length; i++){
            // tick
            double y = ((yticks[i]-coordinateView.getMinY())/coordinateView.getHeight()) * yAxisHeight;
            // barticks
            double yBottom = ((yticks[i < yticks.length-1 ? i+1 : i]-(barSize/2)-coordinateView.getMinY())
                    /coordinateView.getHeight())*yAxisHeight;
            double yTop = ((yticks[i]+(barSize/2)-coordinateView.getMinY())/coordinateView.getHeight()) * yAxisHeight;

            Point2D onaxis = new TranslatedPoint2D(new Point2D.Double(coordsysAreaLB.getX(), 0), 0, Math.round(y));
            // create double ticks
            Point2D barBorder = new TranslatedPoint2D(new Point2D.Double(coordsysAreaLB.getX(), 0), 0, Math.round((yTop + yBottom)/2));
            if (i < yticks.length-1) {
                xyCondTicks.addSegment(barBorder, new TranslatedPoint2D(barBorder, -6, 0)).setColor(boundaryColor);
            }
            // label
            Text label = new Text(yticklabels[i], tickfontSize, style, this.textColor.getAsInt());
            Dimension textSize = label.getTextSize();
            label.setOrigin(new TranslatedPoint2D(onaxis, -7-textSize.getWidth(), -Math.round(textSize.getHeight()/2.0)+0.5));
            xyCondTickMarkLabels.add(label);
        }

        for (int i=0; i<groupSeparators.length; i++) {
            double y = ((groupSeparators[i]-coordinateView.getMinY())/coordinateView.getHeight())*yAxisHeight;
            Point2D barBorder = new TranslatedPoint2D(new Point2D.Double(coordsysAreaLB.getX(), 0), 0, Math.round(y));
            xyCondGuides.addSegment(barBorder, new TranslatedPoint2D(barBorder, xAxisWidth, 0)).setColor(groupGuideColor);
            xyCondGuides.addSegment(barBorder, new TranslatedPoint2D(barBorder, -12, 0)).setColor(boundaryColor);
            double yNext = ((groupSeparators[i == groupSeparators.length - 1 ? i : i + 1] - coordinateView.getMinY() )
                    / coordinateView.getHeight())*yAxisHeight;
            barBorder = new Point2D.Double(20, Math.round((y + yNext)/2));

            // add group labels
            if (i < groupSeparators.length - 1 && this.groupDescriptions.get(i) != null) {
                Text groupLabel = new Text(this.groupDescriptions.get(i), tickfontSize, 1, this.textColor.getAsInt());
                groupLabel.setAngle(0.5 * Math.PI)
                        .setOrigin(new TranslatedPoint2D(barBorder, 0, -groupLabel.getTextSize().getWidth()/2));
                xyCondTickMarkLabels.add(groupLabel);
            }
        }

        setupBoundaries(guides, xAxisWidth, yAxisHeight);
        for(Text txt: tickMarkLabels)
            preContentTextR.addItemToRender(txt);
        for (Text txt : xyCondTickMarkLabels)
            xyCondBoundsTextR.addItemToRender(txt);
        setupAxisLabels(xAxisWidth, yAxisHeight);

        // setup legend areas
        if(Objects.nonNull(legendRight)){
            legendRightViewPort.setBounds(
                    (int)(yAxisLabelText.getOrigin().getX()+yAxisLabelText.getTextSize().getHeight()+4),
                    paddingBot,
                    legendRightWidth-paddingRight,
                    (int)(coordsysAreaRT.getY()-paddingBot)
            );
        } else {
            legendRightViewPort.setBounds(0, 0, 0, 0);
        }
        if(Objects.nonNull(legendBottom)){
            legendBottomViewPort.setBounds(
                    (int)coordsysAreaLB.getX(),
                    paddingBot,
                    (int)(coordsysAreaLB.distance(coordsysAreaRB)),
                    legendBottomHeight
            );
        } else {
            legendBottomViewPort.setBounds(0, 0, 0, 0);
        }
    }

    protected void setupBars(final double[] groupSeparators, String[] xticklabels, double[] xticks) {
        // where to position each barstruct
        double structPos = 0;
        // index for label array
        int index = 0;
        int groupindex = 0;
        // clear everything to avoid rendering stuff multiple times
        this.content.itemsToRender.clear();
        for (BarGroup group : this.groupedBars) {
            // add guide here
            groupSeparators[groupindex++] = structPos - 0.75;
            for (BarGroup.BarStruct struct : group.getSortedBars()) {
                int stackStart = 0; int stackEnd = 0;
                // add description labels on y axis
                // (or) add description labels on x axis (when vertical alignment instead of horizontal)
                xticks[index] = structPos;
                xticklabels[index] = struct.description;
                // adds each stack to the triangle renderer
                for (BarGroup.Stack stack : struct.stacks) {
                    if (stack.length >= 0) {
                        this.content.addItemToRender((makeBar(stackEnd, structPos, stack.length, stack.stackColor, stack.pickColor)));
                        stackEnd += stack.length;
                    } else {
                        this.content.addItemToRender((makeBar(stackEnd, structPos, stack.length, stack.stackColor, stack.pickColor)));
                        stackStart += stack.length;
                    }
                }
                // increment pos for every struct
                structPos++;
                index++;
            }
            // increment for every group (but by smaller increment)
            structPos += 0.5;
        }
        groupSeparators[groupindex] = structPos - 0.75;
    }

    protected void clearLabelRenderer() {
        for (Text txt : tickMarkLabels) {
            preContentTextR.removeItemToRender(txt);
            txt.close();
        }
        tickMarkLabels.clear();
        for (Text txt : xyCondTickMarkLabels) {
            xyCondBoundsTextR.removeItemToRender(txt);
            txt.close();
        }
        xyCondTickMarkLabels.clear();
    }

    protected int calcMaxTickWidth(int maxTickLabelWidth, String[] yticklabels, String[] xticklabels) {
        for(String label:yticklabels){
            int labelW = CharacterAtlas.boundsForText(label.length(), tickfontSize, style).getBounds().width;
            maxTickLabelWidth = Math.max(maxTickLabelWidth, labelW);
        }
        for(String label:xticklabels){
            int labelW = CharacterAtlas.boundsForText(label.length(), tickfontSize, style).getBounds().width;
            maxTickLabelWidth = Math.max(maxTickLabelWidth, labelW);
        }
        return maxTickLabelWidth;
    }

    protected void setupAxisLabels(final double xAxisWidth, final double yAxisHeight) {
        // axis labels
        xAxisLabelText.setTextString(getxAxisLabel());
        xAxisLabelText.setOrigin(new TranslatedPoint2D(coordsysAreaLT, xAxisWidth / 2 - xAxisLabelText.getTextSize().width / 2, 4));
        yAxisLabelText.setTextString(getyAxisLabel());
        yAxisLabelText.setAngle(-(float) Math.PI / 2);
        yAxisLabelText.setOrigin(new TranslatedPoint2D(coordsysAreaRB, 4, yAxisHeight / 2 + yAxisLabelText.getTextSize().width / 2));
    }

    // helper method to add boundaries
    protected void setupBoundaries(final Lines guides, final double xAxisWidth, final double yAxisHeight) {
        // x axis
        guides.addSegment(new Point2D.Double(coordsysAreaLB.getX() - 14, coordsysAreaLB.getY()),
                new TranslatedPoint2D(coordsysAreaLB, xAxisWidth, 0)).setColor(boundaryColor);
        //y axis
        guides.addSegment(new Point2D.Double(coordsysAreaLB.getX(), coordsysAreaLB.getY() - 14),
                new TranslatedPoint2D(coordsysAreaLB, 0, yAxisHeight)).setColor(boundaryColor);
    }

    // creates quad (to be rendered) at pos start, in row row, with value and color
    protected Triangles makeBar(final double start, final double row, final double val, final Color color, final int pickColor) {
        Triangles bar = new Triangles();
        if (this.alignment == AlignmentConstants.HORIZONTAL) {
            bar.addQuad(new Rectangle2D.Double(start, row - ( barSize / 2 ), val, barSize));
        } else if (this.alignment == AlignmentConstants.VERTICAL) {
            bar.addQuad(new Rectangle2D.Double(row - ( barSize / 2 ), start, barSize, val));
        }
        bar.getTriangleDetails().forEach(tri -> tri.setColor(color).setPickColor(pickColor));
        return bar;
    }

    /**
     * @return "X" if {@link #xAxisLabel} is null or the actual axis label.
     */
    public String getxAxisLabel() {
        return xAxisLabel == null ? "X" : xAxisLabel;
    }

    /**
     * @return "Y" if {@link #yAxisLabel} is null or the actual axis label.
     */
    public String getyAxisLabel() {
        return yAxisLabel == null ? "Y" : yAxisLabel;
    }

    /**
     * Sets the specified string as the x axis label which appears on top of the coordinate system.
     * Sets the {@link #isDirty} state of this {@link CoordSysRenderer} to true.
     *
     * @param xAxisLabel to set
     * @return this for chaining
     */
    public CombinedBarRenderer setxAxisLabel(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
        setDirty();
        return this;
    }

    /**
     * Sets the specified string as the y axis label which appears to the right of the coordinate system.
     * Sets the {@link #isDirty} state of this {@link CoordSysRenderer} to true.
     *
     * @param yAxisLabel to set
     * @return this for chaining
     */
    public CombinedBarRenderer setyAxisLabel(String yAxisLabel) {
        this.yAxisLabel = yAxisLabel;
        setDirty();
        return this;
    }

    /**
     * @return the current {@link TickMarkGenerator} which is {@link ExtendedWilkinson} by default.
     */
    public TickMarkGenerator getTickMarkGenerator() {
        return tickMarkGenerator;
    }

    /**
     * Sets the specified {@link TickMarkGenerator} for this {@link CoordSysRenderer}.
     * Sets the {@link #isDirty} state of this {@link CoordSysRenderer} to true.
     *
     * @param tickMarkGenerator to be used for determining tick locations
     *                          and corresponding labels
     * @return this for chaining
     */
    public CombinedBarRenderer setTickMarkGenerator(TickMarkGenerator tickMarkGenerator) {
        this.tickMarkGenerator = tickMarkGenerator;
        setDirty();
        return this;
    }

    @Override
    public void glInit() {
        preContentLinesR.glInit();
        preContentTextR.glInit();
        xyCondBoundsLinesR.glInit();
        xyCondBoundsTextR.glInit();
        postContentLinesR.glInit();
        postContentTextR.glInit();
        if (content != null)
            content.glInit();
        if (legendRight != null)
            legendRight.glInit();
        if (legendBottom != null)
            legendBottom.glInit();
    }

    @Override
    public void render(int vpx, int vpy, int w, int h) {
        if (!isEnabled()) {
            return;
        }

        currentViewPort.setRect(vpx, vpy, w, h);
        if (isDirty || viewportwidth != w || viewportheight != h) {
            // update axes
            viewportwidth = w;
            viewportheight = h;
            setupAndLayout();
            isDirty = false;
        }
        preContentLinesR.render(vpx, vpy, w, h);
        preContentTextR.render(vpx, vpy, w, h);

        // draw into the coord system
        if (content != null) {
            content.glInit();
            int viewPortX = (int) coordsysAreaLB.getX() + vpx;
            int viewPortY = (int) coordsysAreaLB.getY() + vpy;
            int viewPortW = (int) coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int) coordsysAreaLB.distance(coordsysAreaLT);
            GL11.glViewport(viewPortX, viewPortY, viewPortW, viewPortH);
            if (content instanceof AdaptableView) {
                ( (AdaptableView) content ).setView(coordinateView);
            }
            content.render(viewPortX, viewPortY, viewPortW, viewPortH);
            if (this.alignment == AlignmentConstants.VERTICAL) {
                xyCondBoundsTextR.render(viewPortX, vpy, viewPortW, h);
                xyCondBoundsLinesR.render(viewPortX, vpy, viewPortW, h);
            } else if (this.alignment == AlignmentConstants.HORIZONTAL) {
                xyCondBoundsTextR.render(vpx, viewPortY, w, viewPortH);
                xyCondBoundsLinesR.render(vpx, viewPortY, w, viewPortH);
            }
            GL11.glViewport(vpx, vpy, w, h);
        }
        postContentLinesR.render(vpx, vpy, w, h);
        postContentTextR.render(vpx, vpy, w, h);

        // draw legends
        if (Objects.nonNull(legendRight)) {
            legendRight.glInit();
            GL11.glViewport(vpx + legendRightViewPort.x, vpy + legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            legendRight.render(vpx + legendRightViewPort.x, vpy + legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            GL11.glViewport(vpx, vpy, w, h);
        }
        if (Objects.nonNull(legendBottom)) {
            legendBottom.glInit();
            GL11.glViewport(vpx + legendBottomViewPort.x, vpy + legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            legendBottom.render(vpx + legendBottomViewPort.x, vpy + legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            GL11.glViewport(vpx, vpy, w, h);
        }

        // draw overlay
        if (Objects.nonNull(overlay)) {
            overlay.glInit();
            overlay.render(vpx, vpy, w, h);
        }
    }

    @Override
    public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
        if (!isEnabled()) {
            return;
        }
        currentViewPort.setRect(0, 0, w, h);
        if (isDirty || viewportwidth != w || viewportheight != h) {
            // update axes
            viewportwidth = w;
            viewportheight = h;
            setupAndLayout();
            isDirty = false;
        }

        preContentLinesR.renderFallback(g, p, w, h);
        preContentTextR.renderFallback(g, p, w, h);
        if (content != null) {
            int viewPortX = (int) coordsysAreaLB.getX();
            int viewPortY = (int) coordsysAreaLB.getY();
            int viewPortW = (int) coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int) coordsysAreaLB.distance(coordsysAreaLT);
            if (content instanceof AdaptableView) {
                ( (AdaptableView) content ).setView(coordinateView);
            }
            // create viewport graphics
            Graphics2D g_ = (Graphics2D) g.create(viewPortX, viewPortY, viewPortW, viewPortH);
            Graphics2D p_ = (Graphics2D) p.create(viewPortX, viewPortY, viewPortW, viewPortH);
            content.renderFallback(g_, p_, viewPortW, viewPortH);
            if (this.alignment == AlignmentConstants.VERTICAL) {
                Graphics2D g__ = (Graphics2D) g.create(viewPortX, 0, viewPortW, h);
                Graphics2D p__ = (Graphics2D) p.create(viewPortX, 0, viewPortW, h);
                xyCondBoundsLinesR.renderFallback(g__, p__, viewPortW, h);
                xyCondBoundsTextR.renderFallback(g__, p__, viewPortW, h);
            } else if (this.alignment == AlignmentConstants.HORIZONTAL) {
                Graphics2D g__ = (Graphics2D) g.create(0, viewPortY, w, viewPortH);
                Graphics2D p__ = (Graphics2D) p.create(0, viewPortY, w, viewPortH);
                xyCondBoundsLinesR.renderFallback(g__, p__, w, viewPortH);
                xyCondBoundsTextR.renderFallback(g__, p__, w, viewPortH);
            }
        }
        postContentLinesR.renderFallback(g, p, w, h);
        postContentTextR.renderFallback(g, p, w, h);

        // draw legends
        if (Objects.nonNull(legendRight)) {
            // create viewport graphics
            Graphics2D g_ = (Graphics2D) g.create(legendRightViewPort.x, legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            Graphics2D p_ = (Graphics2D) p.create(legendRightViewPort.x, legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
            legendRight.renderFallback(g_, p_, legendRightViewPort.width, legendRightViewPort.height);
        }
        if (Objects.nonNull(legendBottom)) {
            // create viewport graphics
            Graphics2D g_ = (Graphics2D) g.create(legendBottomViewPort.x, legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            Graphics2D p_ = (Graphics2D) p.create(legendBottomViewPort.x, legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
            legendBottom.renderFallback(g_, p_, legendBottomViewPort.width, legendBottomViewPort.height);
        }

        // draw overlay
        if (Objects.nonNull(overlay)) {
            overlay.renderFallback(g, p, w, h);
        }
    }

    @Override
    public void renderSVG(Document doc, Element parent, int w, int h) {
        if (!isEnabled()) {
            return;
        }
        preContentLinesR.renderSVG(doc, parent, w, h);
        preContentTextR.renderSVG(doc, parent, w, h);
        if (content != null) {
            int viewPortX = (int) coordsysAreaLB.getX();
            int viewPortY = (int) coordsysAreaLB.getY();
            int viewPortW = (int) coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int) coordsysAreaLB.distance(coordsysAreaLT);
            if (content instanceof AdaptableView) {
                ( (AdaptableView) content ).setView(coordinateView);
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
            contentGroup.setAttributeNS(null, "transform", "translate(" + ( viewPortX ) + "," + ( viewPortY ) + ")");
            contentGroup.setAttributeNS(null, "clip-path", "url(#" + clipDefID + ")");
            // render the content into the group
            content.renderSVG(doc, contentGroup, viewPortW, viewPortH);

            // content group for conditional content
            Element xyCondContentGroup = SVGUtils.createSVGElement(doc, "g");
            parent.appendChild(xyCondContentGroup);
            Node xyCondDefs = SVGUtils.getDefs(doc);
            Element xyCondClip = SVGUtils.createSVGElement(doc, "clipPath");
            String xyCondClipDefID = SVGUtils.newDefId();
            xyCondClip.setAttributeNS(null, "id", xyCondClipDefID);
            xyCondClip.appendChild(SVGUtils.createSVGRect(doc, 0, 0, viewPortW, viewPortH));
            xyCondDefs.appendChild(xyCondClip);

            if (this.alignment == AlignmentConstants.VERTICAL) {
                // transform the group according to the viewport position and clip it
                xyCondContentGroup.setAttributeNS(null, "transform", "translate(" + ( viewPortX ) + "," + ( 0 ) + ")");
                xyCondContentGroup.setAttributeNS(null, "clip-path", "url(#" + xyCondClipDefID + ")");
                xyCondBoundsTextR.renderSVG(doc, xyCondContentGroup, viewPortW, h);
                xyCondBoundsLinesR.renderSVG(doc, xyCondContentGroup, viewPortW, h);
            } else if (this.alignment == AlignmentConstants.HORIZONTAL) {
                // transform the group according to the viewport position and clip it
                xyCondContentGroup.setAttributeNS(null, "transform", "translate(" + ( 0 ) + "," + ( viewPortY ) + ")");
                xyCondContentGroup.setAttributeNS(null, "clip-path", "url(#" + xyCondClipDefID + ")");
                xyCondBoundsTextR.renderSVG(doc, xyCondContentGroup, w, viewPortH);
                xyCondBoundsLinesR.renderSVG(doc, xyCondContentGroup, w, viewPortH);
            }
        }
        postContentLinesR.renderSVG(doc, parent, w, h);
        postContentTextR.renderSVG(doc, parent, w, h);
        // draw legends
        if (Objects.nonNull(legendRight)) {
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
            legendGroup.setAttributeNS(null, "transform", "translate(" + ( legendRightViewPort.x ) + "," + ( legendRightViewPort.y ) + ")");
            legendGroup.setAttributeNS(null, "clip-path", "url(#" + clipDefID + ")");
            // render the content into the group
            legendRight.renderSVG(doc, legendGroup, legendRightViewPort.width, legendRightViewPort.height);
        }
        if (Objects.nonNull(legendBottom)) {
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
            legendGroup.setAttributeNS(null, "transform", "translate(" + ( legendBottomViewPort.x ) + "," + ( legendBottomViewPort.y ) + ")");
            legendGroup.setAttributeNS(null, "clip-path", "url(#" + clipDefID + ")");
            // render the content into the group
            legendBottom.renderSVG(doc, legendGroup, legendBottomViewPort.width, legendBottomViewPort.height);
        }
    }

    @Override
    public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
        if (!isEnabled()) {
            return;
        }
        preContentLinesR.renderPDF(doc, page, x, y, w, h);
        preContentTextR.renderPDF(doc, page, x, y, w, h);
        if (content != null) {
            int viewPortX = (int) coordsysAreaLB.getX();
            int viewPortY = (int) coordsysAreaLB.getY();
            int viewPortW = (int) coordsysAreaLB.distance(coordsysAreaRB);
            int viewPortH = (int) coordsysAreaLB.distance(coordsysAreaLT);
            if (content instanceof AdaptableView) {
                ( (AdaptableView) content ).setView(coordinateView);
            }
            // render the content into the group
            content.renderPDF(doc, page, viewPortX, viewPortY, viewPortW, viewPortH);
            // render conditional content: e.g. bar identifier
            if (this.alignment == AlignmentConstants.VERTICAL) {
                xyCondBoundsTextR.renderPDF(doc, page, viewPortX, 0, viewPortW, h);
                xyCondBoundsLinesR.renderPDF(doc, page, viewPortX, 0, viewPortW, h);
            } else if (this.alignment == AlignmentConstants.HORIZONTAL) {
                xyCondBoundsTextR.renderPDF(doc, page, 0, viewPortY, w, viewPortH);
                xyCondBoundsLinesR.renderPDF(doc, page, 0, viewPortY, w, viewPortH);
            }
        }
        postContentLinesR.renderPDF(doc, page, x, y, w, h);
        postContentTextR.renderPDF(doc, page, x, y, w, h);
        // draw legends
        if (Objects.nonNull(legendRight)) {
            legendRight.renderPDF(doc, page,
                    legendRightViewPort.x, legendRightViewPort.y,
                    legendRightViewPort.width, legendRightViewPort.height);
        }
        if (Objects.nonNull(legendBottom)) {
            legendBottom.renderPDF(doc, page,
                    legendRightViewPort.x, legendRightViewPort.y,
                    legendRightViewPort.width, legendRightViewPort.height);
        }
    }

    /**
     * Sets the coordinate view. This is the range of x and y coordinates that is displayed by this
     * {@link CoordSysRenderer}. It is not the rectangular area in which the content appears on screen
     * but what coordinates that area corresponds to as a coordinate system.
     * <p>
     * This determines what range of coordinates is visible when rendering the {@link #content}.
     * By default the coordinate view covers the range [-1,1] for both x and y coordinates.
     * When the resulting value ranges maxX-minX or maxY-minY fall below 1e-9, the method
     * refuses to set the view accordingly to prevent unrecoverable zooming and inaccurate
     * or broken renderings due to floating point precision.
     * <p>
     * This method also sets the {@link #isDirty} state of this {@link CoordSysRenderer} to true.
     * <p>
     * When {@link CoordinateViewListener} are registered to this renderer, they will be notified
     * before this method returns.
     *
     * @param minX minimum x coordinate visible in the coordinate system
     * @param minY minimum y coordinate visible in the coordinate system
     * @param maxX maximum x coordinate visible in the coordinate system
     * @param maxY maximum y coordinate visible in the coordinate system
     * @return this for chaining
     */
    public CombinedBarRenderer setCoordinateView(double minX, double minY, double maxX, double maxY) {
        return setCoordinateViewRect(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Sets the coordinate view. This is the range of x and y coordinates that is displayed by this
     * {@link CoordSysRenderer}. It is not the rectangular area in which the content appears on screen
     * but what coordinates that area corresponds to as a coordinate system.
     * <p>
     * See also {@link #setCoordinateView(double, double, double, double)}.
     *
     * @param viewRect to set the view to
     * @return this for chaining
     */
    public CombinedBarRenderer setCoordinateView(Rectangle2D viewRect) {
        return setCoordinateViewRect(viewRect.getMinX(), viewRect.getMinY(), viewRect.getWidth(), viewRect.getHeight());
    }

    protected CombinedBarRenderer setCoordinateViewRect(double x, double y, double w, double h) {
        if (w < 1e-9 || h < 1e-9) {
            System.err.printf("hitting coordinate area precision limit, x-range:%e, y-range:%e%n", w, h);
            return this;
        }
        this.coordinateView = new Rectangle2D.Double(x, y, w, h);
        setDirty();
        if (Objects.nonNull(coordviewListener)) {
            coordviewListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, "setCoordinateView"));
        }
        return this;
    }

    /**
     * Adds a {@link CoordinateViewListener} to this renderer which will be notified
     * whenever the coordinate view changes
     * (i.e. when {@link #setCoordinateView(double, double, double, double)} is called)
     *
     * @param l listener
     * @return this for chaining
     */
    synchronized public CombinedBarRenderer addCoordinateViewListener(CoordinateViewListener l) {
        if (l == null)
            return this;
        coordviewListener = AWTEventMulticaster.add(coordviewListener, l);
        return this;
    }

    /**
     * Removes the specified {@link CoordinateViewListener} from this renderer.
     *
     * @param l listener
     * @return this for chaining
     */
    synchronized public CombinedBarRenderer removeActionListener(CoordinateViewListener l) {
        if (l == null)
            return this;
        coordviewListener = AWTEventMulticaster.remove(coordviewListener, l);
        return this;
    }

    /**
     * Returns the coordinate view, which is the range of x and y coordinates visible in the
     * coordinate system.
     * See {@link #setCoordinateView(double, double, double, double)}.
     *
     * @return the coordinate view
     */
    public Rectangle2D getCoordinateView() {
        return coordinateView;
    }

    /**
     * @return the area of this renderer in which the coordinate system contents are rendered.
     * It is the viewPort for the {@link #content} renderer which is enclosed by
     * the coordinate system axes.
     */
    @Annotations.GLCoordinates
    public Rectangle2D getCoordSysArea() {
        return new Rectangle2D.Double(
                coordsysAreaLB.getX() + currentViewPort.x,
                coordsysAreaLB.getY() + currentViewPort.y,
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
     *
     * @param awtPoint     to be transformed
     * @param canvasheight height of the canvas this {@link CoordSysRenderer} is drawn to
     * @return transformed location
     */
    public Point2D transformAWT2CoordSys(Point2D awtPoint, int canvasheight) {
        Point2D glp = Utils.swapYAxis(awtPoint, canvasheight);
        return transformGL2CoordSys(glp);
    }

    /**
     * Transforms a location in GL coordinates on this renderer to the
     * corresponding coordinates in the coordinate system view.
     *
     * @param point to be transformed
     * @return transformed location
     */
    @Annotations.GLCoordinates
    public Point2D transformGL2CoordSys(Point2D point) {
        Rectangle2D coordSysArea = getCoordSysArea();
        Rectangle2D coordinateView = getCoordinateView();
        double x = point.getX() - coordSysArea.getMinX();
        double y = point.getY() - coordSysArea.getMinY();
        x /= coordSysArea.getWidth() - 1;
        y /= coordSysArea.getHeight() - 1;
        x = x * coordinateView.getWidth() + coordinateView.getMinX();
        y = y * coordinateView.getHeight() + coordinateView.getMinY();
        return new Point2D.Double(x, y);
    }

    /**
     * Transforms a location in coordinates of the current coordinate view
     * to corresponding coordinates of this renderer (in GL coords).
     *
     * @param point to be transformed
     * @return transformed location
     */
    @Annotations.GLCoordinates
    public Point2D transformCoordSys2GL(Point2D point) {
        Rectangle2D coordSysArea = getCoordSysArea();
        Rectangle2D coordSysView = getCoordinateView();
        double x = point.getX() - coordSysView.getMinX();
        double y = point.getY() - coordSysView.getMinY();
        x /= coordSysView.getWidth();
        y /= coordSysView.getHeight();
        x = x * ( coordSysArea.getWidth() - 1 ) + coordSysArea.getMinX();
        y = y * ( coordSysArea.getHeight() - 1 ) + coordSysArea.getMinY();
        return new Point2D.Double(x, y);
    }

    /**
     * Transforms a location in coordinates of the current coordinate view
     * to corresponding coordinates of this renderer's canvas in AWT coordinates
     * (where y axis extends to bottom).
     *
     * @param point        to be transformed
     * @param canvasheight height of the canvas this {@link CoordSysRenderer} is drawn to
     * @return transformed location
     */
    public Point2D transformCoordSys2AWT(Point2D point, int canvasheight) {
        Point2D glPoint = transformCoordSys2GL(point);
        return Utils.swapYAxis(glPoint, canvasheight);
    }

    @Override
    public void close() {
        if (Objects.nonNull(preContentTextR))
            preContentTextR.close();
        if (Objects.nonNull(preContentLinesR))
            preContentLinesR.close();
        if (Objects.nonNull(preContentTextR))
            postContentTextR.close();
        if (Objects.nonNull(preContentLinesR))
            postContentLinesR.close();
        if (Objects.nonNull(content))
            content.close();
        if (Objects.nonNull(legendRight))
            legendRight.close();
        if (Objects.nonNull(legendBottom))
            legendBottom.close();
        if (Objects.nonNull(overlay))
            overlay.close();
    }

    @Override
    public void setEnabled(boolean enable) {
        this.isEnabled = enable;
    }

    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    // add item to renderer
    public void addBarGroup(final BarGroup barGroup) {
        this.groupedBars.add(barGroup);
        this.setupAndLayout();
    }

    /**
     * @return
     */
    // TODO padding bei den bars hinzufügen, bspw. links rechts bei VERTICAL
    public Rectangle2D getBounds() {
        // default values
        double minX = 0;
        double minY = 0;
        double maxX = 1;
        double maxY = 1;
        if (this.alignment == AlignmentConstants.HORIZONTAL) {
            minY = 0;
            minX = groupedBars.parallelStream()
                    .map(e->e.getBounds(this.alignment))
                    .mapToDouble(RectangularShape::getMinX)
                    .min().orElse(0);
            maxY = groupedBars.parallelStream()
                    .map(e->e.getBounds(this.alignment))
                    .mapToDouble(RectangularShape::getHeight)
                    .sum();
            maxX = groupedBars.parallelStream()
                    .map(e->e.getBounds(this.alignment))
                    .mapToDouble(RectangularShape::getWidth)
                    .max().orElse(0);
            return new Rectangle2D.Double(minX, minY - ( barSize / 2 ), (maxX - minX), ( maxY - minY ) + barSize);
        } else if (this.alignment == AlignmentConstants.VERTICAL) {
            minX = 0;
            minY = groupedBars.parallelStream()
                    .map(e->e.getBounds(AlignmentConstants.VERTICAL))
                    .mapToDouble(RectangularShape::getMinY)
                    .min().orElse(0);
            maxX = groupedBars.parallelStream()
                    .map(e->e.getBounds(AlignmentConstants.VERTICAL))
                    .mapToDouble(RectangularShape::getWidth)
                    .sum();
            maxY = groupedBars.parallelStream()
                    .map(e->e.getBounds(AlignmentConstants.VERTICAL))
                    .mapToDouble(RectangularShape::getHeight)
                    .max().orElse(0);
            return new Rectangle2D.Double(minX - (barSize / 2), minY, (maxX - minX) + barSize, maxY - minY);
        }
        return new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
    }
}
