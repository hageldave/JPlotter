package hageldave.jplotter.font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Base64;

/**
 * The FontProvider class provides the fonts JPlotter is using.
 * The font used is Ubuntu Mono, a monospaced font developed for 
 * the linux distribution Ubuntu
 * (see <a href="https://design.ubuntu.com/font/">design.ubuntu.com/font</a>).
 * Fonts are loaded statically from true type font files residing in 
 * {@code .../resources/font/}.
 * <p>
 * The method {@link #getUbuntuMono(float, int)} can be used to obtain
 * the font for a specific font size and style.
 * 
 * @author hageldave
 */
public final class FontProvider {
	
	public static final String UBUNTU_MONO_PLAIN_RESOURCE = "/font/UbuntuMono-R.ttf";
	public static final String UBUNTU_MONO_BOLD_RESOURCE = "/font/UbuntuMono-B.ttf";
	public static final String UBUNTU_MONO_ITALIC_RESOURCE = "/font/UbuntuMono-RI.ttf";
	public static final String UBUNTU_MONO_BOLDITALIC_RESOURCE = "/font/UbuntuMono-BI.ttf";

	public static final Font UBUNTU_MONO_PLAIN = registerTrueTypeFont(
			FontProvider.class.getResource(UBUNTU_MONO_PLAIN_RESOURCE));
	
	public static final Font UBUNTU_MONO_BOLD = registerTrueTypeFont(
			FontProvider.class.getResource(UBUNTU_MONO_BOLD_RESOURCE));
	
	public static final Font UBUNTU_MONO_ITALIC = registerTrueTypeFont(
			FontProvider.class.getResource(UBUNTU_MONO_ITALIC_RESOURCE));
	
	public static final Font UBUNTU_MONO_BOLDITALIC = registerTrueTypeFont(
			FontProvider.class.getResource(UBUNTU_MONO_BOLDITALIC_RESOURCE));
	
	/**
	 * Loads a true type font (ttf) from the specified source
	 * and registers it with the local {@link GraphicsEnvironment}.
	 * 
	 * @param resource to load the ttf from
	 * @return the loaded and registered font
	 * @throws IllegalArgumentException when an IOException occurs
	 * or the specified resource is not in ttf format.
	 */
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
	
	/**
	 * Returns the Ubuntu Mono font for the specified font size and style
	 * @param size font size 
	 * @param style font style, {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC} 
	 * or BOLD|ITALIC
	 * @return font of specified size and style
	 */
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

	/**
	 * Returns the base64 code of the Ubuntu Mono font as a string for the specified style.
	 * @param style font style, {@link Font#PLAIN}, {@link Font#BOLD}, {@link Font#ITALIC}
	 * or BOLD|ITALIC
	 * @return base64 code of the Ubuntu Mono font of specified style
	 */
	public static String getUbuntuMonoFontAsBaseString(int style) {
		String resource;
		switch (style) {
			case Font.PLAIN:
				resource = UBUNTU_MONO_PLAIN_RESOURCE;
				break;
			case Font.BOLD:
				resource = UBUNTU_MONO_BOLD_RESOURCE;
				break;
			case Font.ITALIC:
				resource = UBUNTU_MONO_ITALIC_RESOURCE;
				break;
			case (Font.BOLD | Font.ITALIC):
				resource = UBUNTU_MONO_BOLDITALIC_RESOURCE;
				break;
			default:
				throw new IllegalArgumentException(
						"Style argument is malformed. Only PLAIN, BOLD, ITALIC or BOLD|ITALIC are accepted.");
		}
		try(
				InputStream in = FontProvider.class.getResource(resource).openStream();
				BufferedInputStream bis = new BufferedInputStream(in);
		) {
			byte[] fontFileArray = bis.readAllBytes();
			return Base64.getEncoder().encodeToString(fontFileArray);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the Ubuntu mono font licence as a string
	 *
	 * @return ubuntu mono font licence as a string
	 */
	public static String getUbuntuMonoFontLicence() {
		try(
				InputStream in = FontProvider.class.getResource("/font/LICENCE.txt").openStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
		) {
			StringBuilder sb = new StringBuilder();
			String line;
			while((line=br.readLine()) != null)
				sb.append(line).append("\n");
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
