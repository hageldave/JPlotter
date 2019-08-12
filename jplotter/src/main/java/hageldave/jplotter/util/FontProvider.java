package hageldave.jplotter.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class FontProvider {

	public static final Font UBUNTU_MONO_PLAIN = registerTrueTypeFont(
			FontProvider.class.getResource("/font/UbuntuMono-R.ttf"));
	
	public static final Font UBUNTU_MONO_BOLD = registerTrueTypeFont(
			FontProvider.class.getResource("/font/UbuntuMono-B.ttf"));
	
	public static final Font UBUNTU_MONO_ITALIC = registerTrueTypeFont(
			FontProvider.class.getResource("/font/UbuntuMono-RI.ttf"));
	
	public static final Font UBUNTU_MONO_BOLDITALIC = registerTrueTypeFont(
			FontProvider.class.getResource("/font/UbuntuMono-BI.ttf"));
	
	
	public static Font registerTrueTypeFont(URL resource){
		try(InputStream is = resource.openStream()){
			Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(12f);
			GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			environment.registerFont(font);
			return font;
		} catch (IOException | FontFormatException e) {
			throw new IllegalArgumentException(e);
		}
	}
	
	public static Font getUbuntuMono(float size, int style){
		switch (style) {
		case Font.PLAIN:
			return UBUNTU_MONO_PLAIN.deriveFont(size);
		case Font.BOLD:
			return UBUNTU_MONO_BOLD.deriveFont(size);
		case Font.ITALIC:
			return UBUNTU_MONO_ITALIC.deriveFont(size);
		case (Font.BOLD | Font.ITALIC):
			return UBUNTU_MONO_BOLDITALIC.deriveFont(size);
		default:
			throw new IllegalArgumentException(
					"Style argument is malformed. Only PLAIN, BOLD, ITALIC or BOLD|ITALIC are accepted.");
		}
	}
	
	public static void main(String[] args) {
		System.out.println(UBUNTU_MONO_BOLD);
	}
	
}
