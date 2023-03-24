package hageldave.jplotter.svg;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.NewText;
import hageldave.jplotter.renderables.TextDecoration;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.scilab.forge.jlatexmath.DefaultTeXFont;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;
import org.scilab.forge.jlatexmath.cyrillic.CyrillicRegistration;
import org.scilab.forge.jlatexmath.greek.GreekRegistration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.batik.anim.dom.SVGDOMImplementation.SVG_NAMESPACE_URI;

/**
 * Utility class for SVG related methods.
 *
 * @author hageldave
 */
public class SVGUtils {

	private static final AtomicLong defIdCounter = new AtomicLong();

	/**
	 * Creates a new SVG element of the SVG namespace for the specified document.
	 * @param doc SVG document for creating the element with
	 * @param element name of the element e.g. "g" or "polygon"
	 * @return the created element
	 */
	public static Element createSVGElement(Document doc, String element){
		return doc.createElementNS(SVG_NAMESPACE_URI, element);
	}

	/**
	 * Creates a {@code rect} element of specified size and location, i.e.
	 * sets the "x","y","width" and "height" attributes.
	 * @param doc to create the element with
	 * @param x coordinate of the rectangle
	 * @param y coordinate of the rectangle
	 * @param w width of the rectangle
	 * @param h height of the rectangle
	 * @return the rect element
	 */
	public static Element createSVGRect(Document doc, double x, double y, double w, double h){
		Element rect = createSVGElement(doc, "rect");
		rect.setAttributeNS(null, "x", ""+x);
		rect.setAttributeNS(null, "y", ""+y);
		rect.setAttributeNS(null, "width", ""+w);
		rect.setAttributeNS(null, "height", ""+h);
		return rect;
	}

	/**
	 * Returns the specified coordinate values as would be specified
	 * in a "points" attribute. E.g. "0,0 1.3,2 2.03,4.3"
	 * @param coords to format as points string (x,y interleaved)
	 * @return the points string
	 */
	public static String svgPoints(double...coords){
		String s = "";
		for(int i=0; i < coords.length/2; i++){
			s += svgNumber(coords[i*2+0]);
			s += ",";
			s += svgNumber(coords[i*2+1]);
			if(i < (coords.length/2)-1)
				s+= " ";
		}
		return s;
	}

	/**
	 * Creates a {@code polygon} element with the specified triangle vertices,
	 * i.e. sets the polygon's "points" attribute.
	 * @param doc to create the element with
	 * @param x0 x coordinate of first vertex
	 * @param y0 y coordinate of first vertex
	 * @param x1 x coordinate of second vertex
	 * @param y1 y coordinate of second vertex
	 * @param x2 x coordinate of third vertex
	 * @param y2 y coordinate of third vertex
	 * @return the polygon elements
	 */
	public static Element createSVGTriangle(Document doc, double x0, double y0, double x1, double y1, double x2, double y2){
		Element polygon = createSVGElement(doc, "polygon");
		polygon.setAttributeNS(null, "points", svgPoints(x0,y0,x1,y1,x2,y2));
		return polygon;
	}

	public static Element createSVGLine(Document doc, double x0, double y0, double x1, double y1) {
		Element line = createSVGElement(doc, "line");
		line.setAttributeNS(null, "x0", SVGUtils.svgNumber(x0));
		line.setAttributeNS(null, "y0", SVGUtils.svgNumber(y0));
		line.setAttributeNS(null, "x1", SVGUtils.svgNumber(x1));
		line.setAttributeNS(null, "y1", SVGUtils.svgNumber(y1));
		return line;
	}

	/**
	 * Returns an html like hexadecimal RGB color string
	 * from the specified integer packed ARGB value.
	 * E.g. the value 0xff234567 will result in "#234567".
	 * @param argb integer packed ARGB color value
	 * @return svg color string for use with the "fill" attribute for example.
	 */
	public static String svgRGBhex(int argb){
		return '#'+Integer.toHexString(0xff000000 | argb).substring(2);
	}

