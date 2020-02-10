package hageldave.jplotter.renderers;

import java.awt.AWTEventMulticaster;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import hageldave.jplotter.coordsys.ExtendedWilkinson;
import hageldave.jplotter.coordsys.TickMarkGenerator;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordinateViewListener;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PointeredPoint2D;
import hageldave.jplotter.util.TranslatedPoint2D;
import hageldave.jplotter.util.Utils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Annotations.GLCoordinates;

/**
 * The CoordSysRenderer is a {@link Renderer} that displays a coordinate system.
 * This coordinate system is enclosed by 4 axes that form a rectangle around the
 * area that displays the contents of the coordinate system.
 * <p>
 * The upper x-axis and right y-axis feature the labels (names) of the axes.
 * The lower x-axis and the left y-axis feature tick marks and labels that
 * help to orientate and read off coordinates.
 * The positioning and labeling of the tick marks is done by a {@link TickMarkGenerator}
 * which is per default an instance of {@link ExtendedWilkinson}.
 * For each tick a vertical or horizontal guide line is drawn across the area of the
 * coordinate system.
 * <p>
 * What coordinate range the coordinate system area corresponds to is controlled by
 * the coordinate view (see {@link #setCoordinateView(double, double, double, double)})
 * and defaults to [-1,1] for both axes.
 * The contents that are drawn inside the coordinate area are rendered by the content renderer
 * (see {@link #setContent(Renderer)}).
 * If that renderer implements the {@link AdaptableView} interface it will be passed the
 * view matrix corresponding to the coordinate view.
 * The content renderer will be able to draw within the viewport defined by the coordinate
 * system area of this CoordSysRenderer.
 * <p>
 * Optionally a {@link Renderer} for drawing a legend (such as the {@link Legend} class)
 * can be set to either the bottom or right hand side of the coordinate system (can also
 * use both areas at once).
 * Use {@link #setLegendBottom(Renderer)} or {@link #setLegendRight(Renderer)} to do so.
 * The legend area size can be partially controlled by {@link #setLegendBottomHeight(int)}
 * and {@link #setLegendRightWidth(int)} if this is needed.
 * <p>
 * The overlay renderer ({@link #setOverlay(Renderer)}) can be used to finally draw over all
 * of the renderer viewport.
 * <p>
 * For interacting with this {@link CoordSysRenderer} there already exist implementations of MouseListeners
 * for panning and zooming (see {@link CoordSysPanning} and {@link CoordSysScrollZoom}).
 * 
 * @author hageldave
 */
public class CoordSysRenderer implements Renderer {

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
	
	@GLCoordinates
	protected Rectangle legendRightViewPort = new Rectangle();
	@GLCoordinates
	protected Rectangle legendBottomViewPort = new Rectangle();

	protected Rectangle2D coordinateView = new Rectangle2D.Double(-1,-1,2,2);

	protected TickMarkGenerator tickMarkGenerator = new ExtendedWilkinson();

	protected Lines axes = new Lines().setVertexRoundingEnabled(true);
	protected Lines ticks = new Lines().setVertexRoundingEnabled(true);
	protected Lines guides = new Lines().setVertexRoundingEnabled(true);
	protected LinkedList<Text> tickMarkLabels = new LinkedList<>();
	protected Text xAxisLabelText = new Text("", 13, Font.PLAIN);
	protected Text yAxisLabelText = new Text("", 13, Font.PLAIN);

	protected double[] xticks;
	protected double[] yticks;

	protected int viewportwidth=0;
	protected int viewportheight=0;
	protected boolean isDirty = true;

	protected Color tickColor = Color.DARK_GRAY;
	protected Color guideColor = new Color(0xdddddd);

	protected int paddingLeft = 10;
	protected int paddingRight = 10;
	protected int paddingTop = 10;
	protected int paddingBot = 10;

	@GLCoordinates
	protected PointeredPoint2D coordsysAreaLB = new PointeredPoint2D();
	@GLCoordinates
	protected PointeredPoint2D coordsysAreaRT = Utils.copy(coordsysAreaLB);
	@GLCoordinates
	protected PointeredPoint2D coordsysAreaLT = new PointeredPoint2D(coordsysAreaLB.x, coordsysAreaRT.y);
	@GLCoordinates
	protected PointeredPoint2D coordsysAreaRB = new PointeredPoint2D(coordsysAreaRT.x, coordsysAreaLB.y);

	protected String xAxisLabel = null;
	protected String yAxisLabel = null;
	
	protected ActionListener coordviewListener;
	protected boolean isEnabled=true;
	
