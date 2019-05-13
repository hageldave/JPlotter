package jplotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import org.joml.Matrix3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.awt.GLData;

import jplotter.globjects.DefaultGlyph;
import jplotter.globjects.Lines;
import jplotter.globjects.Points;
import jplotter.globjects.StaticText;
import jplotter.renderers.LinesRenderer;
import jplotter.renderers.PointsRenderer;
import jplotter.renderers.TextRenderer;
import jplotter.util.ExtendedWilkinson;
import jplotter.util.PointeredPoint2D;
import jplotter.util.TranslatedPoint2D;
import jplotter.util.Utils;

public class CoordSysCanvas extends FBOCanvas {
	private static final long serialVersionUID = 1L;

	LinesRenderer linesR = new LinesRenderer();
	TextRenderer textR = new TextRenderer();
	Rectangle2D coordinateArea;
	PointeredPoint2D coordWindowOrigin = new PointeredPoint2D(100, 50);

	LinesRenderer content1 = new LinesRenderer();
	PointsRenderer content2 = new PointsRenderer();

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
	Color guideColor = Color.LIGHT_GRAY;

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


	public CoordSysCanvas(GLData data) {
		super(data);
		this.fboClearColor = Color.WHITE;
		coordinateArea = new Rectangle2D.Double(-1,-1,10,3);
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
		setupTestContent();
	}
	
	public CoordSysCanvas() {
		this(new GLData());
	}

	void setupTestContent(){
		Lines testcontent = new Lines();
		double scaling = 0.1;
		for(int i = 0; i < 100; i++){
			double x1 = i*scaling;
			double x2 = (i+1)*scaling;
			double y1 = Math.sin(x1);
			double y2 = Math.sin(x2);
			testcontent.addSegment(x1, y1, x2, y2, 0xffff00ff);
			testcontent.addSegment(i, i, i+1, i+1, 0xff00ff00);
		}
		testcontent.setThickness(2f);
		testcontent.setPickColor(0xffbabe);
		content1.addItemToRender(testcontent);
		
		Points circlepoints = new Points(DefaultGlyph.CIRCLE_F);
		Points squarepoints = new Points(DefaultGlyph.SQUARE_F);
		Color color1 = new Color(0xffe41a1c);
		Color color2 = new Color(0xff377eb8);
		for(int i = 0; i < 100; i++){
			circlepoints.addPoint(Math.random(), Math.random(), color1);
			squarepoints.addPoint(Math.random(), Math.random(), color2);
		}
		content2.addItemToRender(circlepoints).addItemToRender(squarepoints);
	}

	protected void setupTicksAndGuides() {
		this.xticks = ExtendedWilkinson.getTicks(coordinateArea.getMinX(), coordinateArea.getMaxX(), 5);
		this.yticks = ExtendedWilkinson.getTicks(coordinateArea.getMinY(), coordinateArea.getMaxY(), 5);
		String[] xticklabels = labelsForTicks(xticks);
		String[] yticklabels = labelsForTicks(yticks);
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
			StaticText label = new StaticText(xticklabels[i], 10, Font.PLAIN, true);
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
			double y = coordWindowOrigin.getY()+m*yAxisHeight;
			Point2D onaxis = new Point2D.Double(coordWindowOrigin.getX(),y);
			ticks.addSegment(onaxis, new TranslatedPoint2D(onaxis, -4,0), tickColor);
			// label
			StaticText label = new StaticText(yticklabels[i], 10, Font.PLAIN, true);
			Dimension textSize = label.getTextSize();
			label.setOrigin(
					(int)(onaxis.getX()-7-textSize.getWidth()), 
					(int)(onaxis.getY()-textSize.getHeight()/2.0));
			textR.addItemToRender(label);
			// guide
			guides.addSegment(onaxis, new TranslatedPoint2D(onaxis, xAxisWidth, 0), guideColor);
		}

	}


	protected String[] labelsForTicks(double[] ticks){
		String str1 = String.format(Locale.US, "%.4g", ticks[0]);
		String str2 = String.format(Locale.US, "%.4g", ticks[ticks.length-1]);
		String[] labels = new String[ticks.length];
		if(str1.contains("e") || str2.contains("e")){
			for(int i=0; i<ticks.length; i++){
				String l = String.format(Locale.US, "%.4e", ticks[i]);
				String[] Esplit = l.split("e", -2);
				String[] dotsplit = Esplit[0].split("\\.",-2);
				dotsplit[1] = ('#'+dotsplit[1])
						.replaceAll("0", " ")
						.trim()
						.replaceAll(" ", "0")
						.replaceAll("#", "");
				dotsplit[1] = dotsplit[1].isEmpty() ? "0":dotsplit[1];
				l = dotsplit[0]+'.'+dotsplit[1]+'e'+Esplit[1];
				labels[i] = l;
			}
		} else {
			for(int i=0; i<ticks.length; i++){
				String l = String.format(Locale.US, "%.4f", ticks[i]);
				if(l.contains(".")){
					String[] dotsplit = l.split("\\.",-2);
					dotsplit[1] = ('#'+dotsplit[1])
							.replaceAll("0", " ")
							.trim()
							.replaceAll(" ", "0")
							.replaceAll("#", "");
					if(dotsplit[1].isEmpty()){
						l = dotsplit[0];
					} else {
						l = dotsplit[0]+'.'+dotsplit[1];
					}
				}
				labels[i] = l;
			}
		}
		return labels;
	}


	@Override
	public void initGL() {
		super.initGL();
		linesR.glInit();
		textR.glInit();
		content1.glInit();
		content2.glInit();
	}

	@Override
	public void paintToFBO(int w, int h) {
		if(isDirty || viewportwidth != w || viewportheight != h){
			// update axes
			coordsysframeRT.setLocation(w-50, h-50);
			axes.setDirty();
			setupTicksAndGuides();
		}
		linesR.addItemToRender(guides);
		linesR.addItemToRender(ticks);
		linesR.removeItemToRender(axes);
		linesR.render(w, h);
		// draw into the coord system
		{
			int viewportX = (int)coordsysframeLB.getX();
			int viewPortY = (int)coordsysframeLB.getY();
			int viewPortW = (int)coordsysframeLB.distance(coordsysframeRB);
			int viewPortH = (int)coordsysframeLB.distance(coordsysframeLT);
			GL11.glViewport(viewportX,viewPortY,viewPortW,viewPortH);
			double scaleX = viewPortW/coordinateArea.getWidth();
			double scaleY = viewPortH/coordinateArea.getHeight();
			coordSysScaleMX.set((float)scaleX,0,0,  0,(float)scaleY,0,  0,0,1);
			coordSysTransMX.set(1,0,0,  0,1,0, -(float)coordinateArea.getX(),-(float)coordinateArea.getY(),1);
			content1.setViewMX(coordSysScaleMX.mul(coordSysTransMX,coordSysViewMX), coordSysScaleMX, coordSysTransMX);
			content1.render(viewPortW, viewPortH);
			content2.setViewMX(coordSysViewMX, coordSysScaleMX, coordSysTransMX);
			content2.render(viewPortW, viewPortH);
			GL11.glViewport(0, 0, w, h);
		}
		linesR.removeItemToRender(guides);
		linesR.removeItemToRender(ticks);
		linesR.addItemToRender(axes);
		linesR.render(w, h);
		textR.render(w, h);
	}


	@Override
	public void close() {
		textR.close();
		linesR.close();
		content1.close();
		content2.close();
		super.close();
	}

}
