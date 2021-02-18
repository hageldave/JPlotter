package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.renderers.PointsRenderer;
import hageldave.jplotter.renderers.TextRenderer;

public class Viz {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new JPanel(new BorderLayout()));
		frame.getContentPane().setBackground(Color.white);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));
		
		BlankCanvasFallback canvas = new BlankCanvasFallback();
//		BlankCanvas canvas = new BlankCanvas();
		LinesRenderer render = new LinesRenderer();
		Lines lines = new Lines().setGlobalThicknessMultiplier(2).setStrokePattern(0xf0f0);
		lines.addLineStrip(-.5,-.1,.5,0,1,.1).forEach(seg->seg.setColor(0xff00ff00));
		render.addItemToRender(lines);
		
		TextRenderer txtrender = new TextRenderer();
		Text txt = new Text("hellogy", 12, Font.PLAIN).setOrigin(10, 10);
		txtrender.addItemToRender(txt);
		
		PointsRenderer prender = new PointsRenderer();
		Points points = new Points(DefaultGlyph.CIRCLE_F);
		prender.addItemToRender(points);
		points.addPoint(0.5, 0.1);
		
		
		CoordSysRenderer csr = new CoordSysRenderer();
		csr.setContent(render.withAppended(prender));
		Legend legend = new Legend();
		csr.setLegendBottom(legend);
		legend.addLineLabel(2, 0xffff0055, "a pink line");
		
		canvas.setRenderer(csr);
		
		frame.getContentPane().add(canvas);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				Object subject = canvas;
				if(subject instanceof FBOCanvas) {
					FBOCanvas cnvs = (FBOCanvas)subject;
					cnvs.runInContext(()->cnvs.close());
				}
			}
		});
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
		
	}
	
	static Point2D p(double x, double y) {
		return new Point2D.Double(x, y);
	}

}
