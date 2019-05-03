package jplotter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.lwjgl.opengl.awt.GLData;

import jplotter.FBOCanvas;
import jplotter.renderers.QuadWithFrag;
import jplotter.renderers.TextRenderer;

public class Viz {

	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));
		GLData data = new GLData();
		data.samples = 4;
		data.swapInterval = 0;
		final long starttime = System.currentTimeMillis();
		QuadWithFrag qwf = new QuadWithFrag();
		TextRenderer txtr = new TextRenderer();
		FBOCanvas canvas;
		frame.getContentPane().add(canvas = new FBOCanvas(data) {
			private static final long serialVersionUID = 1L;
			public void paintToFBO(int w, int h) {
				float aspect = (float) w / h;
//				qwf.render(w,h);
				txtr.render(w, h);
			}
			@Override
			public void initGL() {
				super.initGL();
				qwf.glInit();
				txtr.glInit();
			}
		}, BorderLayout.CENTER);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				qwf.close();
				txtr.close();
				canvas.close();
			}
		});
		
		canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int pixel = canvas.getPixel(e.getX(), e.getY(), false);
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
