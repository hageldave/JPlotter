package hageldave.jplotter;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.imagingkit.core.operations.Blending;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.font.FontProvider;

public class ColorMapImages {

	public static void main(String[] args) {
		for(DefaultColorMap map : DefaultColorMap.values()){
			Img img = map.toImg(250, 40, true, !map.name().startsWith("Q"));
			Img txtbackground = img.copy().fill(0);
			Font font = FontProvider.getUbuntuMono(12, Font.PLAIN);
			txtbackground.paint(g2d->{
				g2d.setColor(Color.black);
				g2d.setFont(font);
				FontMetrics fm = g2d.getFontMetrics();
				Rectangle bounds = fm.getStringBounds(map.name(), g2d).getBounds();
				g2d.fillRect(5+bounds.x, 30+bounds.y, bounds.width+4, bounds.height+2);
			});
			img.forEach(Blending.NORMAL.getAlphaBlendingWith(txtbackground, 0.4));
			img.paint(g2d->{
				g2d.setFont(font);
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2d.setColor(Color.black);
				g2d.drawString(map.name(), 7, 31);
				g2d.setColor(Color.white);
				g2d.drawString(map.name(), 6, 30);
			});
			
			ImageSaver.saveImage(img.getRemoteBufferedImage(), map.name()+".png");
			System.out.println("[[images/colormaps/"+map.name()+".png]]\\");
		}
	}
	
}
