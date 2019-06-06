package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.joml.Math;

import hageldave.jplotter.CoordSysCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.interaction.CoordSysScrollZoom;
import hageldave.jplotter.renderables.DefaultGlyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderers.CompleteRenderer;

public class StatLogViz {

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Statlog (Shuttle) data set");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		canvas.setContent(content);

		// setup content
		Points[] pointclasses = new Points[]{
				new Points(DefaultGlyph.CROSS),
				new Points(DefaultGlyph.CIRCLE),
				new Points(DefaultGlyph.CIRCLE_F),
				new Points(DefaultGlyph.SQUARE),
				new Points(DefaultGlyph.SQUARE_F),
				new Points(DefaultGlyph.TRIANGLE),
				new Points(DefaultGlyph.TRIANGLE_F)
		};
		int[] classcolors = new int[]{
				0xff1b9e77,
				0xffd95f02,
				0xff7570b3,
				0xffe7298a,
				0xff66a61e,
				0xffe6ab02,
				0xffa6761d
		};
		URL statlogsrc = new URL("https://archive.ics.uci.edu/ml/machine-learning-databases/statlog/shuttle/shuttle.tst");
		try (	InputStream stream = statlogsrc.openStream();
				Scanner  sc = new Scanner(stream);
				){
			/* Classes on last data column
			 * 1 Rad Flow 
			 * 2 Fpv Close
			 * 3 Fpv Open
			 * 4 High
			 * 5 Bypass
			 * 6 Bpv Close
			 * 7 Bpv Open 
			 */
			for(Points p : pointclasses)
				content.points.addItemToRender(p);
			// iterate dataset
			double maxX,maxY,minX,minY;
			maxX = maxY = Double.NEGATIVE_INFINITY;
			minX = minY = Double.POSITIVE_INFINITY;
			int i = 1;
			while(sc.hasNextLine()){
				String nextLine = sc.nextLine();
				String[] fields = nextLine.split(" ");
				int pclass = Integer.parseInt(fields[9])-1;
				int x = Integer.parseInt(fields[6]);
				int y = Integer.parseInt(fields[7]);
				pointclasses[pclass].addPoint(x, y, 0,1,classcolors[pclass],i++);
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
			minY = -10;
			canvas.setCoordinateView(minX-10, minY-10, maxX+1, maxY+10);
		}
		canvas.setxAxisLabel("Feature 7");
		canvas.setyAxisLabel("Feature 8");

		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
			}
		});
		JPanel footer = new JPanel();
		footer.setLayout(new BoxLayout(footer, BoxLayout.X_AXIS));
		frame.getContentPane().add(footer, BorderLayout.SOUTH);
		JSlider slider = new JSlider(1, 100, 100);
		slider.addChangeListener((e)->{
			double value = slider.getValue()/100.0;
			value*=value;
			for(Points p : pointclasses){
				p.setGlobalAlphaMultiplier(value);
			}
			canvas.repaint();	
		});
		footer.add(Box.createGlue());
		footer.add(new JLabel("Opacity:"));
		footer.add(slider);
		footer.add(Box.createGlue());

		new CoordSysPanning(canvas){{extModifierMask=0;}}.register();
		new CoordSysScrollZoom(canvas).setZoomFactor(1.5).register();
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}