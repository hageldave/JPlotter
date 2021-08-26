package hageldave.jplotter;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;

public class PrecisionTest {

	public static void main(String[] args) {
		JFrame frame = Boilerplate.createJFrameWithBoilerPlate("Precision Test");
		JPlotterCanvas canvas = new BlankCanvas();
		frame.getContentPane().add(canvas.asComponent());
		canvas.addCleanupOnWindowClosingListener(frame);
		
		CoordSysRenderer csr = new CoordSysRenderer();
		canvas.setRenderer(csr);
		CompleteRenderer content = new CompleteRenderer();
		csr.setContent(content);
		content.setGLDoublePrecisionEnabled(true);
		
		new CoordSysScrollZoom(canvas, csr).register();
		
		Points p = new Points(DefaultGlyph.CIRCLE);
		Lines l = new Lines();
		Curves c = new Curves();
		Triangles t = new Triangles();
		
		content.addItemToRender(t).addItemToRender(c).addItemToRender(l).addItemToRender(p);
		
		// create spiral going inwards
		final double cx=1,cy=1;
		double radius = 1;
		double rad = 0;
		final double pi2 = Math.PI*2;
		ArrayList<Point2D> points = new ArrayList<>();
		while(radius > 1e-16) {
			double x = Math.cos(rad)*radius + cx;
			double y = Math.sin(rad)*radius + cy;
			points.add(new Point2D.Double(x, y));
			rad = (rad + 0.8)%pi2;
			radius *= 0.8;
		}
		Point2D[] pointarray = points.toArray(Point2D[]::new);
		points=null;
		System.gc();
		
		l.addLineStrip(pointarray);
		c.addCurvesThrough(pointarray);
		for(int i=0; i<pointarray.length; i++) {
			p.addPoint(pointarray[i]);
		}
		for(int i=0; i<pointarray.length-1; i++) {
			Point2D p1 = pointarray[i];
			Point2D p2 = pointarray[i+1];
			double dx = p2.getX()-p1.getX();
			double dy = p2.getY()-p1.getY();
			double x = p1.getX()+dx*0.5;
			double y = p1.getY()+dy*0.5;
			x -= 0.5*dy;
			y += 0.5*dx;
			t.addTriangle(p1, p2, new Point2D.Double(x, y));
		}
		
		csr.setCoordinateView(cx-1, cy-1, cx+1, cy+1);
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		
	}
	
}
