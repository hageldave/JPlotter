package hageldave.jplotter.renderables;

import hageldave.jplotter.debugging.annotations.DebugGetter;
import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.renderers.GenericRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;

import java.awt.geom.Rectangle2D;

/**
 * Interface for an object that can be rendered by a {@link Renderer} e.g. the {@link GenericRenderer}.
 * It is intended for objects that contain openGL resources such as a {@link VertexArray} for example.
 * For this reason the interface extends the {@link AutoCloseable} interface for disposing of GL resources
 * on {@link #close()}.
 * Its state can be dirty ({@link #isDirty()}) which means that the GL resources are not in sync with the object and implies that
 * a call to the {@link #updateGL(boolean)} method is necessary before rendering.
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
	 * if true, indicates that a call to {@link #updateGL(boolean)} is necessary to sync 
	 * this objects GL resources to its current state.
	 * @return true if dirty
	 */
	public boolean isDirty();
	
	/**
	 * updates GL resources to match this objects state. 
	 * If requested precision differs from currently used, GL resources will
	 * be updated as well to use the requested precision.
	 * @param useGLDoublePrecision true when rendering with double precision
	 */
	@GLContextRequired
	public void updateGL(boolean useGLDoublePrecision);
	
	/**
	 * Reports on the Renderable's currently used precision of its GL resources.
	 * This may depend on the parameter used in the latest call to {@link #updateGL(boolean)}.
	 * Precision is not decided by the Renderable but by the corresponding {@link Renderer}.
	 * @return true if current GL resources (e.g vertex arrays) are using double precision
	 */
	@DebugGetter(ID = "isGLDoublePrecision")
	public boolean isGLDoublePrecision();
	
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

}
