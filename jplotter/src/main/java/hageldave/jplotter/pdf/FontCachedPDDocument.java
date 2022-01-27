package hageldave.jplotter.pdf;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

/**
 * This is a {@link PDDocument} with pre-loaded fonts (Ubuntu mono which is used by JPlotter).
 * These are accessible through {@link #getFont(int)} so the document will not be polluted
 * with freshly loaded fonts every time text is added through {@link PDFUtils}.
 * 
 * @author hageldave
 */
public class FontCachedPDDocument extends PDDocument {
	
	protected final PDType0Font[] fonts = new PDType0Font[4];

	/**
	 * see {@link PDDocument#PDDocument()}
	 */
	public FontCachedPDDocument() {
		super();
		registerUbuntuFonts();
	}

	protected void registerUbuntuFonts() {
		try {
			for(int style=0; style<4; style++) {
				fonts[style] = PDFUtils.createPDFont(this, style);
			}
		} catch (IOException e) {
			throw new RuntimeException("Couldn't create fonts for document", e);
		}
	}
	
	/**
	 * returns the font corresponding to the desired style
	 * @param style one of Font.PLAIN, Font.BOLD, Font.ITALIC, Font.ITALIC|Font.BOLD
	 * @return the font used by JPlotter in the desired style.
	 */
	public PDType0Font getFont(int style) {
		return fonts[style];
	}
	
}
