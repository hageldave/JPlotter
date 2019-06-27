package hageldave.jplotter.svg;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The SVGRenderer interface defines the method
 * {@link #renderSVG(Document, Element, int, int)}
 * which 'renders' the SVGRenderer's content as
 * scalable vector graphics objects, i.e. appends 
 * SVG elements to the specified element.
 * 
 * @author hageldave
 */
public interface SVGRenderer {

	/**
	 * renders this SVGRenderers contents, that is creating 
	 * svg elements and appending them to the specified parent
	 * within the corresponding document.
	 * 
	 * @param doc the containing svg document to create elements with
	 * @param parent the parent element to which elements are to be appended
	 * @param w width of the current viewport
	 * @param h height of the current viewport
	 */
	public default void renderSVG(Document doc, Element parent, int w, int h){}
	
}
