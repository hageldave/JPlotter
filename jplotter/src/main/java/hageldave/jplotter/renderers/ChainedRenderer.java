package hageldave.jplotter.renderers;

import java.awt.geom.Rectangle2D;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The chained renderer is used to realize the methods {@link Renderer#withAppended(Renderer)}
 * and {@link Renderer#withPrepended(Renderer)}.
 * It consists of two other {@link Renderer}s which it executes after each other.
 * All {@link Renderer} and {@link AdaptableView} method calls are delegated to both.
 * 
 * @author hageldave
 */
public class ChainedRenderer implements Renderer, AdaptableView {

	protected Renderer r1,r2;
	
	public ChainedRenderer(Renderer r1, Renderer r2) {
		this.r1 = r1;
		this.r2 = r2;
	}
	
	@Override
	public void setView(Rectangle2D rect) {
		if(r1 instanceof AdaptableView)
			((AdaptableView) r1).setView(rect);
		if(r2 instanceof AdaptableView)
			((AdaptableView) r2).setView(rect);
	}

	@Override
	@GLContextRequired
	public void glInit() {
		r1.glInit();
		r2.glInit();
	}

	@Override
	@GLContextRequired
	public void render(int w, int h) {
		r1.render(w, h);
		r2.render(w, h);
	}

	@Override
	@GLContextRequired
	public void close() {
		if(r1 != null)
			r1.close();
		if(r2 != null)
			r2.close();
		r1=r2=null;
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		r1.renderSVG(doc, parent, w, h);
		r2.renderSVG(doc, parent, w, h);
	}
	
	
}