	public CoordSysRenderer() {
		this.axes.addSegment(coordsysAreaLB, coordsysAreaRB).setColor(Color.BLACK);
		this.axes.addSegment(coordsysAreaLB, coordsysAreaLT).setColor(Color.BLACK);
		this.axes.addSegment(coordsysAreaLT, coordsysAreaRT).setColor(Color.GRAY);
		this.axes.addSegment(coordsysAreaRB, coordsysAreaRT).setColor(Color.GRAY);
		this.axes.setGlobalThicknessMultiplier(2);
		this.preContentLinesR
		.addItemToRender(guides)
		.addItemToRender(ticks);
		this.preContentTextR
		.addItemToRender(xAxisLabelText)
		.addItemToRender(yAxisLabelText);
		this.postContentLinesR.addItemToRender(axes);
	}
	
	/**
	 * Sets the {@link #isDirty} state of this CoordSysRenderer to true.
	 * This indicates that axis locations, tick marks, labels and guides
	 * have to be recomputed.
	 * This will be done during {@link #paintToFBO(int, int)} which
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
		this.legendRight= legend;
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
		return old;
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
	 * @param padding amount of blank area
	 * @return this for chaining
	 */
	public CoordSysRenderer setPaddingLeft(int padding) {
		this.paddingLeft = padding;
		return this;
	}

	/**
	 * Sets the padding to the right side, which is the size of the blank area
	 * before any visible elements of the CoordSysRenderer are drawn
	 * @param padding amount of blank area
	 * @return this for chaining
	 */
	public CoordSysRenderer setPaddingRight(int padding) {
		this.paddingRight = padding;
		return this;
	}

	/**
	 * Sets the padding to the top side, which is the size of the blank area
	 * before any visible elements of the CoordSysRenderer are drawn
	 * @param padding amount of blank area
	 * @return this for chaining
	 */
	public CoordSysRenderer setPaddingTop(int padding) {
		this.paddingTop = padding;
		return this;
	}

	/**
	 * Sets the padding to the bottom side, which is the size of the blank area
	 * before any visible elements of the CoordSysRenderer are drawn
	 * @param padding amount of blank area
	 * @return this for chaining
	 */
	public CoordSysRenderer setPaddingBot(int padding) {
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
	public CoordSysRenderer setLegendBottomHeight(int legendBottomHeight) {
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
	public CoordSysRenderer setLegendRightWidth(int legendRightWidth) {
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
	@GLContextRequired
	protected void setupAndLayout() {
		Pair<double[],String[]> xticksAndLabels = tickMarkGenerator.genTicksAndLabels(
				coordinateView.getMinX(), 
				coordinateView.getMaxX(), 
				5, 
				false);
		Pair<double[],String[]> yticksAndLabels = tickMarkGenerator.genTicksAndLabels(
				coordinateView.getMinY(), 
				coordinateView.getMaxY(), 
				5, 
				true);
		this.xticks = xticksAndLabels.first;
		this.yticks = yticksAndLabels.first;
		String[] xticklabels = xticksAndLabels.second;
		String[] yticklabels = yticksAndLabels.second;

		final int tickfontSize = 11;
		final int labelfontSize = 12;
		final int style = Font.PLAIN;
		// find maximum length of y axis labels
		int maxYTickLabelWidth = 0;
		for(String label:yticklabels){
			int labelW = CharacterAtlas.boundsForText(label.length(), tickfontSize, style).getBounds().width;
			maxYTickLabelWidth = Math.max(maxYTickLabelWidth, labelW);
		}
		int maxXTickLabelHeight = CharacterAtlas.boundsForText(1, tickfontSize, style).getBounds().height;
		int maxLabelHeight = CharacterAtlas.boundsForText(1, labelfontSize, style).getBounds().height;

		int legendRightW = Objects.nonNull(legendRight) ? legendRightWidth+4:0;
		int legendBotH = Objects.nonNull(legendBottom) ? legendBottomHeight+4:0;

		// move coordwindow origin so that labels have enough display space
		coordsysAreaLB.x[0] = maxYTickLabelWidth + paddingLeft + 7;
		coordsysAreaLB.y[0] = maxXTickLabelHeight + paddingBot + legendBotH + 6;
		// move opposing corner of coordwindow to have enough display space
		coordsysAreaRT.x[0] = viewportwidth-paddingRight-maxLabelHeight-legendRightW-4;
		coordsysAreaRT.y[0] = viewportheight-paddingTop-maxLabelHeight-4;

		// dispose of old stuff
		ticks.removeAllSegments();
		guides.removeAllSegments();
		for(Text txt:tickMarkLabels){
			preContentTextR.removeItemToRender(txt);
			txt.close();
		}
		tickMarkLabels.clear();

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
			Text label = new Text(xticklabels[i], tickfontSize, style);
			Dimension textSize = label.getTextSize();
			label.setOrigin(new Point2D.Double(
					(int)(onaxis.getX()-textSize.getWidth()/2.0), 
					(int)(onaxis.getY()-6-textSize.getHeight())+0.5));
			tickMarkLabels.add(label);
			// guide
			guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, yAxisHeight)).setColor(guideColor);
		}
		// yaxis ticks
		for(int i=0; i<yticks.length; i++){
			// tick
			double m = (yticks[i]-coordinateView.getMinY())/coordinateView.getHeight();
			double y = m*yAxisHeight;
			Point2D onaxis = new TranslatedPoint2D(coordsysAreaLB, 0, Math.round(y));
			ticks.addSegment(onaxis, new TranslatedPoint2D(onaxis, -4, 0)).setColor(tickColor);
			// label
			Text label = new Text(yticklabels[i], tickfontSize, style);
			Dimension textSize = label.getTextSize();
			label.setOrigin(new TranslatedPoint2D(onaxis, -7-textSize.getWidth(), -Math.round(textSize.getHeight()/2.0)+0.5));
			tickMarkLabels.add(label);
			// guide
			guides.addSegment(onaxis, new TranslatedPoint2D(coordsysAreaRB, 0, m*yAxisHeight)).setColor(guideColor);
		}
		for(Text txt: tickMarkLabels){
			preContentTextR.addItemToRender(txt);
		}
		// axis labels
		xAxisLabelText.setTextString(getxAxisLabel());
		xAxisLabelText.setOrigin(new TranslatedPoint2D(coordsysAreaLT, xAxisWidth/2 - xAxisLabelText.getTextSize().width/2, 4));
		yAxisLabelText.setTextString(getyAxisLabel());
		yAxisLabelText.setAngle(-(float)Math.PI/2);
		yAxisLabelText.setOrigin(new TranslatedPoint2D(coordsysAreaRB, 4, yAxisHeight/2 + yAxisLabelText.getTextSize().width/2));

		// setup legend areas
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
	
	/**
	 * @return "X" if {@link #xAxisLabel} is null or the actual axis label.
	 */
	public String getxAxisLabel() {
		return xAxisLabel == null ? "X":xAxisLabel;
	}

	/**
	 * @return "Y" if {@link #yAxisLabel} is null or the actual axis label.
	 */
	public String getyAxisLabel() {
		return yAxisLabel == null ? "Y":yAxisLabel;
	}
	
	/**
	 * Sets the specified string as the x axis label which appears on top of the coordinate system.
	 * Sets the {@link #isDirty} state of this {@link CoordSysRenderer} to true.
	 * @param xAxisLabel to set
	 * @return this for chaining
	 */
	public CoordSysRenderer setxAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
		setDirty();
		return this;
	}

