package hageldave.jplotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.svg.SVGUtils;

public class SVGTest {

	public static void main(String[] args) {
		Legend legend = new Legend();
		legend.addGlyphLabel(DefaultGlyph.TRIANGLE_F, new Color(0xffe41a1c), "rand pnts");
		legend.addGlyphLabel(DefaultGlyph.ARROW, new Color(0xff377eb8), "-(x,y)");
		legend.addLineLabel(2, new Color(0xffff00ff), "sin(x)");
		legend.addLineLabel(2, new Color(0xff00ff00), "x=y");

		BlankCanvas bc = new BlankCanvas();
		bc.setPreferredSize(new Dimension(100,200));
		JFrame f2 = new JFrame("f2");
		f2.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f2.getContentPane().add(bc);
		bc.setRenderer(legend);
		SwingUtilities.invokeLater(()->{
			f2.pack();
			f2.setVisible(true);
			f2.transferFocus();
		});
		
		bc.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(SwingUtilities.isRightMouseButton(e)){
					Document doc = bc.paintSVG();
					SVGUtils.documentToXMLFile(doc, new File("svgtest.svg"));
					System.out.println("svg exported:");
					System.out.println(SVGUtils.documentToXMLString(doc));
				}
			}
		});
	}

}
