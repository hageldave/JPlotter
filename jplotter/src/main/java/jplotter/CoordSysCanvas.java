package jplotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;

import jplotter.globjects.Lines;
import jplotter.globjects.StaticText;
import jplotter.renderers.LinesRenderer;
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


	public CoordSysCanvas() {
		this.fboClearColor = Color.WHITE;
		coordinateArea = new Rectangle2D.Double(20.5, 500, 0.9, 10000);
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
	}

	@Override
	public void paintToFBO(int w, int h) {
		if(isDirty || viewportwidth != w || viewportheight != h){
			// update axes
			coordsysframeRT.setLocation(w-50, h-50);
			axes.setDirty();
			setupTicksAndGuides();
		}
		linesR.render(w, h);
		textR.render(w, h);
	}


	@Override
	public void close() {
		textR.close();
		linesR.close();
		super.close();
	}

}
