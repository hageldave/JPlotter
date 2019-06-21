package hageldave.jplotter.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface SVGRenderer {

	public default void renderSVG(Document doc, Element parent, int w, int h){}
	
}
