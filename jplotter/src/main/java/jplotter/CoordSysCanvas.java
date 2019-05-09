package jplotter;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import jplotter.globjects.Lines;
import jplotter.renderers.LinesRenderer;
import jplotter.renderers.TextRenderer;
import jplotter.util.PointeredPoint2D;
import jplotter.util.TranslatedPoint2D;
import jplotter.util.Utils;

public class CoordSysCanvas extends FBOCanvas {
	private static final long serialVersionUID = 1L;
	
	LinesRenderer linesR = new LinesRenderer();
	TextRenderer textR = new TextRenderer();
	Rectangle2D coordinateArea;
	PointeredPoint2D coordWindowOrigin = new PointeredPoint2D(50, 50);
	
	Lines axes = new Lines();
	Lines ticks = new Lines();
	
	double[] xticks;
	double[] yticks;
	double[][] xticklocations;
	double[][] yticklocations;
	
	int viewportwidth=0;
	int viewportheight=0;
	boolean isDirty = true;
	
	PointeredPoint2D coordsysframeLB = coordWindowOrigin;
	PointeredPoint2D coordsysframeRT = Utils.copy(coordWindowOrigin);
	PointeredPoint2D coordsysframeLT = new PointeredPoint2D(coordsysframeLB.x, coordsysframeRT.y);
	PointeredPoint2D coordsysframeRB = new PointeredPoint2D(coordsysframeRT.x, coordsysframeLB.y);
	
	
	public CoordSysCanvas() {
		this.fboClearColor = Color.WHITE;
		coordinateArea = new Rectangle2D.Double(20, 30, 200, 150);
		xticks = new double[5];
		yticks = new double[5];
		xticklocations = new double[5][1];
		yticklocations = new double[5][1];
		for(int i = 0; i < 5; i++){
			xticks[i] = coordinateArea.getX() + coordinateArea.getWidth()*i/4;
			yticks[i] = coordinateArea.getY() + coordinateArea.getHeight()*i/4;
		}
		
		axes.addSegment(coordsysframeLB, coordsysframeRB, Color.BLACK);
		axes.addSegment(coordsysframeLB, coordsysframeLT, Color.BLACK);
		axes.addSegment(coordsysframeLT, coordsysframeRT, Color.GRAY);
		axes.addSegment(coordsysframeRB, coordsysframeRT, Color.GRAY);
		axes.setThickness(2);
		linesR.addItemToRender(axes);
		
		for(int i = 0; i < 5; i++){
			PointeredPoint2D onXaxis = new PointeredPoint2D(xticklocations[i], coordsysframeLB.y);
			ticks.addSegment(onXaxis, new TranslatedPoint2D(onXaxis, 0, -5), Color.gray);
			PointeredPoint2D onYaxis = new PointeredPoint2D(coordsysframeLB.x, yticklocations[i]);
			ticks.addSegment(onYaxis, new TranslatedPoint2D(onYaxis, -5, 0), Color.gray);
		}
		linesR.addItemToRender(ticks);
	}
	
	protected void deduceTicks() {
		double minX = coordinateArea.getMinX();
		double maxX = coordinateArea.getMaxX();
		minX = 3.3;
		maxX = 4.2;
		double diff = maxX-minX;
		double orderOfMagnitude = Math.floor(Math.log10(diff));
		double[] goodIncrements = new double[]{1, 5, 2, 2.5, 4, 3, 1.5, 6, 8};
		int numtickstoplace = 5;
		
		
		for(int i = 0; i < goodIncrements.length; i++){
			
		}
	}
	
	
	@Override
	public void initGL() {
		super.initGL();
		linesR.glInit();
		textR.glInit();
	}
	
	@Override
	public void paintToFBO(int w, int h) {
		if(isDirty || viewportwidth != w || viewportheight != h){
			// update axes
			coordsysframeRT.setLocation(w-50, h-50);
			axes.setDirty();
			for(int i=0; i<5; i++){
				xticklocations[i][0] = 50+i*(w-100f)/4;
				yticklocations[i][0] = 50+i*(h-100f)/4;
			}
			ticks.setDirty();
		}
		linesR.render(w, h);
	}
	
	
	@Override
	public void close() {
		textR.close();
		linesR.close();
		super.close();
	}

	
	public static void main(String[] args) {
		double start =  Math.random() * Math.pow(10, (Math.random()*8)-3);
		double range =  Math.random() * Math.pow(10, (Math.random()*8)-3);
		start = 0.1582;
		range = 0.0447;
		double end = start+range;
		double oom = Math.log10(range);
		int oomi = (int)Math.floor(oom);
		int starti = (int)Math.floor(start * Math.pow(10, -oomi));
		int endi = (int)Math.ceil(end * Math.pow(10, -oomi));
		System.out.println(oomi);
	}
	
}
