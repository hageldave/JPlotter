package jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jplotter.globjects.CharacterAtlas;
import jplotter.globjects.DefaultGlyph;
import jplotter.globjects.Lines;
import jplotter.globjects.Points;
import jplotter.renderers.CompleteRenderer;

public class Viz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));;
		CoordSysCanvas canvas = new CoordSysCanvas();
		CompleteRenderer content = new CompleteRenderer();
		canvas.setContent(content);
		
		// setup content
		{
			Lines testcontent = new Lines();
			double scaling = 0.1;
			for(int i = 0; i < 100; i++){
				double x1 = i*scaling;
				double x2 = (i+1)*scaling;
				double y1 = Math.sin(x1);
				double y2 = Math.sin(x2);
				testcontent.addSegment(x1, y1, x2, y2, 0xffff00ff);
				testcontent.addSegment(i, i, i+1, i+1, 0xff00ff00);
			}
			testcontent.setThickness(2f);
			testcontent.setPickColor(0xffbabe);
			content.lines.addItemToRender(testcontent);
			
			Points circlepoints = new Points(DefaultGlyph.TRIANGLE_F);
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
