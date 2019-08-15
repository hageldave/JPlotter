package hageldave.jplotter.font;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Scanner;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.Pixel;
import hageldave.imagingkit.core.io.ImageLoader;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.util.Pair;

/**
 * The SignedDistanceCharacters class comprises signed distance fields 
 * of a set of characters. 
 * A signed distance field is an image in which each pixel encodes the
 * distance to the nearest edge.
 * Such an image of a character glyph allows for efficient rendering of
 * text with arbitrary size and rotation using alpha testing and blending.
 * For more details read this paper 
 * <a href="https://dl.acm.org/citation.cfm?id=1281665">dl.acm.org/citation.cfm?id=1281665</a>.
 * <p>
 * The signed distance fields for each character are stored in a single
 * image alongside a lookup table for the bounding rectangles for each
 * character within that image.
 * The set of characters used is defined in {@link #CHARACTERS} which is
 * a String sorted by char value ascending.
 * <p>
 * Since the process of computing a SignedDistanceCharacters object is quite
 * time consuming, there are pre-computed SignedDistanceCharacters instances
 * for the Ubuntu Mono font used by JPlotter (see {@link FontProvider}) which
 * are statically loaded from files.
 * These are located in {@code .../resources/font/} alongside the corresponding
 * true type font files.
 * Similar to the {@link FontProvider#getUbuntuMono(float, int)}
 * the {@link #getUbuntuMonoSDC(int)} method can be used to access the
 * pre-computed SignedDistanceCharacters.
 * 
 * @author hageldave
 */
public class SignedDistanceCharacters {
	
	protected static final int genFontSize = 35;
	protected static final int padding = 8;
	protected static final Img FONTMETRIC_IMG = new Img(64, 64);
	
