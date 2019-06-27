package hageldave.jplotter.canvas;

import java.util.Objects;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The BlankCanvas is an {@link FBOCanvas} that uses a single {@link Renderer} 
 * to draw its contents.
 * <p>
 * This can for example come in handy when a {@link Legend} should be drawn in
 * another canvas than the {@link CoordSysCanvas}. 
 * 
 * @author hageldave
 */
public class BlankCanvas extends FBOCanvas {
	private static final long serialVersionUID = 1L;

	protected Renderer renderer;

	@Override
	@GLContextRequired
	protected void paintToFBO(int width, int height) {
		if(Objects.nonNull(renderer) && width > 0 && height > 0){
			renderer.glInit();
			renderer.render(width, height);
		}
	}
	
	@Override
	protected void paintToSVG(Document doc, Element parent, int w, int h) {
		renderer.renderSVG(doc, parent, w, h);
	}

	@Override
	@GLContextRequired
	public void initGL() {
		super.initGL();
		if(Objects.nonNull(renderer))
			renderer.glInit();
	}

	@Override
	@GLContextRequired
	public void close() {
		super.close();
		if(Objects.nonNull(renderer))
			renderer.close();
		renderer = null;
	}
	
	/**
	 * Sets the renderer of this canvas.
	 * @param renderer to draw contents.
	 */
	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

}
