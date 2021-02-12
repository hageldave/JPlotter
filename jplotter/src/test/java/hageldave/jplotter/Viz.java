package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.LinesRenderer;

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
		Lines lines = new Lines().setStrokePattern(0b1110_0111_1001_0110);
		render.addItemToRender(lines);
		lines.addSegment(0, 0, 40, 50).setColor0(0xff00ff00).setColor1(0xffff0000);
		
		CoordSysRenderer csr = new CoordSysRenderer();
		csr.setContent(render);
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