	/**
	 * Character set for instances of {@link SignedDistanceCharacters}.
	 * Characters in this string are sorted by char value ascending.
	 */
	public static final String CHARACTERS = 
			" !\"#$%&'()*+,-./0123456789:;<=>?@" +
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`" +
			"abcdefghijklmnopqrstuvwxyz{|}~" +
			"¢£¥©®×÷ĆćČčĐđŠšŽžΆΈΉΊΌΎΏΐΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΫ" +
			"άέέίΰαβγδεζηθικλμνξοπρστυφχψωϊϋόύ" + 
			"ЁЂЄЅІЇЈЉЊЋЎЏАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ" +
			"абвгдежзийклмнопрстуфхцчшщъыьэюяёђєѕіїјљњћўџҐґὰάὲέὴήὶίὸόὺύὼώ‘’“”€";
	
	public static final SignedDistanceCharacters UBUNTU_MONO_PLAIN = loadFromResource(
			SignedDistanceCharacters.class.getResource("/font/SDF_UbuntuMono-R.png"), 
			SignedDistanceCharacters.class.getResource("/font/BOUNDS_UbuntuMono-R.txt"));
	
	public static final SignedDistanceCharacters UBUNTU_MONO_BOLD = loadFromResource(
			SignedDistanceCharacters.class.getResource("/font/SDF_UbuntuMono-B.png"), 
			SignedDistanceCharacters.class.getResource("/font/BOUNDS_UbuntuMono-B.txt"));
	
	public static final SignedDistanceCharacters UBUNTU_MONO_ITALIC = loadFromResource(
			SignedDistanceCharacters.class.getResource("/font/SDF_UbuntuMono-RI.png"), 
			SignedDistanceCharacters.class.getResource("/font/BOUNDS_UbuntuMono-RI.txt"));
	
	public static final SignedDistanceCharacters UBUNTU_MONO_BOLDITALIC = loadFromResource(
			SignedDistanceCharacters.class.getResource("/font/SDF_UbuntuMono-BI.png"), 
			SignedDistanceCharacters.class.getResource("/font/BOUNDS_UbuntuMono-BI.txt"));
	
	/**
	 * left bounds for each character out of {@link #CHARACTERS} within
	 * this SignedDistanceCharacters texture image.
	 */
	public final int[] leftBounds = new int[CHARACTERS.length()];
	/**
	 * right bounds for each character out of {@link #CHARACTERS} within
	 * this SignedDistanceCharacters texture image.
	 */
	public final int[] rightBounds = new int[CHARACTERS.length()];
	/**
	 * top bounds for each character out of {@link #CHARACTERS} within
	 * this SignedDistanceCharacters texture image.
	 */
	public final int[] topBounds = new int[CHARACTERS.length()];
	/**
	 * bottom bounds for each character out of {@link #CHARACTERS} within
	 * this SignedDistanceCharacters texture image.
	 */
	public final int[] botBounds = new int[CHARACTERS.length()];
	/**
	 * texture image containing the signed distance fields of each character
	 * in {@link #CHARACTERS}.
	 */
	public final Img texImg;
	
	
	/**
	 * Creates a new {@link SignedDistanceCharacters} instance for the specified font.
	 * @param f font to use for the SDC
	 */
	public SignedDistanceCharacters(Font f) {
		Font font = f.deriveFont((float)genFontSize);
		// create texture img and setup character bounds
		Pair<Pair<Img, Img>, Rectangle> initial = mkCharDistanceField('K', font, null, null);
		Dimension dim = initial.first.first.getDimension();
		int charsPerLine = 24;
		texImg = new Img(dim.width*charsPerLine, dim.height*(CHARACTERS.length()/charsPerLine+1));
		int idx = 0;
		for(int j=0; idx<CHARACTERS.length(); j++){
			for(int i=0; i<charsPerLine && idx<CHARACTERS.length(); i++){
				// make texture for character
				Pair<Pair<Img,Img>, Rectangle> next = mkCharDistanceField(
						CHARACTERS.charAt(idx), 
						font, 
						initial.first.first, 
						initial.first.second
				);
				// put character into collection image
				int x = i*dim.width;
				int y = j*dim.height;
				next.first.first.copyArea(0, 0, dim.width, dim.height, texImg, x, y);
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
	
	private SignedDistanceCharacters(Img texImg, int[] leftBounds, int[] rightBounds, int[] topBounds, int[] botBounds){
		this.texImg = texImg;
		System.arraycopy(leftBounds, 0, this.leftBounds, 0, this.leftBounds.length);
		System.arraycopy(rightBounds, 0, this.rightBounds, 0, this.rightBounds.length);
		System.arraycopy(topBounds, 0, this.topBounds, 0, this.topBounds.length);
		System.arraycopy(botBounds, 0, this.botBounds, 0, this.botBounds.length);
	}
	
	/**
	 * Returns the {@link SignedDistanceCharacters} instance for the specified style
	 * of the Ubuntu Mono font (which is a pre-computed constant).
	 * @param style font style, one of {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
	 * or BOLD|ITALIC.
	 * @return ubuntu mono SDC for specified style
	 */
	public static SignedDistanceCharacters getUbuntuMonoSDC(int style){
		switch (style) {
		case Font.PLAIN:
			return UBUNTU_MONO_PLAIN;
		case Font.BOLD:
			return UBUNTU_MONO_BOLD;
		case Font.ITALIC:
			return UBUNTU_MONO_ITALIC;
		case (Font.BOLD | Font.ITALIC):
			return UBUNTU_MONO_BOLDITALIC;
		default:
			throw new IllegalArgumentException(
					"Style argument is malformed. Only PLAIN, BOLD, ITALIC or BOLD|ITALIC are accepted.");
		}
	}

	protected static Pair<Pair<Img,Img>,Rectangle> mkCharDistanceField(char ch, Font f, Img img, Img img2x) {
		// determine width of character and descent for font
		int[] advance = {0};
		int[] descent = {0};
		int[] fontHeight = {0};
		Rectangle bounds = new Rectangle();
		FONTMETRIC_IMG.paint(g2d->{
			FontMetrics fontMetrics = g2d.getFontMetrics(f);
			descent[0] = fontMetrics.getDescent();
			advance[0] = fontMetrics.charWidth('K');
			fontHeight[0] = fontMetrics.getHeight();
			Rectangle b = fontMetrics.getStringBounds(""+ch, g2d).getBounds();
			bounds.setRect(b);
		});
		
		bounds.setLocation(padding, padding);
		// create signed distance field image
		Img tex;
		if(Objects.isNull(img)){
			tex = new Img(padding*2+advance[0], padding*2+fontHeight[0]).fill(0xff000000);
		} else {
			tex = img;
			tex.fill(0);
		}
		tex.paint(g2d->{
			g2d.setColor(Color.WHITE);
			g2d.setFont(f);
			g2d.drawString(""+ch, padding, padding+fontHeight[0]-descent[0]);
		});
		// upscale x2
		Img tex2;
		if(Objects.isNull(img2x)){
			tex2 = new Img(tex.getWidth()*2, tex.getHeight()*2);
		} else {
			tex2 = img2x;
			tex2.fill(0);
		}
		tex2.forEach(true, px->{
			px.setValue(tex.getValue(px.getX()/2, px.getY()/2));
		});
		
		int maxEdgeDist = padding*2;
		tex2.forEach(true, px->{
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
			int value0 = tex2.getValue(px.getX()*2+0, px.getY()*2+0);
			int value1 = tex2.getValue(px.getX()*2+1, px.getY()*2+0);
			int value2 = tex2.getValue(px.getX()*2+0, px.getY()*2+1);
			int value3 = tex2.getValue(px.getX()*2+1, px.getY()*2+1);
			int value = Pixel.g(value0)+Pixel.g(value1)+Pixel.g(value2)+Pixel.g(value3);
			value /= 4;
			px.setRGB(value,value,value);
		});
		// DEBUG
//		tex.paint(g2d->{
//			g2d.setColor(Color.blue);
//			g2d.setFont(f);
//			g2d.drawString(""+ch, padding, padding+fontHeight[0]-descent[0]);
//			g2d.setColor(Color.GREEN);
//			g2d.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
//		});PLAIN
		return Pair.of(Pair.of(tex,tex2), bounds);
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
	
	/**
	 * Creates (pre-computes) the files for the Ubuntu Mono {@link SignedDistanceCharacters}.
	 * Just in case these ever need to be recreated.
	 */
	static void makeSDCFiles() {
		SignedDistanceCharacters signedDistanceCharacters = new SignedDistanceCharacters(FontProvider.UBUNTU_MONO_PLAIN);
		ImageSaver.saveImage(signedDistanceCharacters.texImg.getRemoteBufferedImage(), "SDF_UbuntuMono-R.png");
		bounds2File(signedDistanceCharacters, new File("BOUNDS_UbuntuMono-R.txt"));
		
		signedDistanceCharacters = new SignedDistanceCharacters(FontProvider.UBUNTU_MONO_BOLD);
		ImageSaver.saveImage(signedDistanceCharacters.texImg.getRemoteBufferedImage(), "SDF_UbuntuMono-B.png");
		bounds2File(signedDistanceCharacters, new File("BOUNDS_UbuntuMono-B.txt"));
		
		signedDistanceCharacters = new SignedDistanceCharacters(FontProvider.UBUNTU_MONO_ITALIC);
		ImageSaver.saveImage(signedDistanceCharacters.texImg.getRemoteBufferedImage(), "SDF_UbuntuMono-RI.png");
		bounds2File(signedDistanceCharacters, new File("BOUNDS_UbuntuMono-RI.txt"));
		
		signedDistanceCharacters = new SignedDistanceCharacters(FontProvider.UBUNTU_MONO_BOLDITALIC);
		ImageSaver.saveImage(signedDistanceCharacters.texImg.getRemoteBufferedImage(), "SDF_UbuntuMono-BI.png");
		bounds2File(signedDistanceCharacters, new File("BOUNDS_UbuntuMono-BI.txt"));
	}
	
	static SignedDistanceCharacters loadFromResource(URL imgResource, URL boundsResource){
		Img texImg = null;
		try(InputStream is = imgResource.openStream()){
			texImg = ImageLoader.loadImg(is);
		} catch (IOException e) {
			throw new IllegalArgumentException("loading image from resource failed - " + imgResource.toExternalForm(), e);
		}
		int[][] bounds = null;
		try(BufferedInputStream bis = new BufferedInputStream(boundsResource.openStream());
			Scanner sc = new Scanner(bis))
		{
			// read array length
			String next = sc.nextLine();
			int len = Integer.parseInt(next);
			bounds = new int[4][len];
			for(int i = 0; i < len; i++){
				next = sc.nextLine();
				String[] split = next.split(" ", -1);
				for(int j = 0; j < 4; j++){
					bounds[j][i] = Integer.parseInt(split[j]);
				}
			}
		} catch (IOException e) {
			throw new IllegalArgumentException("loading bounds from text resource failed - " + boundsResource.toExternalForm(), e);
		}
		Objects.requireNonNull(texImg);
		Objects.requireNonNull(bounds);
		return new SignedDistanceCharacters(texImg, bounds[0], bounds[1], bounds[2], bounds[3]);
	}
	
	static void bounds2File(SignedDistanceCharacters sdc, File f){
		try(FileWriter fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw))
		{
			bw.append(""+CHARACTERS.length());
			bw.append('\n');
			for(int i = 0; i < CHARACTERS.length(); i++){
				bw.append("" +sdc.leftBounds[i]);
				bw.append(" "+sdc.rightBounds[i]);
				bw.append(" "+sdc.topBounds[i]);
				bw.append(" "+sdc.botBounds[i]);
				bw.append('\n');
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
