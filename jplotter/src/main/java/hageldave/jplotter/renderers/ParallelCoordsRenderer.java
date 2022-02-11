package hageldave.jplotter.renderers;


/*
 * TODO
 *  This renderer is pretty similar to ParallelCoordsRenderer with the exception
 *  that there shouldn't be as many interaction classes.
 *  The same applies to the BarRenderer in the barchart branch
 *  Maybe some sort of coordinate system interface could be thought of, 
 *  which already defines the common methods, so that they can be reused.
 */

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
    protected Text xAxisLabelText = new Text("", 13, Font.PLAIN);
    protected Text yAxisLabelText = new Text("", 13, Font.PLAIN);


    protected LinkedList<Feature> features = new LinkedList<>();

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

    protected String yAxisLabel = null;

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
        this.preContentLinesR
                .addItemToRender(guides)
                .addItemToRender(ticks);
        this.preContentTextR
                .addItemToRender(xAxisLabelText)
                .addItemToRender(yAxisLabelText);

        this.guideColor = ()->getColorScheme().getColor4();
        this.tickColor = ()->getColorScheme().getColor3();
        this.textColor = ()->getColorScheme().getColorText();

        updateColors();
    }

    /**
     * Helper method to update the colors if the color scheme is changed.
     * @return this for chaining
     */
    protected ParallelCoordsRenderer updateColors() {
        this.xAxisLabelText.setColor(this.textColor.getAsInt());
        this.yAxisLabelText.setColor(this.textColor.getAsInt());

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

    public LinkedList<Feature> getFeatures() {
        return features;
    }

    public ParallelCoordsRenderer addFeature(Feature feature) {
        this.features.add(feature);
        return this;
    }

    public ParallelCoordsRenderer addFeature(double min, double max, String label) {
        return this.addFeature(new Feature(min, max, label));
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
        //final int xLabelfontSize = 12;
        final int style = Font.PLAIN;

        // find maximum length of y axis labels of first feature
        int maxYTickLabelWidth = 0;
        if (!features.isEmpty()) {
            Pair<double[],String[]> featTicksAndLabels = tickMarkGenerator.genTicksAndLabels(features.getFirst().min, features.getFirst().max, 5, true);
            for(String label : featTicksAndLabels.second){
                int labelW = CharacterAtlas.boundsForText(label.length(), tickfontSize, style).getBounds().width;
                maxYTickLabelWidth = Math.max(maxYTickLabelWidth, labelW);
            }
        }
        int maxXTickLabelHeight = CharacterAtlas.boundsForText(1, tickfontSize, style).getBounds().height;
        int maxLabelHeight = CharacterAtlas.boundsForText(1, yLabelfontSize, style).getBounds().height;

        int legendRightW = Objects.nonNull(legendRight) ? legendRightWidth+4:0;
        int legendBotH = Objects.nonNull(legendBottom) ? legendBottomHeight+4:0;

        // move coordwindow origin so that labels have enough display space
        coordsysAreaLB.x[0] = maxYTickLabelWidth + paddingLeft + 7;
        coordsysAreaLB.y[0] = maxXTickLabelHeight + paddingBot + legendBotH + 12;
        // move opposing corner of coordwindow to have enough display space
        coordsysAreaRT.x[0] = viewportwidth-paddingRight-maxLabelHeight-legendRightW-10;
        coordsysAreaRT.y[0] = viewportheight-paddingTop-maxLabelHeight-4;

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
            Pair<double[],String[]> featTicksAndLabels = tickMarkGenerator.genTicksAndLabels(feature.min, feature.max, 5, true);

            // tick
            double m = (double) index / (features.size()-1);
            double x = coordsysAreaLB.getX()+m*axisDimensions.getWidth();
            Point2D onaxis = new Point2D.Double(Math.round(x),coordsysAreaLB.getY());

            // label
            Text label = new Text(feature.label, tickfontSize, Font.BOLD, this.textColor.getAsInt());
            Dimension textSize = label.getTextSize();
            label.setOrigin(new Point2D.Double(
                    (int)(onaxis.getX()-textSize.getWidth()/2.0),
                    (int)(onaxis.getY()-12-textSize.getHeight())+0.5));

            if (lastMaxX > label.getBounds().getMinX()) {
                shiftLabels = true;
                break;
            }

            lastMaxX = label.getBounds().getMaxX();
            tickMarkLabels.add(label);

            // feature guide
            guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, axisDimensions.getHeight())).setColor(guideColor);

            double[] yticks = featTicksAndLabels.first;
            String[] yticklabels = featTicksAndLabels.second;

            // get max of the ticks to normalize them to the 0-1 area
            double maxValue = Arrays.stream(yticks).max().orElse(1.0);
            double minValue = Arrays.stream(yticks).min().orElse(0.0);

            // yaxis ticks
            for(int i=0; i<yticks.length; i++){
                // tick
                double y_m = (yticks[i]-minValue)/(maxValue-minValue);
                double y = y_m*axisDimensions.getHeight();
                Point2D onYaxis = new TranslatedPoint2D(new PointeredPoint2D(0, coordsysAreaLB.getY()), x, Math.round(y));
                ticks.addSegment(onYaxis, new TranslatedPoint2D(onYaxis, -4, 0)).setColor(tickColor);
                // label
                Text yLabel = new Text(yticklabels[i], tickfontSize, style, this.textColor.getAsInt());
                Dimension yTextSize = yLabel.getTextSize();
                yLabel.setOrigin(new TranslatedPoint2D(onYaxis, -7-yTextSize.getWidth(), -Math.round(yTextSize.getHeight()/2.0)+0.5));

                // we need to use a different one here, as tickMarkLabels get cleared when labels have to be shifted
                tickMarkLabels.add(yLabel);
            }
            index++;
        }

        if (shiftLabels) {
            clearCoordSysBuildingBlocks();
            index=0;
            for (Feature feature : features) {
                Pair<double[],String[]> featTicksAndLabels = tickMarkGenerator.genTicksAndLabels(feature.min, feature.max, 5, true);

                // calculate first the new coord view bounds
                String[] allLabels = new String[features.size()-1];
                for (int i = 0; i < allLabels.length; i++)
                    allLabels[i] = features.get(i).label;

                // calculate first the new coord view bounds
                Rectangle2D rotatedXLblBounds = getMaxBoundsOfAllLabels(allLabels, Math.PI/4, tickfontSize, style);

                // add rotated label height to the y padding
                coordsysAreaLB.y[0] = paddingBot+legendBotH+rotatedXLblBounds.getHeight()+ 8;
                // check if y or x label is larger and add that size to the padding
                coordsysAreaLB.x[0] = Math.max(0, (int) rotatedXLblBounds.getWidth())+paddingLeft+ 8;
                coordsysAreaRT.x[0] = viewportwidth-paddingRight-maxLabelHeight-legendRightW- 8;

                // correct the coordsysArea so that it doesn't get inverted
                axisDimensions = preventCoordSysInversion();

                // now really put it into renderer
                double m = (double) index / (features.size()-1);
                double x = coordsysAreaLB.getX() + m * axisDimensions.getWidth();
                Point2D onaxis = new Point2D.Double(Math.round(x), coordsysAreaLB.getY());
                Text label = new Text(feature.label, tickfontSize, Font.BOLD, this.textColor.getAsInt());
                label.setAngle(Math.PI/4);
                label.setOrigin(new Point2D.Double(
                        (int) onaxis.getX()-label.getBoundsWithRotation().getWidth()+12,
                        (int) onaxis.getY()-label.getBoundsWithRotation().getHeight()-7));

                tickMarkLabels.add(label);
                // guide
                guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, axisDimensions.getHeight())).setColor(guideColor);

                // yaxis ticks
                double[] yticks = featTicksAndLabels.first;
                String[] yticklabels = featTicksAndLabels.second;

                // get max of the ticks to normalize them to the 0-1 area
                double maxValue = Arrays.stream(yticks).max().orElse(1.0);
                double minValue = Arrays.stream(yticks).min().orElse(0.0);

                for(int i=0; i<yticks.length; i++){
                    // tick
                    double y_m = (yticks[i]-minValue)/(maxValue-minValue);
                    double y = y_m*axisDimensions.getHeight();
                    Point2D onYaxis = new TranslatedPoint2D(new PointeredPoint2D(0, coordsysAreaLB.getY()), x, Math.round(y));
                    ticks.addSegment(onYaxis, new TranslatedPoint2D(onYaxis, -4, 0)).setColor(tickColor);
                    // label
                    Text yLabel = new Text(yticklabels[i], tickfontSize, style, this.textColor.getAsInt());
                    Dimension yTextSize = yLabel.getTextSize();
                    yLabel.setOrigin(new TranslatedPoint2D(onYaxis, -7-yTextSize.getWidth(), -Math.round(yTextSize.getHeight()/2.0)+0.5));

                    // we need to use a different one here, as tickMarkLabels get cleared when labels have to be shifted
                    tickMarkLabels.add(yLabel);
                }
                index++;
            }
        }

        for(Text txt: tickMarkLabels){
            preContentTextR.addItemToRender(txt);
        }

        // axis labels
        yAxisLabelText.setTextString(getyAxisLabel());
        yAxisLabelText.setAngle(-(float)Math.PI/2);
        yAxisLabelText.setOrigin(new TranslatedPoint2D(coordsysAreaRB, 12, axisDimensions.getHeight()/2 + yAxisLabelText.getTextSize().width/2));

        // setup legend areas (this will stay the same)
        if(Objects.nonNull(legendRight)){
            legendRightViewPort.setBounds(
                    (int)(yAxisLabelText.getOrigin().getX()+yAxisLabelText.getTextSize().getHeight()+4),
                    paddingBot,
                    legendRightWidth,
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

    private Rectangle2D preventCoordSysInversion() {
        coordsysAreaLB.y[0] = Math.min(coordsysAreaRT.y[0] + 1, coordsysAreaLB.y[0]);
        coordsysAreaRT.x[0] = Math.max(coordsysAreaLB.x[0] + 1, coordsysAreaRT.x[0]);
        return new Rectangle2D.Double(0, 0, coordsysAreaLB.distance(coordsysAreaRB), coordsysAreaLB.distance(coordsysAreaLT));
    }

    private void clearCoordSysBuildingBlocks() {
        for(Text txt:tickMarkLabels){
            preContentTextR.removeItemToRender(txt);
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
     * @return "Y" if {@link #yAxisLabel} is null or the actual axis label.
     */
    public String getyAxisLabel() {
        return yAxisLabel == null ? "Y":yAxisLabel;
    }



    /**
     * Sets the specified string as the y axis label which appears to the right of the coordinate system.
     * Sets the {@link #isDirty} state of this {@link ParallelCoordsRenderer} to true.
     * @param yAxisLabel to set
     * @return this for chaining
     */
    public ParallelCoordsRenderer setyAxisLabel(String yAxisLabel) {
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
            // update axes
            //axes.setDirty();
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
            // update axes
            //axes.setDirty();
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
    @Override
    public void setEnabled(boolean enable) {
        this.isEnabled = enable;
    }
    @Override
    public boolean isEnabled() {
        return this.isEnabled;
    }

    public static class Feature {
        public final double min;
        public final double max;
        public final String label;

        public Feature(double min, double max, String label) {
            this.min = min;
            this.max = max;
            this.label = label;
        }
    }
}
