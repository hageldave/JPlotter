package hageldave.jplotter;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.interaction.CoordSysViewSelector;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.*;
import hageldave.jplotter.renderers.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.geom.Point2D;
import java.util.Arrays;

public class Viz {
	
	static JPlotterCanvas mkCanvas(boolean fallback) {
		return fallback ? new BlankCanvasFallback() : new BlankCanvas();
	}
	
	static boolean useFallback(String[] args) {
		return Arrays.stream(args).filter(arg->"jplotter_fallback=true".equals(arg)).findAny().isPresent();
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(new JPanel(new BorderLayout()));
		frame.getContentPane().setBackground(new Color(0xff333333));
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));
		
		JPlotterCanvas canvas = mkCanvas(false);
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
		
		TrianglesRenderer trirender = new TrianglesRenderer();
		Triangles tris = new Triangles();
		tris.enableAAinFallback(true);
		tris.addTriangle(-.5, 0, .5, .2, .4, .6).setColor0(0xff0000ff).setColor1(0xffff0000).setColor2(0xff00ff00);
		trirender.addItemToRender(tris);
		
		
		CoordSysRenderer csr = new CoordSysRenderer();
		csr.setColorScheme(DefaultColorScheme.DARK.get());
		csr.setContent(render.withAppended(prender).withAppended(trirender));
		Legend legend = new Legend();
		csr.setLegendBottom(legend);
		legend.addLineLabel(2, 0xffff0055, "a pink line");
		
		canvas.setRenderer(csr);
		canvas.asComponent().addMouseListener(new MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if(!SwingUtilities.isRightMouseButton(e)) return;
				ColorScheme scheme = csr.getColorScheme();
				if(scheme != DefaultColorScheme.LIGHT.get()) {
					csr.setColorScheme(DefaultColorScheme.LIGHT.get());
					frame.getContentPane().setBackground(new Color(DefaultColorScheme.LIGHT.get().getColorBackground()));
				} else {
					csr.setColorScheme(DefaultColorScheme.DARK.get());
					frame.getContentPane().setBackground(new Color(DefaultColorScheme.DARK.get().getColorBackground()));
				}
				canvas.scheduleRepaint();
			};
		});
		new CoordSysViewSelector(canvas,csr) {
			@Override
			public void areaSelected(double minX, double minY, double maxX, double maxY) {
				
			}
		}.register();
		
		frame.getContentPane().add(canvas.asComponent());
		
		canvas.addCleanupOnWindowClosingListener(frame);
		
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
