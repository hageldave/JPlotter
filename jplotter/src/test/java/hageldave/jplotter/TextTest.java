package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.misc.SignedDistanceCharacters;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderers.TextRenderer;

public class TextTest {
	
	static String sample = "The quick brown jumps 0.1749m to x@ZZYYXX";

	public static void main(String[] args) {
		TextRenderer tr = new TextRenderer();
		for(int i = 10; i < 24; i+=2){
			tr.addItemToRender(new Text(sample, i, Font.PLAIN, false)
					.setOrigin(i*11, i*12)
					.setAngle(-3.1415/4));
		}
		BlankCanvas canvas = new BlankCanvas();
		canvas.setPreferredSize(new Dimension(700, 400));
		canvas.setRenderer(tr);
		
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(canvas);
		
		
		JSlider sliderLeft = new JSlider(0, 100);
		JSlider sliderRight = new JSlider(0, 100);
		sliderLeft.setMajorTickSpacing(50);
		sliderRight.setMajorTickSpacing(50);
		sliderLeft.setPaintTicks(true);
		sliderRight.setPaintTicks(true);
		Container limits = new Container();
		limits.setLayout(new BorderLayout());
		limits.add(sliderLeft, BorderLayout.NORTH);
		limits.add(sliderRight, BorderLayout.SOUTH);
		frame.getContentPane().add(limits, BorderLayout.SOUTH);
		
		ChangeListener sliderchange = e->{
			double l = sliderLeft.getValue()*1.0/100;
			double r = sliderRight.getValue()*1.0/100;
			SignedDistanceCharacters.smoothStepLeft=l;
			SignedDistanceCharacters.smoothStepRight=r;
			System.out.println(l + " << >> " + r);
			canvas.repaint();
		};
		sliderLeft.addChangeListener(sliderchange);
		sliderRight.addChangeListener(sliderchange);
		
		
		JSlider sliderAngle = new JSlider(0, 360);
		frame.getContentPane().add(sliderAngle, BorderLayout.NORTH);
		sliderAngle.addChangeListener((e)->{
			int value = sliderAngle.getValue();
			double angle = value*3.1415*2/360;
			tr.getItemsToRender().forEach(txt->txt.setAngle(angle));
			canvas.repaint();
		});
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canvas.runInContext(()->canvas.close());
			}
		});
		
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
			frame.transferFocus();
		});
	}
	
}