	/**
	 * Returns a CSS rgba color definition string
	 * for the specified integer packed ARGB color value.
	 * E.g. the value 0xffb5ff00 will result in "rgba(0.71,1,0,1)".
	 * @param argb integer packed ARGB value
	 * @return CSS style rgba color definition
	 */
	public static String cssRGBA(int argb){
		return "rgba("
				+svgNumber(Pixel.r_normalized(argb))+","
				+svgNumber(Pixel.g_normalized(argb))+","
				+svgNumber(Pixel.b_normalized(argb))+","
				+svgNumber(Pixel.a_normalized(argb))+")";
	}

	/**
	 * Serializes the specified document as xml formatted string.
	 * @param doc to serialize
	 * @return xml representation of the specified document
	 * @throws RuntimeException when either an
	 * {@link IOException} or {@link TranscoderException} occurs during the process.
	 */
	public static String documentToXMLString(Document doc){
		TranscoderInput input = new TranscoderInput(doc);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try(
				OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
				){
			TranscoderOutput output = new TranscoderOutput(osw);
			new SVGTranscoder().transcode(input, output);
		} catch (IOException | TranscoderException e) {
			throw new RuntimeException(e);
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		StringBuilder xml = new StringBuilder();
		try(Scanner sc = new Scanner(bais)){
			while(sc.hasNext()){
				xml.append(sc.nextLine());
				xml.append(System.lineSeparator());
			}
		}
		return xml.toString();
	}

	/**
	 * Writes the specified document to file in xml format.
	 * @param doc document to serialize
	 * @param file to write to
	 * @throws RuntimeException when either an
	 * {@link IOException} or {@link TranscoderException} occurs during the process.
	 */
	public static void documentToXMLFile(Document doc, File file){
		TranscoderInput input = new TranscoderInput(doc);
		try(
				FileOutputStream fos = new FileOutputStream(file);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				OutputStreamWriter osfw = new OutputStreamWriter(bos, "UTF-8");
				){
			TranscoderOutput output = new TranscoderOutput(osfw);
			SVGTranscoder svgTranscoder = new SVGTranscoder();
			svgTranscoder.transcode(input, output);
		} catch (IOException | TranscoderException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the {@code defs} element of the specified document.
	 * @param doc to retrieve the definitions element from
	 * @return the {@code defs} node or null if not existent.
	 */
	public static Node getDefs(Document doc){
		Element element = doc.getElementById("JPlotterDefs");
		return element != null ? element : doc.getElementsByTagName("defs").item(0);
	}

	/**
	 * A new id string for use within the definitions section.
	 * A global atomic counter is incremented to retrieve a
	 * unique number and an id string of the form "def_2dh" is returned
	 * where the part after the underscore is the unique number formatted
	 * as 32-system number.
	 * @return new unique definitions id string
	 */
	public static String newDefId(){
		return "def_"+Long.toString(defIdCounter.incrementAndGet(), 32);
	}

	/**
	 * Returns a CSS font styling definition from
	 * a {@link Font} style bit field.
	 * E.g. The style {@code Font.ITALIC|Font.BOLD} translates
	 * to "font-style:italic;font-weight:bold;".
	 * @param style bitfield as used by {@link Font}
	 * @return CSS font styling string
	 */
	public static String fontStyleAndWeightCSS(int style){
		switch(style){
		case Font.ITALIC:
			return "font-style:italic;font-weight:normal;";
		case Font.BOLD:
			return "font-style:normal;font-weight:bold;";
		case Font.ITALIC|Font.BOLD:
			return "font-style:italic;font-weight:bold;";
		default:
			return "font-style:normal;font-weight:normal;";
		}
	}

	/**
	 * Formats the specified number for SVG, that is
	 * at most 3 decimal places but preferably less to
	 * bring down document size.
	 * @param x number to format
	 * @return formatted number
	 */
	public static String svgNumber(double x){
		if(x==(int)x){
			return ""+((int)x);
		}
		String s = String.format(Locale.US, "%.3f", x);
		if(s.contains(".")){
			s = s.replace('0', ' ').trim().replace(' ', '0');
			s = s.startsWith(".") ? "0"+s:s;
			s = s.endsWith(".") ? s.substring(0, s.length()-1):s;
		}
		return s;
	}

	/**
	 * Creates an SVG {@code symbol} definition in the specified documents {@code defs} section
	 * for the specified {@link Glyph}.
	 * If the document already contains an element for the specified id, no symbol is appended
	 * to the defs.
	 * @param doc document to create the symbol definition in
	 * @param glyph to create the symbol from
	 * @param defId id for symbol
	 * @return the specified defId
	 */
	public static String createGlyphSymbolDef(Document doc, Glyph glyph, String defId){
		if(doc.getElementById(defId)==null){
			Node defs = getDefs(doc);
			Element symbol = createSVGElement(doc, "symbol");
			symbol.setAttributeNS(null, "id", defId);
			defs.appendChild(symbol);
			List<Element> glyphSVG = glyph.createSVGElements(doc);
			for(Element e:glyphSVG){
				e.setAttributeNS(null, "vector-effect", "non-scaling-stroke");
				symbol.appendChild(e);
			}
			symbol.setAttributeNS(null, "overflow", "visible");
		}
		return defId;
	}

	/**
	 * Creates an SVG document that has the {@link SVGDOMImplementation#SVG_NAMESPACE_URI}
	 * with the specified width and height for the root svg element.
	 * @param w width
	 * @param h height
	 * @return SVG document
	 */
	public static Document createSVGDocument(int w, int h){
		DOMImplementation domImplementation = SVGDOMImplementation.getDOMImplementation();
		Document document = domImplementation.createDocument(SVG_NAMESPACE_URI, "svg", null);
		Element root = document.getDocumentElement();
		root.setAttributeNS(null,"width",""+w);
		root.setAttributeNS(null, "height", ""+h);
		return document;
	}

	/**
	 * Draws all components of a {@link Container} to
	 * an SVG document.
	 * For this an {@link SVGGraphics2D} object is used that will
	 * create the SVG DOM for the specified container and its children.
	 * Instances of {@link JPlotterCanvas} will be treated separately by
	 * using their {@link JPlotterCanvas#paintSVG(Document, Element)} method
	 * to create their part of the DOM that cannot be generated from {@link SVGGraphics2D}.
	 * <p>
	 * For drawing a single {@link JPlotterCanvas} to SVG the method {@link JPlotterCanvas#paintSVG()}
	 * can be used instead of this method.
	 *
	 * @param c container to be converted to SVG
	 * @return SVG document representing the specified container.
	 */
	public static Document containerToSVG(Container c){
		Document document = createSVGDocument(c.getWidth(), c.getHeight());
		// make jplotter defs
		Element defs = createSVGElement(document, "defs");
		defs.setAttributeNS(null, "id", "JPlotterDefs");
		document.getDocumentElement().appendChild(defs);
		{ /* draw all non JPlotterCanvas components
	       * (and those that are isSvgAsImageRenderingEnabled()==true
		   * which is checked by respective implementation's paint methods)
		   */
			SVGGraphics2D g2d = new SVGPatchedGraphics2D(document);
			c.paintAll(g2d);
			// set default font size to inherit by awt/swing elements (batik does not do this)
			Element awtgroup = SVGUtils.createSVGElement(document, "g");
			awtgroup.setAttribute("font-size", new JLabel().getFont().getSize()+"px");
			document.getDocumentElement().appendChild(awtgroup);
			awtgroup.appendChild(g2d.getTopLevelGroup(true));
		}
		// draw JPlotterCanvases
		containerToSVG(c, document, document.getDocumentElement());
		return document;
	}

	private static void containerToSVG(Container c, Document doc, Element parent){
		for(Component comp:c.getComponents()){
			if(comp instanceof JPlotterCanvas){
				JPlotterCanvas canvas = (JPlotterCanvas)comp;
				if(canvas.isSvgAsImageRenderingEnabled())
					return; // was already rendered through SVGGraphics2D
				Element group = SVGUtils.createSVGElement(doc, "g");
				group.setAttributeNS(null, "transform",
						"translate("+(canvas.asComponent().getX())+","+(canvas.asComponent().getY())+")");
				parent.appendChild(group);
				canvas.paintSVG(doc, group);
			} else {
				if(comp instanceof Container){
					Element group = SVGUtils.createSVGElement(doc, "g");
					group.setAttributeNS(null, "transform",
							"translate("+(comp.getX())+","+(comp.getY())+")");
					parent.appendChild(group);
					containerToSVG((Container)comp, doc, group);
				}
			}
		}
	}

	public static Element latexToSVG(NewText txt, Document doc, double x, double y) throws IOException {
		SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(doc);
		SVGGraphics2D g2 = new SVGGraphics2D(ctx, true);

		DefaultTeXFont.registerAlphabet(new CyrillicRegistration());
		DefaultTeXFont.registerAlphabet(new GreekRegistration());

		double iconHeight = y;
		for (NewText singleLineText : txt.generateTextObjectForEachLine()) {

			TeXFormula formula = new TeXFormula(singleLineText.getTextString());
			TeXIcon icon = formula.createTeXIcon(TeXConstants.STYLE_DISPLAY, txt.fontsize);
			icon.setInsets(new Insets(txt.getInsets().top, txt.getInsets().left, txt.getInsets().bottom, txt.getInsets().right));

			g2.setSVGCanvasSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
			g2.setColor(txt.getBackground());
			if (singleLineText.getTextString().length() > 0)
				g2.fillRect((int) x, (int) iconHeight, icon.getIconWidth(), icon.getIconHeight());

			JLabel jl = new JLabel();
			jl.setForeground(txt.getColor());
			icon.paintIcon(jl, g2, (int) x, (int) iconHeight);
			iconHeight += icon.getIconHeight();
		}
		Element textGroup = SVGUtils.createSVGElement(doc, "g");
		doc.getDocumentElement().appendChild(textGroup);
		textGroup.appendChild(g2.getTopLevelGroup(true));
		return textGroup;
	}

	public static Element textToSVG(NewText txt, Document doc, Element parent, double x, double y) throws IOException {
		String fontfamily = "'Ubuntu Mono', monospace";
		double textHeight = 1;

		for (NewText singleLineText : txt.generateTextObjectForEachLine()) {
			if (txt.getBackground().getRGB() != 0) {
				Element backgroundText;
				if (txt.getInsets().right != 0 || txt.getInsets().left != 0 || txt.getInsets().top != 0 || txt.getInsets().bottom != 0) {
					// still hacky
					PDDocument pdDoc = new PDDocument();
					PDType0Font font = PDFUtils.createPDFont(pdDoc, txt.style);
					float width = font.getStringWidth(singleLineText.getTextString()) / 1000 * txt.fontsize;
					pdDoc.close();
					backgroundText = SVGUtils.createSVGRect(doc, x, y + textHeight, width+txt.getHorizontalInsets(), singleLineText.getBounds().getHeight());
					backgroundText.setAttributeNS(null, "transform", "translate(" + SVGUtils.svgNumber(0) + "," + SVGUtils.svgNumber(- textHeight + (txt.getBounds().getHeight())) + ") scale(1,-1)");
					parent.appendChild(backgroundText);
				} else {
					backgroundText = SVGUtils.createTextBackgroundFilter(doc, parent, txt.getBackground());
					backgroundText.setAttributeNS(null, "transform", "translate(" + SVGUtils.svgNumber(0) + "," + SVGUtils.svgNumber(- textHeight  +(txt.getBounds().getHeight()-txt.getTextSize().getHeight())) + ") scale(1,-1)");
				}
				backgroundText.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
				backgroundText.setTextContent(singleLineText.getTextString());
				backgroundText.setAttributeNS(null, "style", "font-family:" + fontfamily + ";font-size:" + txt.fontsize + "px;" + SVGUtils.fontStyleAndWeightCSS(txt.style));
				backgroundText.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(txt.getBackground().getRGB()));
				backgroundText.setAttributeNS(null, "x", "" + 0);
				backgroundText.setAttributeNS(null, "y", "" + (txt.getTextSize().height - txt.fontsize));
			}

			Element text = SVGUtils.createSVGElement(doc, "text");
			parent.appendChild(text);

			text.setAttributeNS("http://www.w3.org/XML/1998/namespace", "xml:space", "preserve");
			text.setTextContent(singleLineText.getTextString());
			text.setAttributeNS(null, "style", "font-family:" + fontfamily + ";font-size:" + txt.fontsize + "px;" + SVGUtils.fontStyleAndWeightCSS(txt.style));
			text.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(txt.getColor().getRGB()));
			if (txt.getColorA() != 1) {
				text.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(txt.getColorA()));
			}
			text.setAttributeNS(null, "x", "" + 0);
			text.setAttributeNS(null, "y", "-" + (txt.getTextSize().height - txt.fontsize));

			if (txt.getTextDecoration() ==  TextDecoration.UNDERLINE) {
				text.setAttributeNS(null, "text-decoration", "underline");
			} else if (txt.getTextDecoration() ==  TextDecoration.STRIKETHROUGH) {
				text.setAttributeNS(null, "text-decoration", "line-through");
			}

			double fontDescent = CharacterAtlas.getFontMetrics(txt.fontsize, txt.style).getMaxDescent();
			parent.setAttributeNS(null, "transform",
					"translate("+SVGUtils.svgNumber(x)+","+SVGUtils.svgNumber(y+fontDescent)+")" + "rotate(" + SVGUtils.svgNumber(txt.getAngle() * 180 / Math.PI)+")");
			parent.setAttributeNS(null, "transform-origin", txt.getAnchorPointExport().getX() + " " + txt.getAnchorPointExport().getY());

			text.setAttributeNS(null, "transform", "translate(" + SVGUtils.svgNumber(txt.getInsets().left) + "," + SVGUtils.svgNumber(- textHeight + (txt.getBounds().getHeight()-txt.getTextSize().getHeight() - txt.getInsets().top)) + ") scale(1,-1)");
			textHeight += singleLineText.getBounds().getHeight();
		}
		return parent;
	}

