package jplotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.joml.Matrix3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.awt.GLData;

import jplotter.renderables.CharacterAtlas;
import jplotter.renderables.Lines;
import jplotter.renderables.Text;
import jplotter.renderers.AdaptableView;
import jplotter.renderers.LinesRenderer;
import jplotter.renderers.Renderer;
import jplotter.renderers.TextRenderer;
import jplotter.util.ExtendedWilkinson;
import jplotter.util.Pair;
import jplotter.util.PointeredPoint2D;
import jplotter.util.TickMarkGenerator;
import jplotter.util.TranslatedPoint2D;
import jplotter.util.Utils;

public class CoordSysCanvas extends FBOCanvas {
	private static final long serialVersionUID = 1L;

	LinesRenderer linesR = new LinesRenderer();
	TextRenderer textR = new TextRenderer();
	Rectangle2D coordinateArea = new Rectangle2D.Double(-1,-1,2,2);
	PointeredPoint2D coordWindowOrigin = new PointeredPoint2D(100, 50);
	
	TickMarkGenerator tickMarkGenerator = new ExtendedWilkinson();

	Renderer content=null;

	Lines axes = new Lines();
	Lines ticks = new Lines();
	Lines guides = new Lines();

	double[] xticks;
	double[] yticks;
	double[][] xticklocations;
	double[][] yticklocations;

	int viewportwidth=0;
	int viewportheight=0;
	boolean isDirty = true;

	Color tickColor = Color.DARK_GRAY;
	Color guideColor = new Color(0xdddddd);
	
	int leftPadding = 10;
	int rightPadding = 10;
	int topPadding = 10;
	int botPadding = 10;

	PointeredPoint2D coordsysframeLB = coordWindowOrigin;
	PointeredPoint2D coordsysframeRT = Utils.copy(coordWindowOrigin);
	PointeredPoint2D coordsysframeLT = new PointeredPoint2D(coordsysframeLB.x, coordsysframeRT.y);
	PointeredPoint2D coordsysframeRB = new PointeredPoint2D(coordsysframeRT.x, coordsysframeLB.y);

	Matrix3f coordSysScaleMX = new Matrix3f();
	Matrix3f coordSysTransMX = new Matrix3f();
	/** 
	 * The transform that corresponds to this coordinate systems {@link #coordinateArea}
	 * = {@link #coordSysScaleMX} * {@link #coordSysTransMX}
	 * which is passed to the content renderers
	 */
	Matrix3f coordSysViewMX = new Matrix3f();

	String xAxisLabel = null;
	String yAxisLabel = null;

	public CoordSysCanvas(GLData data) {
		super(data);
		this.fboClearColor = Color.WHITE;
		xticklocations = new double[5][1];
		yticklocations = new double[5][1];
		axes.addSegment(coordsysframeLB, coordsysframeRB, Color.BLACK);
		axes.addSegment(coordsysframeLB, coordsysframeLT, Color.BLACK);
		axes.addSegment(coordsysframeLT, coordsysframeRT, Color.GRAY);
		axes.addSegment(coordsysframeRB, coordsysframeRT, Color.GRAY);
		axes.setThickness(2);
		linesR.addItemToRender(guides);
		linesR.addItemToRender(ticks);
		linesR.addItemToRender(axes);
	}

	public CoordSysCanvas() {
		this(new GLData());
	}
	
	public void setDirty() {
		this.isDirty = true;
	}

	public void setContent(Renderer content) {
		this.content = content;
		setDirty();
	}

