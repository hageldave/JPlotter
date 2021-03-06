package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Color;
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
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderers.TextRenderer;

public class TextTest {
	
	static String sample = " The quick brown jumps 0.18749m to x@ZZYYXX";

	public static void main(String[] args) {
		double[][] smoothStepLeft_ = {null};
		double[][] smoothStepRight_ = {null};
		
		TextRenderer tr = new TextRenderer(){
			{
				// make protected static arrays accessible
				smoothStepLeft_[0] = smoothStepLeft;
				smoothStepRight_[0] = smoothStepRight;
			}
		};
		
		double[] smoothStepLeft = smoothStepLeft_[0];
		double[] smoothStepRight = smoothStepRight_[0];
		
		for(int i = 10; i <= 27; i+=1){
			tr.addItemToRender(new Text(i + sample, i, Font.PLAIN)
					.setOrigin((i-9)*12, (i-9)*20)
					.setAngle(-3.1415/4)
					.setColor(Color.black)
					);
		}
		BlankCanvas canvas = new BlankCanvas();
		canvas.setPreferredSize(new Dimension(700, 400));
		canvas.setRenderer(tr);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
			smoothStepLeft[10-10]=l;
			smoothStepRight[10-10]=r;
			System.out.println(l + " << >> " + r);
			canvas.repaint();
		};
		sliderLeft.addChangeListener(sliderchange);
		sliderRight.addChangeListener(sliderchange);
		sliderLeft.addChangeListener(e->{
			int value = sliderLeft.getValue();
			int diff = 50-value;
			sliderRight.setValue(50+diff);
		});
		
		
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
