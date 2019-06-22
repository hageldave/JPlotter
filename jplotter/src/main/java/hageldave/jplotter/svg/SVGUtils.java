package hageldave.jplotter.svg;

import static org.apache.batik.anim.dom.SVGDOMImplementation.SVG_NAMESPACE_URI;

import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.svg2svg.SVGTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
	
	public static String svgRGB(int argb){
		return '#'+Integer.toHexString(0xff000000 | argb).substring(2);
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
			new SVGTranscoder().transcode(input, output);
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
}
