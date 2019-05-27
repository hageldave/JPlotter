package hageldave.jplotter;

import static hageldave.jplotter.renderers.CompleteRenderer.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.CoordSysCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;

public class Viz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		content.setRenderOrder(PNT, LIN, TRI, TXT);
		canvas.setContent(content);
		
		// setup content
		{
			Triangles tris = new Triangles();
			Lines testcontent = new Lines();
			double scaling = 0.1;
			Color triColor = new Color(0x886633aa,true);
			for(int i = 0; i < 100; i++){
				double x1 = i*scaling;
				double x2 = (i+1)*scaling;
				double y1 = Math.sin(x1);
				double y2 = Math.sin(x2);
				testcontent.addSegment(x1, y1, x2, y2, 0xffff00ff);
				testcontent.addSegment(i, i, i+1, i+1, 0xff00ff00);
				tris.addQuad(x1,0, x1, y1, x2, y2, x2, 0, triColor);
			}
			testcontent.setThickness(2f);
			testcontent.setPickColor(0xffbabe);
			content.lines.addItemToRender(testcontent);
			content.triangles.addItemToRender(tris);
			
			Points circlepoints = new Points(DefaultGlyph.TRIANGLE_F);
			circlepoints.setGlobalAlphaMultiplier(0.2f);
			Points quiver = new Points(DefaultGlyph.ARROW);
			Color color1 = new Color(0xffe41a1c);
			Color color2 = new Color(0xff377eb8);
			for(int i = 0; i < 100; i++){
				circlepoints.addPoint(Math.random(), Math.random(), color1);
				double x = Math.random();
				double y = Math.random();
				quiver.addPoint(x,y, Math.atan2(-y, -x), Math.sqrt(x*x+y*y), color2);
			}
			content.points.addItemToRender(circlepoints).addItemToRender(quiver);
		}
		canvas.setCoordinateView(0, 0, 2, 1);
		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
			}
		});
		
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int pixel = canvas.getPixel(
						e.getX(), 
						e.getY(), 
						SwingUtilities.isRightMouseButton(e), 
						5);
				System.out.println(Integer.toHexString(pixel));
			}
		});
		new CoordSysPanning(canvas).register();
		new CoordSysScrollZoom(canvas).register();
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
