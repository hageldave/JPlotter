package jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.lwjgl.opengl.awt.GLData;

import jplotter.FBOCanvas;
import jplotter.globjects.CharacterAtlas;
import jplotter.globjects.DynamicText;
import jplotter.globjects.Lines;
import jplotter.globjects.StaticText;
import jplotter.renderers.LinesRenderer;
import jplotter.renderers.QuadWithFrag;
import jplotter.renderers.TextRenderer;

public class Viz {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		JFrame frame = new JFrame("AWT test");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().setPreferredSize(new Dimension(300, 300));
		GLData data = new GLData();
		data.samples = 4;
		data.swapInterval = 0;
		QuadWithFrag qwf = new QuadWithFrag();
		TextRenderer txtr = new TextRenderer();
		LinesRenderer lnsr = new LinesRenderer();
		FBOCanvas canvas;
		frame.getContentPane().add(canvas = new FBOCanvas(data) {
			private static final long serialVersionUID = 1L;
			{
				fboClearColor = Color.ORANGE;
			}
			public void paintToFBO(int w, int h) {
//				qwf.render(w,h);
				txtr.render(w, h);
				lnsr.render(w, h);
			}
			@Override
			public void initGL() {
				super.initGL();
				qwf.glInit();
				txtr.glInit();
				lnsr.glInit();
			}
		}, BorderLayout.CENTER);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				qwf.close();
				txtr.close();
				lnsr.close();
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
			public void updateGL() {
				setTextFromString(Long.toString(System.currentTimeMillis()));
				super.updateGL();
			}
		}.setOrigin(40, 200).setPickColor(0xc0ffee));
		
		Lines lines = new Lines();
		for(int i=-100; i<100; i++){
			int j = i+1;
			double x1 = (i+100)*2;
			double y1 = Math.exp(i*0.1)/(Math.exp(i*0.1)+1)*100+10;
			double x2 = (j+100)*2;
			double y2 = Math.exp(j*0.1)/(Math.exp(j*0.1)+1)*100+10;
			lines.addSegment(new Point2D.Double(x1,y1), new Point2D.Double(x2,y2), Color.MAGENTA);
		}
		lnsr.addLines(lines);
		
		

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
