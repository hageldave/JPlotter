package jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jplotter.renderables.CharacterAtlas;
import jplotter.renderables.DefaultGlyph;
import jplotter.renderables.Points;
import jplotter.renderers.CompleteRenderer;

public class IrisViz {

	public static void main(String[] args) throws IOException {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		canvas.setContent(content);

		// setup content
		{
			URL irissrc = new URL("https://gist.githubusercontent.com/netj/8836201/raw/6f9306ad21398ea43cba4f7d537619d0e07d5ae3/iris.csv");
			try (	InputStream stream = irissrc.openStream();
					Scanner  sc = new Scanner(stream);
					){
				if(sc.hasNextLine()){
					String nextLine = sc.nextLine();
					System.out.println(nextLine);
				}
				Points setosa = new Points(DefaultGlyph.CROSS);
				Points versicolor = new Points(DefaultGlyph.SQUARE);
				Points virginica = new Points(DefaultGlyph.TRIANGLE);
				while(sc.hasNextLine()){
					String nextLine = sc.nextLine();
					String[] fields = nextLine.split(",");
					double sepall = Double.parseDouble(fields[0]);
					double sepalw = Double.parseDouble(fields[1]);
					double petall = Double.parseDouble(fields[2]);
					double petalw = Double.parseDouble(fields[3]);
					if(fields[4].contains("Setosa")){
						setosa.addPoint(petall, petalw, Color.RED);
					} else if(fields[4].contains("Versicolor")) {
						versicolor.addPoint(petall, petalw, Color.BLUE);
					} else {
						virginica.addPoint(petall, petalw, Color.ORANGE);
					}
				}
				content.points.addItemToRender(virginica).addItemToRender(versicolor).addItemToRender(setosa);
			}
		}
		canvas.setCoordinateView(0, 0, 8, 3);

		frame.getContentPane().add(canvas, BorderLayout.CENTER);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.close();
				CharacterAtlas.clearAndCloseAtlasCollection();
			}
		});
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}

}
