package hageldave.jplotter.renderables;

import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.renderers.GenericRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 * Interface for an object that can be rendered by a {@link Renderer} e.g. the {@link GenericRenderer}.
 * It is intended for objects that contain openGL resources such as a {@link VertexArray} for example.
 * For this reason the interface extends the {@link AutoCloseable} interface for disposing of GL resources
 * on {@link #close()}.
 * Its state can be dirty ({@link #isDirty()}) which means that the GL resources are not in sync with the object and implies that
 * a call to the {@link #updateGL()} method is necessary before rendering.
 * The {@link #initGL()} method has to be called once before the first rendering to allocate required GL resources.
 * 
 * @author hageldave
 */
public interface Renderable extends AutoCloseable {
	
	/** 
	 * Allocates GL resources for this Renderable such as a {@link VertexArray}.
	 * Should return early if already initialized.
	 */
	@GLContextRequired
	public void initGL();
	
	/**
	 * if true, indicates that a call to {@link #updateGL()} is necessary to sync 
	 * this objects GL resources to its current state.
	 * @return true if dirty
	 */
	public boolean isDirty();
	
	/**
	 * updates GL resources to match this objects state.
	 */
	@GLContextRequired
	public void updateGL();
	
	/**
	 * disposes of any GL resources belonging to this object.
	 */
	@GLContextRequired
	public void close();
	
	/**
	 * tests if this {@link Renderable} intersects or contains the specified
	 * rectangle.
	 * @param rect rectangle to test
	 * @return true when intersecting
	 */
	public boolean intersects(Rectangle2D rect);
	
	/**
	 * Indicates whether this Renderable is hidden i.e. will not be drawn.
	 * @return true when hidden
	 */
	public default boolean isHidden() {return false;}

	/**
	 * Returns the details of the each Renderable
	 * @return all RenderableDetails, if they exist (e.g. Text has no RenderableDetails)
	 */
	public default ArrayList<RenderableDetails> getRenderableDetails() {
		return null;
	};
}
