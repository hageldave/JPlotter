package jplotter.renderers;

import jplotter.globjects.FBO;
import jplotter.globjects.Shader;
import jplotter.renderables.Renderable;

/**
 * The Renderer interface defines methods to 
 * initialize the renderer,
 * execute a rendering pass,
 * closing the renderer
 * and reporting whether the renderer is capable of drawing
 * picking color, ie.e drawing into the first and second color attachment
 * of an FBO.
 * 
 * @author hageldave
 */
public interface Renderer extends AutoCloseable {

	/**
	 * initializes this renderer, i.e. allocates GL resources
	 * such as a {@link Shader} or the resources of
	 * {@link Renderable}s it's using.
	 */
	public void glInit();
	
	/**
	 * renders this {@link Renderer}'s 'scene'.
	 * @param w width of the current viewport in pixels
	 * @param h height of the current viewport in pixels
	 */
	public void render(int w, int h);
	
	/**
	 * Disposes of any GL resources belonging to this object.
	 */
	public void close();
	
	/**
	 * @return true when this renderer can draw to the second
	 * color attachment of an {@link FBO} that is the picking color.
	 * This may be used to determine if the second color attachment
	 * can be enabled along side the first as draw buffer.
	 */
	public default boolean drawsPicking(){return false;}
	
}