	/**
	 * Sets the specified string as the y axis label which appears to the right of the coordinate system.
	 * Sets the {@link #isDirty} state of this {@link CoordSysRenderer} to true.
	 * @param yAxisLabel to set
	 * @return this for chaining
	 */
	public CoordSysRenderer setyAxisLabel(String yAxisLabel) {
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
	 * @param tickMarkGenerator to be used for determining tick locations 
	 * and corresponding labels
	 * @return this for chaining
	 */
	public CoordSysRenderer setTickMarkGenerator(TickMarkGenerator tickMarkGenerator) {
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
	public void render(int w, int h) {
		if(!isEnabled()){
			return;
		}
		if(isDirty || viewportwidth != w || viewportheight != h){
			// update axes
			axes.setDirty();
			viewportwidth = w;
			viewportheight = h;
			setupAndLayout();
			isDirty = false;
		}
		preContentLinesR.render(w, h);
		preContentTextR.render(w, h);
		// draw into the coord system
		if(content != null){
			content.glInit();
			int viewPortX = (int)coordsysAreaLB.getX();
			int viewPortY = (int)coordsysAreaLB.getY();
			int viewPortW = (int)coordsysAreaLB.distance(coordsysAreaRB);
			int viewPortH = (int)coordsysAreaLB.distance(coordsysAreaLT);
			GL11.glViewport(viewPortX,viewPortY,viewPortW,viewPortH);
			if(content instanceof AdaptableView){
				((AdaptableView) content).setView(coordinateView);
			}
			content.render(viewPortW, viewPortH);
			GL11.glViewport(0, 0, w, h);
		}
		postContentLinesR.render(w, h);
		postContentTextR.render(w, h);

		// draw legends
		if(Objects.nonNull(legendRight)){
			legendRight.glInit();
			GL11.glViewport(legendRightViewPort.x, legendRightViewPort.y, legendRightViewPort.width, legendRightViewPort.height);
			legendRight.render(legendRightViewPort.width, legendRightViewPort.height);
			GL11.glViewport(0, 0, w, h);
		}
		if(Objects.nonNull(legendBottom)){
			legendBottom.glInit();
			GL11.glViewport(legendBottomViewPort.x, legendBottomViewPort.y, legendBottomViewPort.width, legendBottomViewPort.height);
			legendBottom.render(legendBottomViewPort.width, legendBottomViewPort.height);
			GL11.glViewport(0, 0, w, h);
		}

		// draw overlay
		if(Objects.nonNull(overlay)){
			overlay.glInit();
			overlay.render(w,h);
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
	public CoordSysRenderer setCoordinateView(double minX, double minY, double maxX, double maxY){
		if(maxX-minX < 1e-9 || maxY-minY < 1e-9){
			System.err.printf("hitting coordinate area precision limit, x-range:%e, y-range:%e%n", maxX-minX, maxY-minY);
			return this;
		}
		this.coordinateView = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		setDirty();
		if(Objects.nonNull(coordviewListener)){
			coordviewListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_FIRST, "setCoordinateView"));
		}
		return this;
	}
	
	/**
	 * Calls {@link #setCoordinateView(double, double, double, double)} with minX, minY, maxX, maxY
	 * of the specified rectangle 
	 * @param viewRect to set the view to
	 * @return this for chaining
	 */
	public CoordSysRenderer setCoordinateView(Rectangle2D viewRect){
		return setCoordinateView(viewRect.getMinX(), viewRect.getMinY(), viewRect.getMaxX(), viewRect.getMaxY());
	}

	/**
	 * Adds a {@link CoordinateViewListener} to this renderer which will be notified
	 * whenever the coordinate view changes 
	 * (i.e. when {@link #setCoordinateView(double, double, double, double)} is called)
	 * @param l listener
	 * @return this for chaining
	 */
	synchronized public CoordSysRenderer addCoordinateViewListener(CoordinateViewListener l){
		if(l==null)
			return this;
		coordviewListener = AWTEventMulticaster.add(coordviewListener, l);
		return this;
	}

	/**
	 * Removes the specified {@link CoordinateViewListener} from this renderer.
	 * @param l listener
	 * @return this for chaining
	 */
	synchronized public CoordSysRenderer removeActionListener(CoordinateViewListener l){
		if(l==null)
			return this;
		coordviewListener = AWTEventMulticaster.remove(coordviewListener, l);
		return this;
	}

	/**
	 * Returns the coordinate view, which is the range of x and y coordinates visible in the
	 * coordinate system.
	 * See {@link #setCoordinateView(double, double, double, double)}.
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
	@GLCoordinates
	public Rectangle2D getCoordSysArea() {
		return new Rectangle2D.Double(
				coordsysAreaLB.getX(), 
				coordsysAreaLB.getY(), 
				coordsysAreaLB.distance(coordsysAreaRB), 
				coordsysAreaLB.distance(coordsysAreaLT)
				);
	}
	
	/**
	 * Transforms a location in AWT coordinates (y axis extends to bottom) 
	 * on this renderer to the corresponding coordinates in the coordinate 
	 * system view (in GL coords).
	 * @param awtPoint to be transformed
	 * @return transformed location
	 */
	public Point2D transformAWT2CoordSys(Point2D awtPoint){
		awtPoint = Utils.swapYAxis(awtPoint, viewportheight);
		return transformGL2CoordSys(awtPoint);
	}
	
	/**
	 * Transforms a location in GL coordinates on this renderer to the 
	 * corresponding coordinates in the coordinate system view.
	 * @param point to be transformed
	 * @return transformed location
	 */
	@GLCoordinates
	public Point2D transformGL2CoordSys(Point2D point){
		Rectangle2D coordSysArea = getCoordSysArea();
		Rectangle2D coordinateView = getCoordinateView();
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
	@GLCoordinates
	public Point2D transformCoordSys2GL(Point2D point){
		Rectangle2D coordSysArea = getCoordSysArea();
		Rectangle2D coordSysView = getCoordinateView();
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
	 * to corresponding coordinates of this renderer in AWT coordinates 
	 * (where y axis extends to bottom).
	 * @param point to be transformed
	 * @return transformed location
	 */
	public Point2D transformCoordSys2AWT(Point2D point){
		Point2D glPoint = transformCoordSys2GL(point);
		return Utils.swapYAxis(glPoint, viewportheight);
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
		preContentTextR = null;
		preContentLinesR = null;
		postContentTextR = null;
		postContentLinesR = null;
		content = null;
		legendRight = null;
		legendBottom = null;
		overlay = null;
	}
	@Override
	public void setEnabled(boolean enable) {
		this.isEnabled = enable;
	}
	@Override
	public boolean isEnabled() {
		return this.isEnabled;
	}
	
}
