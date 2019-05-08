package jplotter.renderers;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import jplotter.globjects.Lines;
import jplotter.util.PointeredPoint2D;
import jplotter.util.Utils;

public class CoordSysRenderer implements Renderer {

	LinesRenderer linesR = new LinesRenderer();
	TextRenderer textR = new TextRenderer();
	Rectangle2D coordinateArea;
	PointeredPoint2D coordWindowOrigin = new PointeredPoint2D(50, 50);
	
	Lines axes = new Lines();
	Lines ticks = new Lines();
	
	double[] xticks;
	double[] yticks;
	
	int viewportwidth=0;
	int viewportheight=0;
	boolean isDirty = true;
	
	PointeredPoint2D coordsysframeLB = coordWindowOrigin;
	PointeredPoint2D coordsysframeRT = Utils.copy(coordWindowOrigin);
	PointeredPoint2D coordsysframeLT = new PointeredPoint2D(coordsysframeLB.x, coordsysframeRT.y);
	PointeredPoint2D coordsysframeRB = new PointeredPoint2D(coordsysframeRT.x, coordsysframeLB.y);
	
	
	public CoordSysRenderer() {
		coordinateArea = new Rectangle2D.Double(20, 30, 200, 150);
		xticks = new double[5];
		yticks = new double[5];
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
	}


	@Override
	public void glInit() {
		linesR.glInit();
		textR.glInit();
	}


	@Override
	public void render(int w, int h) {
		if(isDirty || viewportwidth != w || viewportheight != h){
			// update axes
			coordsysframeRT.setLocation(w-50, h-50);
			axes.setDirty();
		}
		linesR.render(w, h);
	}


	@Override
	public void close() {
		linesR.close();
		textR.close();
	}
	
}
