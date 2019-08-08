package hageldave.jplotter.misc;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.imagingkit.core.util.ImageFrame;

public class SignedDistanceCharacters {
	
	public static final int genFontSize = 32;
	public static final int padding = 8;
	
	protected static final Img FONTMETRIC_IMG = new Img(64, 64);
	
	public static Img mkCharDistanceField(char ch, String fontname, int style) {
		Font f = new Font(fontname, style, genFontSize);
		// determine width of character and descent for font
		int[] advance = {0};
		int[] descent = {0};
		int[] fontHeight = {0};
		FONTMETRIC_IMG.paint(g2d->{
			FontMetrics fontMetrics = g2d.getFontMetrics(f);
			descent[0] = fontMetrics.getDescent();
			advance[0] = fontMetrics.getMaxAdvance();
			fontHeight[0] = fontMetrics.getHeight();
		});
		// create signed distance field image
		Img tex = new Img(padding*2+advance[0], padding*2+fontHeight[0]).fill(0xff000000);
		tex.paint(g2d->{
			g2d.setColor(Color.WHITE);
			g2d.setFont(f);
			g2d.drawString(""+ch, padding, padding+fontHeight[0]-descent[0]);
		});
		int maxEdgeDist = padding;
		tex.forEach(px->{
			double edgeDistance = edgeDistance(px, maxEdgeDist);
			edgeDistance = Math.min(edgeDistance, maxEdgeDist);
			// normalize
			edgeDistance = edgeDistance/maxEdgeDist;
			if(px.r() == 0){
				px.setG((int) (128-edgeDistance*128));
			} else {
				px.setG((int) (127+edgeDistance*128));
			}
		});
		tex.forEach(px->{
			px.setRGB(px.g(), px.g(), px.g());
		});
		return tex;
	}
	
	static double edgeDistance(Pixel px, int maxDist){
		Img source = px.getSource();
		int x = px.getX();
		int y = px.getY();
		double dist = maxDist;
		for(int dx = -maxDist; dx <= maxDist; dx++){
			for(int dy = -maxDist; dy <= maxDist; dy++){
				int value = source.getValue(x+dx, y+dy, Img.boundary_mode_zero);
				if(Pixel.r(value) != px.r()){
					// found a value that differs from current value
					// update distance to smaller one
					dist = Math.min(dist, Math.sqrt(dx*dx+dy*dy));
				}
			}
		}
		return dist;
	}
	
	public static void main(String[] args) {
		String characters = CharacterAtlas.CHARACTERS;
		Img img = mkCharDistanceField(characters.charAt(0), CharacterAtlas.FONT_NAME, Font.PLAIN);
		System.out.println(img.getWidth());
		for(int i=1; i < characters.length(); i++){
			Img next = mkCharDistanceField(characters.charAt(i), CharacterAtlas.FONT_NAME, Font.PLAIN);
			Img merge = new Img(img.getWidth()+next.getWidth(), img.getHeight());
			img.copyArea(0, 0, img.getWidth(), img.getHeight(), merge, 0, 0);
			next.copyArea(0, 0, next.getWidth(), next.getHeight(), merge, img.getWidth(), 0);
			img = merge;
		}
		ImageFrame.display(img);
		System.out.println(img.getWidth());
		System.out.println(characters.length());
	}
	
}
