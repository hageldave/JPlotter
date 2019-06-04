package hageldave.jplotter;

import static hageldave.jplotter.renderers.CompleteRenderer.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import hageldave.jplotter.CoordSysCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CompleteRenderer;

public class Viz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setContentPane(new JPanel());
		frame.getContentPane().setBackground(Color.white);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		content.setRenderOrder(PNT, LIN, TRI, TXT);
		canvas.setContent(content);
		
		// setup content
		{
			Triangles tris = new Triangles();
			Lines lines = new Lines();
			double scaling = 0.1;
			Color triColor = new Color(0xff6633aa,true);
			for(int i = 0; i < 100; i++){
				double x1 = i*scaling;
				double x2 = (i+1)*scaling;
				double y1 = Math.sin(x1);
				double y2 = Math.sin(x2);
				lines.addSegment(x1, y1, x2, y2, 0xffff00ff,0xffff00ff, 0xbabe01);
				lines.addSegment(i, i, i+1, i+1, 0xff00ff00);
				tris.addQuad(x1,0, x1, y1, x2, y2, x2, 0, triColor);
			}
			tris.setGlobalAlphaMultiplier(0.2);
			lines.setThickness(2);
			lines.setGlobalAlphaMultiplier(0.8);
			content.lines.addItemToRender(lines);
			content.triangles.addItemToRender(tris);
			
			Points trianglepoints = new Points(DefaultGlyph.TRIANGLE_F);
			Points quiver = new Points(DefaultGlyph.ARROW);
			Color color1 = new Color(0xffe41a1c);
			Color color2 = new Color(0xff377eb8);
			for(int i = 0; i < 100; i++){
				trianglepoints.addPoint(Math.random(), Math.random(), color1);
				double x = Math.random();
				double y = Math.random();
				quiver.addPoint(x,y, Math.atan2(-y, -x), Math.sqrt(x*x+y*y), color2);
			}
			content.points.addItemToRender(trianglepoints).addItemToRender(quiver);
		}
		Legend legend = new Legend();
		legend.addGlyphLabel(DefaultGlyph.TRIANGLE_F, new Color(0xffe41a1c), "rand pnts");
		legend.addGlyphLabel(DefaultGlyph.ARROW, new Color(0xff377eb8), "-(x,y)");
		legend.addLineLabel(2, new Color(0xffff00ff), "sin(x)");
		legend.addLineLabel(2, new Color(0xff00ff00), "x=y");
		canvas.setLegendRight(legend);
		canvas.setLegendRightWidth(80);
		
		CompleteRenderer overlay = new CompleteRenderer();
		canvas.setOverlay(overlay);
		MouseAdapter areaZoom = new MouseAdapter() {
			Lines areaBorder = new Lines();
			Point start,end;
			@Override
			public void mousePressed(MouseEvent e) {
				start = e.getPoint();
				overlay.addItemToRender(areaBorder);
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				end = e.getPoint();
				int h = canvas.getHeight();
				areaBorder.removeAllSegments()
				.addSegment(start.x, h-start.y, start.x, h-end.y, 0xff222222)
				.addSegment(end.x, h-start.y, end.x, h-end.y, 0xff222222)
				.addSegment(start.x, h-start.y, end.x, h-start.y, 0xff222222)
				.addSegment(start.x, h-end.y, end.x, h-end.y, 0xff222222)
				;
				canvas.repaint();
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				areaBorder.removeAllSegments();
				overlay.lines.removeItemToRender(areaBorder);
				if(start != null && end != null){
					Point2D p1 = canvas.transformMouseToCoordSys(start);
					Point2D p2 = canvas.transformMouseToCoordSys(end);
					canvas.setCoordinateView(
							Math.min(p1.getX(), p2.getX()),
							Math.min(p1.getY(), p2.getY()),
							Math.max(p1.getX(), p2.getX()),
							Math.max(p1.getY(), p2.getY())
					);
				}
				canvas.repaint();
				start = null;
				end = null;
			}
		};
		canvas.addMouseListener(areaZoom);
		canvas.addMouseMotionListener(areaZoom);
		
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
//		new CoordSysPanning(canvas).register();
		new CoordSysScrollZoom(canvas).register();
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
