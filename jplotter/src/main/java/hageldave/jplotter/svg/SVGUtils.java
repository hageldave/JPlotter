package hageldave.jplotter.svg;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.font.FontProvider;
import hageldave.jplotter.misc.Glyph;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JLabel;

import static hageldave.jplotter.font.FontProvider.getUbuntuMonoFontAsBaseString;
import static java.awt.Font.BOLD;
import static java.awt.Font.ITALIC;
import static java.awt.Font.PLAIN;
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
		Element rect = createSVGElement(doc, "polygon");
		rect.setAttributeNS(null, "points", svgPoints(x0,y0,x1,y1,x2,y2));
		return rect;
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
		createFontDefinitionStyleElement(document);
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
	
	public static void createFontDefinitionStyleElement(Document document) {
		String styleID = "UbuntuMonoStyle";
		if(document.getElementById(styleID) != null) {
			return; // element is already present in document
		}
		
		// set Ubuntu Mono font
		Element styleElement = SVGUtils.createSVGElement(document, "style");
		styleElement.setAttributeNS(null, "id", styleID);
		styleElement.setAttributeNS(null, "type", "text/css"); 
		String fontface_css = new StringBuilder(1024*16)
				.append(System.lineSeparator())
				.append("@font-face { font-family:\"Ubuntu Mono\"; src: url(\"data:font/ttf;base64,")
				.append(getUbuntuMonoFontAsBaseString(PLAIN))
				.append("\") format(\"truetype\"); font-weight: normal; font-style: normal;}")
				.append(System.lineSeparator())

				.append("@font-face { font-family:\"Ubuntu Mono\"; src: url(\"data:font/ttf;base64,")
				.append(getUbuntuMonoFontAsBaseString(BOLD))
				.append("\") format(\"truetype\"); font-weight: bold; font-style: normal;}")
				.append(System.lineSeparator())

				.append("@font-face { font-family:\"Ubuntu Mono\"; src: url(\"data:font/ttf;base64,")
				.append(getUbuntuMonoFontAsBaseString(ITALIC))
				.append("\") format(\"truetype\"); font-weight: normal; font-style: italic;}")
				.append(System.lineSeparator())

				.append("@font-face { font-family:\"Ubuntu Mono\"; src: url(\"data:font/ttf;base64,")
				.append(getUbuntuMonoFontAsBaseString(BOLD | ITALIC))
				.append("\") format(\"truetype\"); font-weight: bold; font-style: italic;}")
				.append(System.lineSeparator())
				.toString();
		CDATASection cdatasection_fontface_css = document.createCDATASection(fontface_css);
		styleElement.appendChild(cdatasection_fontface_css);
		//					styleElement.setTextContent(fontface_css);
		// includes the ubuntu mono font licence
		String licenceString = FontProvider.getUbuntuMonoFontLicence();
		for (String line : licenceString.split("[\\r\\n]{2}")) {
			// remove "--" as those characters as they aren't allowed inside comments
			String cleanedLine = line.replaceAll("-", "");
			// create comment with cleaned line
			Comment comment = document.createComment(cleanedLine);
			styleElement.appendChild(comment);
		}
		document.getDocumentElement().appendChild(styleElement);
	}

}
