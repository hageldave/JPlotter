package hageldave.jplotter;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

/** 
 * Class containing convenience methods to skip the tedious boilerplate code
 * needed for setup in common scenarios.
 */
public class Boilerplate {

	public static JFrame createJFrameWithBoilerPlate(String title) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		frame.setTitle(title);
		frame.setMinimumSize(new Dimension(400, 400));
		return frame;
	}
	
}