	public static Element createTextBackgroundFilter(Document doc, Element parent, Color backgroundColor) {
        String defID = SVGUtils.newDefId();
        Element defs = SVGUtils.createSVGElement(doc, "defs");
        Element filter = SVGUtils.createSVGElement(doc, "filter");
        filter.setAttributeNS(null, "x", "" + 0);
        filter.setAttributeNS(null, "y", "" + 0);
        filter.setAttributeNS(null, "width", "" + 1);
        filter.setAttributeNS(null, "height", "" + 1);
        filter.setAttributeNS(null, "id", defID);
        Element feFlood = SVGUtils.createSVGElement(doc, "feFlood");
        feFlood.setAttributeNS(null, "flood-color", SVGUtils.svgRGBhex(backgroundColor.getRGB()));
        feFlood.setAttributeNS(null, "flood-opacity", SVGUtils.svgNumber(backgroundColor.getAlpha() / 255.0));
        feFlood.setAttributeNS(null, "result", "bg");

        Element feMerge = SVGUtils.createSVGElement(doc, "feMerge");
        Element feMergeNode = SVGUtils.createSVGElement(doc, "feMergeNode");
        feMergeNode.setAttributeNS(null, "in", "bg");
        Element feMergeNode2 = SVGUtils.createSVGElement(doc, "feMergeNode");
        feMergeNode2.setAttributeNS(null, "in", "SourceGraphic");

        feMerge.appendChild(feMergeNode);
        feMerge.appendChild(feMergeNode2);
        filter.appendChild(feFlood);
        filter.appendChild(feMerge);
        defs.appendChild(filter);
        parent.appendChild(defs);

        Element backgroundText = SVGUtils.createSVGElement(doc, "text");
        parent.appendChild(backgroundText);
        backgroundText.setAttributeNS(null, "filter", "url(#" + defID + ")");
        return backgroundText;
	}
}