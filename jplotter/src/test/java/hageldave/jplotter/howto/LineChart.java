package hageldave.jplotter.howto;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.CoordSysCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;

public class LineChart {

	private static double[] randomData(int n){
		double[] d = new double[n];
		for(int i=0; i<n; i++){
			d[i]=Math.random()*2-1;
		}
		return d;
	}

	public static void main(String[] args) {
		// obtain data series
		double[] seriesA_y = randomData(20);
		double[] seriesA_x = IntStream.range(0, 20).mapToDouble(i->i/19.0).toArray();
		double[] seriesB_y = randomData(30);
		double[] seriesB_x = randomData(30);
		Arrays.sort(seriesB_x);
		// create Lines objects, one solid the other dashed
		Lines lineA = new Lines();
		Lines lineB = new Lines();
		lineB.setStrokePattern(0xf0f0);
		// add line segments to A
		for(int i=0; i<seriesA_y.length-1; i++){
			double x1=seriesA_x[i], x2=seriesA_x[i+1];
			double y1=seriesA_y[i], y2=seriesA_y[i+1];
			SegmentDetails segment = lineA.addSegment(x1, y1, x2, y2);
			segment.setColor(Color.RED);
		}
		// add line segments to B (the short way)
		ArrayList<SegmentDetails> segmentsB = lineB.addLineStrip(seriesB_x, seriesB_y);
		segmentsB.forEach(seg->seg.setColor(Color.BLUE));
		// use a coordinate system for display
		CoordSysCanvas canvas = new CoordSysCanvas();
		canvas.setPreferredSize(new Dimension(700, 400));
		canvas.setBackground(Color.WHITE);
		canvas.setCoordinateView(-1,-1,1,1);
		// set the content renderer of the coordinate system 
		// we want to render Lines objects
		LinesRenderer content = new LinesRenderer();
		content.addItemToRender(lineA).addItemToRender(lineB);
		canvas.setContent(content);
		// display within a JFrame
		JFrame frame = new JFrame();
		frame.getContentPane().add(canvas);
		frame.setTitle("scatterplot");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// code to clean up opengl resources
				canvas.runInContext(()->canvas.close());
			}
		});
		// make visible on AWT event dispatch thread
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		// (optional) area under curve with transparency
		Triangles areaA = new Triangles();
		areaA.setGlobalAlphaMultiplier(0.2);
		// add area quads under each segment
		for(SegmentDetails segment: lineA.getSegments()){
			Point2D pL=segment.p0, pR=segment.p1;
			if(Math.signum(pL.getY()) == Math.signum(pR.getY())){
				// points are on same side of x-axis
				ArrayList<TriangleDetails> quad = areaA.addQuad(
						pL.getX(), 0, pL.getX(), pL.getY(), 
						pR.getX(), pR.getY(), pR.getX(), 0);
				quad.forEach(tri->tri.setColor(Color.RED));
			} else {
				// segment intersects x-axis, need to find intersection
				double x0=pL.getX(),y0=pL.getY(), x2=pR.getX(), y2=pR.getY();
				double m = (y2-y0)/(x2-x0);
				// solving for x1 in (x1-x0)*m+y0 = 0 --> 1x = x0-y0/m;
				double x1 = x0-y0/m;
				areaA.addTriangle(x0, y0, x1, 0, x0, 0).setColor(Color.RED);
				areaA.addTriangle(x2, y2, x1, 0, x2, 0).setColor(Color.RED);
			}
		}
		TrianglesRenderer areaContent = new TrianglesRenderer();
		areaContent.addItemToRender(areaA);
		canvas.setContent(areaContent.withAppended(content));
		

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
