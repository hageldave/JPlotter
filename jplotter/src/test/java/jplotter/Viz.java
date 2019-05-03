package jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.lwjgl.opengl.awt.GLData;

import jplotter.FBOCanvas;
import jplotter.globjects.CharacterAtlas;
import jplotter.globjects.DynamicText;
import jplotter.globjects.StaticText;
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
			{
				fboClearColor = Color.ORANGE;
			}
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
		
		txtr.addText(new StaticText("hello", 12, Font.BOLD, false).setOrigin(40,50));
		txtr.addText(new StaticText("Whatup", 18, Font.PLAIN, false).setOrigin(40,32));
		txtr.addText(new DynamicText(Long.toString(System.currentTimeMillis()).toCharArray(), 22, Font.ITALIC, true){
			@Override
			public void updateVA() {
				setTextFromString(Long.toString(System.currentTimeMillis()));
				super.updateVA();
			}
		}.setOrigin(40, 200).setPickColor(0xc0ffee));
		
		

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
