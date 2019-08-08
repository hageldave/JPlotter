package hageldave.jplotter.misc;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.imagingkit.core.util.ImageFrame;
import hageldave.jplotter.util.Pair;

public class SignedDistanceCharacters {
	
	public static double smoothStepLeft = 0.37;
	public static double smoothStepRight = 0.63;
	
	protected static final int genFontSize = 32;
	protected static final int padding = 8;
	protected static final Img FONTMETRIC_IMG = new Img(64, 64);
	public static final String CHARACTERS = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	
	
	final int[] leftBounds = new int[CHARACTERS.length()];
	final int[] rightBounds = new int[CHARACTERS.length()];
	final int[] topBounds = new int[CHARACTERS.length()];
	final int[] botBounds = new int[CHARACTERS.length()];
	final Img texImg;
	protected final String fontname;
	protected final int style;
	protected final Font font;
	
	public SignedDistanceCharacters(String fontname, int style) {
		this.fontname = fontname;
		this.style = style;
		this.font = new Font(fontname, style, genFontSize);
		// create texture and setup character bounds
		Dimension dim = mkCharDistanceField(' ', font).first.getDimension();
		int charsPerLine = 16;
		texImg = new Img(dim.width*charsPerLine, dim.height*(CHARACTERS.length()/charsPerLine+1));
		int idx = 0;
		for(int j=0; idx<CHARACTERS.length(); j++){
			for(int i=0; i<charsPerLine && idx<CHARACTERS.length(); i++){
				// make texture for character
				Pair<Img, Rectangle> next = mkCharDistanceField(CHARACTERS.charAt(idx), font);
				// put character into collection image
				int x = i*dim.width;
				int y = j*dim.height;
				next.first.copyArea(0, 0, dim.width, dim.height, texImg, x, y);
				// set bounds for character
				leftBounds[idx]  = x+(int)next.second.getMinX();
				rightBounds[idx] = x+(int)next.second.getMaxX();
				topBounds[idx] = y+(int)next.second.getMinY();
				botBounds[idx] = y+(int)next.second.getMaxY();
//				int k = idx;
//				texImg.paint(g->{
//					g.setColor(Color.CYAN);
//					g.drawRect(leftBounds[k], topBounds[k], rightBounds[k]-leftBounds[k], botBounds[k]-topBounds[k]);
//				});
				idx++;
			}
		}
	}
	
	protected static Pair<Img,Rectangle> mkCharDistanceField(char ch, Font f) {
		// determine width of character and descent for font
		int[] advance = {0};
		int[] descent = {0};
		int[] fontHeight = {0};
		Rectangle bounds = new Rectangle();
		FONTMETRIC_IMG.paint(g2d->{
			FontMetrics fontMetrics = g2d.getFontMetrics(f);
			descent[0] = fontMetrics.getDescent();
			advance[0] = fontMetrics.getMaxAdvance();
			fontHeight[0] = fontMetrics.getHeight();
			Rectangle b = fontMetrics.getStringBounds(""+ch, g2d).getBounds();
			bounds.setRect(b);
		});
		bounds.setLocation(padding, padding);
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
		// DEBUG
//		tex.paint(g2d->{
//			g2d.setColor(Color.blue);
//			g2d.setFont(f);
//			g2d.drawString(""+ch, padding, padding+fontHeight[0]-descent[0]);
//			g2d.setColor(Color.GREEN);
//			g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
//		});
		return Pair.of(tex, bounds);
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
		SignedDistanceCharacters signedDistanceCharacters = new SignedDistanceCharacters(CharacterAtlas.FONT_NAME, Font.PLAIN);
		ImageFrame.display(signedDistanceCharacters.texImg);
	}
	
}
