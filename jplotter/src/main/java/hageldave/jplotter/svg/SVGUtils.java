package hageldave.jplotter.svg;

import static org.apache.batik.anim.dom.SVGDOMImplementation.SVG_NAMESPACE_URI;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.misc.Glyph;

public class SVGUtils {
	
	private static final AtomicLong defIdCounter = new AtomicLong();

	public static Element createSVGElement(Document doc, String element){
		return doc.createElementNS(SVG_NAMESPACE_URI, element);
	}
	
	public static Element createSVGRect(Document doc, double x, double y, double w, double h){
		Element rect = createSVGElement(doc, "rect");
		rect.setAttributeNS(null, "x", ""+x);
		rect.setAttributeNS(null, "y", ""+y);
		rect.setAttributeNS(null, "width", ""+w);
		rect.setAttributeNS(null, "height", ""+h);
		return rect;
	}
	
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
	
	public static Element createSVGTriangle(Document doc, double x0, double y0, double x1, double y1, double x2, double y2){
		Element rect = createSVGElement(doc, "polygon");
		rect.setAttributeNS(null, "points", svgPoints(x0,y0,x1,y1,x2,y2));
		return rect;
	}
	
	public static String svgRGBhex(int argb){
		return '#'+Integer.toHexString(0xff000000 | argb).substring(2);
	}
	
	public static String cssRGBA(int argb){
		return "rgba("
				+Pixel.r_normalized(argb)+","
				+Pixel.g_normalized(argb)+","
				+Pixel.b_normalized(argb)+","
				+Pixel.a_normalized(argb)+")";
	}
	
	public static String documentToXMLString(Document doc){
		TranscoderInput input = new TranscoderInput(doc);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try(
				OutputStreamWriter osw = new OutputStreamWriter(baos, "UTF-8");
		){
			TranscoderOutput output = new TranscoderOutput(osw);
			new SVGTranscoder().transcode(input, output);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TranscoderException e) {
			e.printStackTrace();
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		String xml = "";
		try(Scanner sc = new Scanner(bais)){
			while(sc.hasNext()){
				xml += sc.nextLine();
				xml += System.lineSeparator();
			}
		}
		return xml;
	}
	
	public static void documentToXMLFile(Document doc, File file){
		TranscoderInput input = new TranscoderInput(doc);
		try(
				FileOutputStream fos = new FileOutputStream(file);
				OutputStreamWriter osfw = new OutputStreamWriter(fos, "UTF-8");
		){
			TranscoderOutput output = new TranscoderOutput(osfw);
			SVGTranscoder svgTranscoder = new SVGTranscoder();
			svgTranscoder.transcode(input, output);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TranscoderException e) {
			e.printStackTrace();
		}
	}
	
	public static Node getDefs(Document doc){
		return doc.getElementsByTagName("defs").item(0);
	}
	
	public static String newDefId(){
		return "def_"+Long.toString(defIdCounter.incrementAndGet(), 32);
	}
	
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
	
	public static String svgNumber(double x){
		String s = ""+x;
		if(s.contains(".")){
			s = s.substring(0, Math.min(s.length(), s.indexOf('.')+4));
		}
		return s;
	}
	
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
}
