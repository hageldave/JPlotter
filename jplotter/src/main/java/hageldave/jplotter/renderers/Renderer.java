package hageldave.jplotter.renderers;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.globjects.FBO;
import hageldave.jplotter.globjects.Shader;
import hageldave.jplotter.renderables.Renderable;

/**
 * The Renderer interface defines methods to 
 * initialize the renderer,
 * execute a rendering pass,
 * closing the renderer.
 * <p>
 * <b>Implementation Notice:</b><br>
 * A renderer's fragment shader is obliged to output color for two
 * buffers, which are the two color attachments of an {@link FBO}.
 * These have to be written to <br>
 * {@code layout(location=0) out vec4 c1;} and <br>
 * {@code layout(location=1) out vec4 c2;}.<br>
 * When the renderer has no use for the picking attachment, {@code vec4(0,0,0,0)}
 * can be written as default.
 * 
 * @author hageldave
 */
public interface Renderer extends AutoCloseable {

	/**
	 * Initializes this renderer, i.e. allocates GL resources such as a 
	 * {@link Shader} or the resources of {@link Renderable}s it's using.
	 * This method should return early when already been called before.
	 */
	@GLContextRequired
	public void glInit();
	
	/**
	 * renders this {@link Renderer}'s 'scene'.
	 * @param w width of the current viewport in pixels
	 * @param h height of the current viewport in pixels
	 */
	@GLContextRequired
	public void render(int w, int h);
	
	/**
	 * Disposes of any GL resources belonging to this object.
	 */
	@GLContextRequired
	public void close();
	
}
