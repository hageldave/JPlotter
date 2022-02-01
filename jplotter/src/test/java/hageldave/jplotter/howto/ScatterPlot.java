package hageldave.jplotter.howto;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.interaction.kml.CoordSysScrollZoom;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.PointsRenderer;

import javax.swing.*;
import java.awt.*;

public class ScatterPlot {
	
	private static double[][] randomData(int n){
		double[][] d = new double[n][3];
		for(int i=0; i<n; i++){
			d[i][0]=Math.random()*2-1;
			d[i][1]=Math.random()*2-1;
			d[i][2]=(d[i][1]+1)/2;
		}
		return d;
	}

	public static void main(String[] args) {
		// generate or get data
		double[][] dataA = randomData(50);
		double[][] dataB = randomData(100);
		// create Points objects that display as filled circles and crosses
		Points pointsA = new Points(DefaultGlyph.CIRCLE_F);
		Points pointsB = new Points(DefaultGlyph.CROSS);
		// add the individual coordinates as points
		for(double[] entry: dataA){
			double x=entry[0], y=entry[1], heat=entry[2];
			PointDetails point = pointsA.addPoint(x,y);
			// set point's color
			int argb = DefaultColorMap.D_COOL_WARM.interpolate(heat);
			point.setColor(argb);
		}
		for(double[] entry: dataB){
			pointsB.addPoint(entry[0],entry[1]).setColor(Color.GRAY);
		}
		// use a coordinate system for display
		CoordSysRenderer coordsys = new CoordSysRenderer();
		coordsys.setCoordinateView(-1,-1,1,1);
		// set the content renderer of the coordinate system 
		// we want to render Points objects
		PointsRenderer content = new PointsRenderer();
		content.addItemToRender(pointsB).addItemToRender(pointsA);
		coordsys.setContent(content);

		// display within a JFrame
		JFrame frame = new JFrame();
		boolean useOpenGL = true;
		JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
		canvas.setRenderer(coordsys);
		canvas.asComponent().setPreferredSize(new Dimension(400, 400));
		canvas.asComponent().setBackground(Color.WHITE);
		frame.getContentPane().add(canvas.asComponent());
		frame.setTitle("scatterplot");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		canvas.addCleanupOnWindowClosingListener(frame);
		// make visible on AWT event dispatch thread
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		// use mouse focused zoom
		new CoordSysScrollZoom(canvas, coordsys, true).register();

		long t=System.currentTimeMillis()+2000;
		while(t>System.currentTimeMillis());
		if("false".equals("true"))
		SwingUtilities.invokeLater(()->{
			Img img = new Img(frame.getSize());
			img.paint(g2d->frame.paintAll(g2d));
			ImageSaver.saveImage(img.getRemoteBufferedImage(), "scatterplot.png");
		});
	}
	
}
