package hageldave.jplotter.howto;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.charts.BarChart;
import hageldave.jplotter.renderables.BarGroup;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.TrianglesRenderer;
import hageldave.jplotter.util.ExportUtil;
import hageldave.jplotter.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.function.BiFunction;
import java.util.function.DoubleFunction;
import java.util.stream.IntStream;

public class BarChartDemo {
	
	public static void main(String[] args) {
		newWay();
	}

	public static void oldWay() {
		// have some data
		String[] cases = {"A","B","C","D1","D2*"};
		double[] scores = IntStream.range(0, cases.length)
				.mapToDouble(i->Math.random()).toArray();
		// make helper function to create the bars
		BiFunction<Integer,Double,Triangles> makeBar = (row, val)->{
			Triangles bar = new Triangles();
			bar.addQuad(new Rectangle2D.Double(0, row-0.4, val, 0.8));
			bar.getTriangleDetails().forEach(tri->{
				int red=0xff_e41a1c, blue=0xff_377eb8;
				tri.setColor(val < 0.5 ? red:blue);
			});
			return bar;
		};
		// We want to display Triangles, so we need the appropriate renderer.
		TrianglesRenderer barRenderer = new TrianglesRenderer();
		// add the bar for each case to the renderer
		for(int i=0; i<cases.length; i++){
			Triangles bar = makeBar.apply(i, scores[i]);
			barRenderer.addItemToRender(bar);
		}
		// use a coordinate system for display
		CoordSysRenderer coordsys = new CoordSysRenderer();
		coordsys.setCoordinateView(0, -1, 1, cases.length);
		// set the content renderer of the coordinate system 
		coordsys.setContent(barRenderer);
		// we need to change the tick marks labeling for the y axis
		coordsys.setTickMarkGeneratorY((min,max,desired,vert)->{
			// make ticks at integer values (0,1,2,...)
			double[] ticks = IntStream.range(0, cases.length)
					.mapToDouble(i -> (double)i)
					.toArray();
			// use case names as tick mark labels
			return Pair.of(ticks, cases);
		});
		// set axis labels
		coordsys.setxAxisLabel("Score");
		coordsys.setyAxisLabel("");
		
		// display within a JFrame
		JFrame frame = new JFrame();
		boolean useOpenGL = true;
		JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
		canvas.setRenderer(coordsys);
		canvas.asComponent().setPreferredSize(new Dimension(500, 300));
		canvas.asComponent().setBackground(Color.WHITE);
		frame.getContentPane().add(canvas.asComponent());
		frame.setTitle("barchart");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		canvas.addCleanupOnWindowClosingListener(frame);
		// make visible on AWT event dispatch thread
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});

		frame.setJMenuBar(ExportUtil.createSaveMenu(frame, "howto_barchart"));
	}
	
	public static void newWay() {
		// have some data
		String[] cases = {"A","B","C","D1","D2*"};
		double[] scores = IntStream.range(0, cases.length)
			.mapToDouble(i->Math.random()).toArray();
		DoubleFunction<Color> colormap =  (val) -> {
			int red=0xff_e41a1c, blue=0xff_377eb8;
			return val < 0.5 ? new Color(red):new Color(blue);
		};
		
		// make the barchart
		BarChart barchart = new BarChart(false, true);
		for(int i=0; i<cases.length; i++) {
			BarGroup group = new BarGroup();
			group.addBarStack(0, scores[i], colormap.apply(scores[i]), cases[i]);
			barchart.addData(group);
		}
		// only single bar per group, no need for gap
		barchart.getBarRenderer().setBargroupGap(0);
		// select view automatically to fit all bars
		barchart.alignBarRenderer();
		barchart.getBarRenderer().setxAxisLabel("Score");
		
		// boilerplate code to put the component in a frame and show it
		JFrame frame = new JFrame();
		barchart.getCanvas().asComponent().setPreferredSize(new Dimension(500, 300));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(barchart.getCanvas().asComponent());
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});
		
		frame.setJMenuBar(ExportUtil.createSaveMenu(frame, "howto_barchart"));
	}


}



