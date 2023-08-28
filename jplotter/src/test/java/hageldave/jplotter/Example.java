package hageldave.jplotter;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.ExportUtil;

import javax.swing.*;
import java.awt.*;
import java.util.function.DoubleUnaryOperator;

public class Example {

	@SuppressWarnings("resource" /* compiler is too dumb to realize there is no leak */)
	public static void main(String[] args) {
		// lets prepare some data we want to visualize
		DoubleUnaryOperator fx = Math::sin;
		// lets sample the sine function so we can later plot a line
		int numCurveSamples = 100;
		double[] curveX = new double[numCurveSamples];
		double[] curveY = new double[numCurveSamples];
		for(int i=0; i<numCurveSamples; i++){
			double x = i*Math.PI*2/numCurveSamples;
			double y = fx.applyAsDouble(x);
			curveX[i]=x;  curveY[i]=y;
		}
		// also lets make some random (x,y) samples that we can compare against the function
		int numPointSamples = 400;
		double[] pointsX = new double[numPointSamples];
		double[] pointsY = new double[numPointSamples];
		double[] diffToCurve = new double[numPointSamples];
		for(int i=0; i<numPointSamples; i++){
			double x = Math.random()*Math.PI*2;
			double y = Math.random()*Math.PI*2-Math.PI;
			diffToCurve[i] = y-fx.applyAsDouble(x);
			pointsX[i]=x;  pointsY[i]=y;
		}
		
		// alright this should be enough data for now
		// see how we want to visualize the data
		
		// the sine samples should be visualized as a line in a kind of red color
		Lines sineLine = new Lines();
		int sineColor = 0xff66c2a5;
		sineLine.setGlobalThicknessMultiplier(2)
			.setStrokePattern(0xf790)
			.addLineStrip(curveX, curveY)
			.forEach(segment -> segment.setColor(sineColor));
		
		// the random samples should be visualized as points, but lets make 3 classes
		// class 1: y(x) < sin(x)-0.5  
		// class 2: sin(x)-0.5 <= y(x) <= sin(x)+0.5
		// class 3: sin(x)+0.5 < y(x)
		Points pointsC1 = new Points(DefaultGlyph.CROSS);
		Points pointsC2 = new Points(DefaultGlyph.CIRCLE);
		Points pointsC3 = new Points(DefaultGlyph.CROSS);
		int c1Color = 0xff8da0cb, c2Color = sineColor, c3Color = 0xfffc8d62;
		for(int i=0; i<numPointSamples; i++){
			if(diffToCurve[i] < -0.5){
				pointsC1.addPoint(pointsX[i], pointsY[i]).setColor(c1Color);
			} else if(diffToCurve[i] > 0.5) {
				pointsC3.addPoint(pointsX[i], pointsY[i]).setColor(c3Color);
			} else {
				pointsC2.addPoint(pointsX[i], pointsY[i]).setColor(c2Color);
			}
		}
		
		// okay we're good to go, lets display the data in a coordinate system
		CoordSysRenderer coordsys = new CoordSysRenderer();
		CompleteRenderer content = new CompleteRenderer();
		coordsys.setContent( content
				.addItemToRender(sineLine)
				.addItemToRender(pointsC1)
				.addItemToRender(pointsC2)
				.addItemToRender(pointsC3));
		// lets set the coordinate view to cover the whole sampling space
		coordsys.setCoordinateView(-.5, -3.3, 6.5, 3.3);
		
		// lets add a legend so the viewer can make sense of the data
		Legend legend = new Legend();
		coordsys.setLegendRightWidth(80);
		coordsys.setLegendRight(legend
				.addLineLabel(2, sineColor, "f(x)")
				.addGlyphLabel(DefaultGlyph.CROSS, c1Color, "< f(x)-0.5")
				.addGlyphLabel(DefaultGlyph.CIRCLE, c2Color, "~ f(x)")
				.addGlyphLabel(DefaultGlyph.CROSS, c3Color, "> f(x)+0.5"));
		
		// display the coordinate system on a blank canvas
		boolean useOpenGL = true;
		JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
		canvas.setRenderer(coordsys);
		// lets add some controls for exploring the data
		new CoordSysScrollZoom(canvas,coordsys).setZoomFactor(1.7).register();
		new CoordSysViewSelector(canvas,coordsys) {
			{extModifierMask=0;/* no need for shift to be pressed */}
			@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
				coordsys.setCoordinateView(minX, minY, maxX, maxY);
			}
		}.register();
		
		// lets put a JFrame around it all and launch
		JFrame frame = new JFrame("Example Viz");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(canvas.asComponent());
		canvas.asComponent().setPreferredSize(new Dimension(480, 400));
		canvas.asComponent().setBackground(Color.white);
		
		canvas.addCleanupOnWindowClosingListener(frame);
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		frame.setJMenuBar(ExportUtil.createSaveMenu(frame, "example_export"));
	}
}
