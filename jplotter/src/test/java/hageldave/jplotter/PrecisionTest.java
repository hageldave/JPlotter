package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.util.Utils;

public class PrecisionTest {

	public static void main(String[] args) {
		JFrame frame = Boilerplate.createJFrameWithBoilerPlate("Precision Test");
		BlankCanvas canvas = new BlankCanvas();
		frame.getContentPane().add(canvas.asComponent());
		canvas.addCleanupOnWindowClosingListener(frame);
		
		CoordSysRenderer csr = new CoordSysRenderer();
		canvas.setRenderer(csr);
		CompleteRenderer content = new CompleteRenderer();
		csr.setContent(content);
		new CoordSysScrollZoom(canvas, csr).register();
		
		JCheckBox cbxDoublePrecision = new JCheckBox("GL double precision");
		cbxDoublePrecision.addChangeListener(e->{
			content.setGLDoublePrecisionEnabled(cbxDoublePrecision.isSelected());
			canvas.scheduleRepaint();
		});
		content.setGLDoublePrecisionEnabled(cbxDoublePrecision.isSelected());
		frame.getContentPane().add(cbxDoublePrecision, BorderLayout.NORTH);
		
		Points p = new Points(DefaultGlyph.CIRCLE);
		Lines l = new Lines();
		Curves c = new Curves();
		Triangles t = new Triangles();
		
		content
		.addItemToRender(t)
		.addItemToRender(c)
		.addItemToRender(l)
		.addItemToRender(p);
		
		// create spiral going inwards
		final double cx=1e-4,cy=1e-4;
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
		for(int i=0; i < pointarray.length; i+=1) {
			Point2D point = pointarray[i];
			double dx = point.getX()-cx;
			double dy = point.getY()-cy;
			double atan2 = Math.atan2(dy, dx);
			if(atan2 < 1.3 && atan2 > 0.5) {
				Text label = new Text(String.format("r=%e", Utils.hypot(dx, dy)), 10, Font.PLAIN);
				label.setOrigin(new Point2D.Double(cx+1.1*dx,cy+1.1*dy));
				content.addItemToRender(label);
			}
		}
		
		csr.setCoordinateView(cx-1, cy-1, cx+1, cy+1);
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		
	}
	
}