	protected void setupTicksGuidesAndLabels() {
		Pair<double[],String[]> xticksAndLabels = tickMarkGenerator.genTicksAndLabels(
				coordinateArea.getMinX(), 
				coordinateArea.getMaxX(), 
				5, 
				false);
		Pair<double[],String[]> yticksAndLabels = tickMarkGenerator.genTicksAndLabels(
				coordinateArea.getMinY(), 
				coordinateArea.getMaxY(), 
				5, 
				true);
		this.xticks = xticksAndLabels.first;
		this.yticks = yticksAndLabels.first;
		String[] xticklabels = xticksAndLabels.second;
		String[] yticklabels = yticksAndLabels.second;
		
		final int tickfontSize = 10;
		final int labelfontSize = 12;
		final int style = Font.PLAIN;
		final boolean antialiased = true;
		// find maximum length of y axis labels
		int maxYTickLabelWidth = 0;
		for(String label:yticklabels){
			int labelW = CharacterAtlas.boundsForText(label.length(), tickfontSize, style, antialiased).getBounds().width;
			maxYTickLabelWidth = Math.max(maxYTickLabelWidth, labelW);
		}
		int maxXTickLabelHeight = CharacterAtlas.boundsForText(1, tickfontSize, style, antialiased).getBounds().height;
		int maxLabelHeight = CharacterAtlas.boundsForText(1, labelfontSize, style, antialiased).getBounds().height;
		// move coordwindow origin so that labels have enough display space
		coordWindowOrigin.x[0] = maxYTickLabelWidth + leftPadding + 7;
		coordWindowOrigin.y[0] = maxXTickLabelHeight + botPadding + 6;
		// move opposing corner of coordwindow to have enough display space
		coordsysframeRT.x[0] = getWidth()-rightPadding-maxLabelHeight-4;
		coordsysframeRT.y[0] = getHeight()-topPadding-maxLabelHeight-4;
		
		ticks.removeAllSegments();
		textR.deleteAllItems();
		guides.removeAllSegments();
		double xAxisWidth = coordsysframeLB.distance(coordsysframeRB);
		double yAxisHeight = coordsysframeLB.distance(coordsysframeLT);
		// xaxis ticks
		for(int i=0; i<xticks.length; i++){
			// tick
			double m = (xticks[i]-coordinateArea.getMinX())/coordinateArea.getWidth();
			double x = coordWindowOrigin.getX()+m*xAxisWidth;
			Point2D onaxis = new Point2D.Double(x,coordWindowOrigin.getY());
			ticks.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0,-4), tickColor);
			// label
			Text label = new Text(xticklabels[i], tickfontSize, style, antialiased);
			Dimension textSize = label.getTextSize();
			label.setOrigin(
					(int)(onaxis.getX()-textSize.getWidth()/2.0), 
					(int)(onaxis.getY()-6-textSize.getHeight()));
			textR.addItemToRender(label);
			// guide
			guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, 0, yAxisHeight), guideColor);
		}
		// yaxis ticks
		for(int i=0; i<yticks.length; i++){
			// tick
			double m = (yticks[i]-coordinateArea.getMinY())/coordinateArea.getHeight();
			Point2D onaxis = new TranslatedPoint2D(coordsysframeLB, 0, m*yAxisHeight);
			ticks.addSegment(onaxis, new TranslatedPoint2D(onaxis, -4, 0), tickColor);
			// label
			Text label = new Text(yticklabels[i], tickfontSize, style, antialiased);
			Dimension textSize = label.getTextSize();
			label.setOrigin(new TranslatedPoint2D(onaxis, -7-textSize.getWidth(), -textSize.getHeight()/2.0));
			textR.addItemToRender(label);
			// guide
			guides.addSegment(onaxis, new TranslatedPoint2D(coordsysframeRB, 0, m*yAxisHeight), guideColor);
		}
		// axis labels
		Text labelX = new Text(getxAxisLabel(), labelfontSize, style, antialiased);
		Text labelY = new Text(getyAxisLabel(), labelfontSize, style, antialiased);
		labelY.setAngle(-(float)Math.PI/2);
		labelX.setOrigin(new TranslatedPoint2D(coordsysframeLT, xAxisWidth/2 - labelX.getTextSize().width/2, 4));
		labelY.setOrigin(new TranslatedPoint2D(coordsysframeRB, 4, yAxisHeight/2 - labelY.getTextSize().width/2));
		textR.addItemToRender(labelX);
		textR.addItemToRender(labelY);
	}
	
	public String getxAxisLabel() {
		return xAxisLabel == null ? "X":xAxisLabel;
	}
	
	public String getyAxisLabel() {
		return yAxisLabel == null ? "Y":yAxisLabel;
	}
	
	public void setxAxisLabel(String xAxisLabel) {
		this.xAxisLabel = xAxisLabel;
		setDirty();
	}
	
	public void setyAxisLabel(String yAxisLabel) {
		this.yAxisLabel = yAxisLabel;
		setDirty();
	}

	
	public TickMarkGenerator getTickMarkGenerator() {
		return tickMarkGenerator;
	}
	
	public void setTickMarkGenerator(TickMarkGenerator tickMarkGenerator) {
		this.tickMarkGenerator = tickMarkGenerator;
		setDirty();
	}

	@Override
	public void initGL() {
		super.initGL();
		linesR.glInit();
		textR.glInit();
		if(content != null)
			content.glInit();
	}

	@Override
	public void paintToFBO(int w, int h) {
		if(isDirty || viewportwidth != w || viewportheight != h){
			// update axes
			coordsysframeRT.setLocation(w-50, h-50);
			axes.setDirty();
			setupTicksGuidesAndLabels();
			viewportwidth = w;
			viewportheight = h;
			isDirty = false;
		}
		linesR.addItemToRender(guides);
		linesR.addItemToRender(ticks);
		linesR.removeItemToRender(axes);
		linesR.render(w, h);
		// draw into the coord system
		if(content != null){
			content.glInit();
			int viewportX = (int)coordsysframeLB.getX();
			int viewPortY = (int)coordsysframeLB.getY();
			int viewPortW = (int)coordsysframeLB.distance(coordsysframeRB);
			int viewPortH = (int)coordsysframeLB.distance(coordsysframeLT);
			GL11.glViewport(viewportX,viewPortY,viewPortW,viewPortH);
			if(content instanceof AdaptableView){
				double scaleX = viewPortW/coordinateArea.getWidth();
				double scaleY = viewPortH/coordinateArea.getHeight();
				coordSysScaleMX.set((float)scaleX,0,0,  0,(float)scaleY,0,  0,0,1);
				coordSysTransMX.set(1,0,0,  0,1,0, -(float)coordinateArea.getX(),-(float)coordinateArea.getY(),1);
				((AdaptableView) content).setViewMX(coordSysScaleMX.mul(coordSysTransMX,coordSysViewMX), coordSysScaleMX, coordSysTransMX);
			}
			content.render(viewPortW, viewPortH);
			GL11.glViewport(0, 0, w, h);
		}
		linesR.removeItemToRender(guides);
		linesR.removeItemToRender(ticks);
		linesR.addItemToRender(axes);
		linesR.render(w, h);
		textR.render(w, h);
	}
	
	public void setCoordinateArea(double minX, double minY, double maxX, double maxY){
		if(maxX-minX < 1e-9 || maxY-minY < 1e-9){
			System.err.printf("hitting coordinate area precision limit, x-range:%e, y-range:%e%n", maxX-minX, maxY-minY);
			return;
		}
		this.coordinateArea = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);
		setDirty();
	}
	
	public Rectangle2D getCoordinateArea() {
		return coordinateArea;
	}
	
	public Rectangle2D getCoordSysFrame() {
		return new Rectangle2D.Double(
				coordsysframeLB.getX(), 
				coordsysframeLB.getY(), 
				coordsysframeLB.distance(coordsysframeRB), 
				coordsysframeLB.distance(coordsysframeLT)
		);
	}


	@Override
	public void close() {
		if(Objects.nonNull(textR))
			textR.close();
		if(Objects.nonNull(linesR))
			linesR.close();
		if(Objects.nonNull(content))
			content.close();
		textR = null;
		linesR = null;
		content = null;
		super.close();
	}

}
