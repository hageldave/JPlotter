package jplotter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.joml.Math;

import jplotter.globjects.CharacterAtlas;
import jplotter.globjects.DefaultGlyph;
import jplotter.globjects.Points;
import jplotter.renderers.CompleteRenderer;

public class StatLogViz {

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("Statlog (Shuttle) data set - Features 7 & 8");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		canvas.setContent(content);

		// setup content
		{
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
				canvas.setCoordinateArea(minX-10, minY-10, maxX+1, maxY+10);
			}
		}

		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.close();
				CharacterAtlas.clearAndCloseAtlasCollection();
			}
		});

		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int pixel = canvas.getPixel(e.getX(), e.getY(), SwingUtilities.isRightMouseButton(e));
				System.out.println(Integer.toHexString(pixel));
			}
		});

		Runnable renderLoop = new Runnable() {
			public void run() {
				if (!(canvas.isValid()))
					return;
				canvas.render();
				SwingUtilities.invokeLater(this);
			}
		};
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
		SwingUtilities.invokeLater(renderLoop);
	}

}
