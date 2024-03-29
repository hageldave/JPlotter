package hageldave.jplotter;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.misc.DefaultGlyph;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class SVGTest {

	public static void main(String[] args) {
		Legend legend = new Legend();
		legend.addGlyphLabel(DefaultGlyph.TRIANGLE_F, 0xffe41a1c, "rand pnts");
		legend.addGlyphLabel(DefaultGlyph.ARROW, 0xff377eb8, "-(x,y)");
		legend.addLineLabel(2, 0xffff00ff, "sin(x)");
		legend.addLineLabel(2, 0xff00ff00, "x=y");

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
				if(SwingUtilities.isMiddleMouseButton(e)){
					try {
						PDDocument doc = bc.paintPDF();
						doc.save("pdftest.pdf");
						doc.close();
						System.out.println("pdf exported.");
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		});
	}

}
