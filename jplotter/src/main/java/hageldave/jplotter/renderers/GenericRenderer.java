package hageldave.jplotter.renderers;

import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.Utils;

import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Objects;

/**
 * The GenericRenderer class is an abstract {@link Renderer} intended
 * for rendering some kind of {@link Renderable} (e.g. Points).
 * It provides some typical attributes such as a List of Renderables to render,
 * a Shader object, a projection and view matrix.
 * It also implements the {@link #render(int, int, int, int)} method while exposing
 * a new interface for implementations of GenericRenderers that need
 * to implement {@link #renderStart(int, int, Shader)} {@link #renderItem(Renderable, Shader)}
 * and {@link #renderEnd()}.
 * 
 * @author hageldave
 * @param <T> the kind of renderable this GenericRenderer handles
 */
public abstract class GenericRenderer<T extends Renderable> implements Renderer, AdaptableView, GLDoublePrecisionSupport {
	
	protected LinkedList<T> itemsToRender = new LinkedList<>();
	protected Shader shaderF;
	protected Shader shaderD;
	protected float[] orthoMX = GLUtils.orthoMX(null,0, 1, 0, 1);
	protected Rectangle2D view = null;
	protected boolean isEnabled = true;
	protected boolean isGLDoublePrecisionEnabled = false;
	
	/**
	 * Executes the rendering procedure IF this {@link Renderer}s shader 
	 * is non null, view port width and height are greater zero and the
	 * list {@link #itemsToRender} is nonempty.
	 * The procedure goes as follows:
	 * <ol>
	 * <li>calling {@link Renderable#initGL()} on every Renderable</li>
	 * <li>binding the shader ({@link #shaderF} or {@link #shaderD}} using {@link Shader#bind()})</li>
	 * <li>setting the projection matrix {@link #orthoMX} to an
	 * orthographic projection on the area (0,0) to (w,h)</li> 
	 * <li>calling {@link #renderStart(int, int, Shader)}</li>
	 * <li>iterating over every Renderable, calling {@link Renderable#updateGL(boolean)}
	 * if {@link Renderable#isDirty()}, and then passing it to {@link #renderItem(Renderable, Shader)}</li>
	 * <li>calling {@link #renderEnd()} after iteration</li>
	 * <li>releasing the shader</li>
	 * </ol>
	 */
	@Override
	@GLContextRequired
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled()){
			return;
		}
		Shader shader = getShader();
		boolean useDoublePrecision = shader == shaderD;
		if(Objects.nonNull(shader) && w>0 && h>0 && !itemsToRender.isEmpty()){
			// initialize all objects first
			for(T item: itemsToRender){
				item.initGL();
			}
			// bind shader
			shader.bind();
			// prepare for rendering (e.g. en/disable depth or blending and such)
			orthoMX = GLUtils.orthoMX(orthoMX, 0, w, 0, h);
			renderStart(w,h, shader);
			// render every item
			for(T item: itemsToRender){
				if(item.isHidden())
					continue;
				if(item.isDirty() || item.isGLDoublePrecision()!=useDoublePrecision){
					// update items gl state if necessary
					item.updateGL(useDoublePrecision);
				}
				renderItem(item, shader);
			}
			// clean up after renering (e.g. en/disable depth or blending and such)
			renderEnd();
			shader.release();
		}
	}
	
	protected Shader getShader() {
		return (isGLDoublePrecisionEnabled && Objects.nonNull(shaderD)) ? shaderD:shaderF; 
	}
	
	/**
	 * Is called during the {@link #render(int, int, int, int)} routine before 
	 * {@link #renderItem(Renderable, Shader)} is called.
	 * At this stage the shader has already been bound, 
	 * the projection matrix {@link #orthoMX} been set
	 * and the items to render been GL initialized ({@link Renderable#initGL()}).
	 * <p>
	 * This method can for example be used to set GL properties like blending
	 * and corresponding blend function that is to be active during rendering items.
	 * 
	 * @param w view port width in pixels
	 * @param h view port height in pixels
	 * @param shader shader in use
	 */
	@GLContextRequired
	protected abstract void renderStart(int w, int h, Shader shader);

	/**
	 * Is called during the {@link #render(int, int, int, int)} routine after
	 * {@link #renderStart(int, int, Shader)} for every item contained in this renderer.
	 * <p>
	 * This method should take care of rendering the specified item, i.e.
	 * setting relevant shader parameters and issuing a GL draw call that
	 * will draw the item.
	 * 
	 * @param item to render
	 * @param shader in use (float or double precision shader)
	 */
	@GLContextRequired
	protected abstract void renderItem(T item, Shader shader);
	
	/**
	 * Is called during the {@link #render(int, int, int, int)} routine after
	 * all items have been rendered ({@link #renderItem(Renderable, Shader)}).
	 * This will be called before the shader is closed.
	 * <p>
	 * This method can for example be used to revert any GL property changes made
	 * during {@link #renderStart(int, int, Shader)}.
	 */
	@GLContextRequired
	protected abstract void renderEnd();

	/**
	 * Adds an item to this renderer's {@link #itemsToRender} list.
	 * The renderer will take care of calling {@link Renderable#initGL()} during
	 * its {@link #render(int, int, int, int)} method and will as well call
	 * {@link Renderable#updateGL(boolean)} if {@link Renderable#isDirty()}.
	 * 
	 * @param item to add
	 * @return this for chaining
	 */
	public GenericRenderer<T> addItemToRender(T item){
		if(!itemsToRender.contains(item))
			// add item to render chain
			itemsToRender.add(item);
		return this;
	}

	/**
	 * Removes an item from this renderer's {@link #itemsToRender} list.
	 * @param item to remove
	 * @return true when successfully removed, else false (e.g. when not contained in list)
	 */
	public boolean removeItemToRender(T item){
		return itemsToRender.remove(item);
	}
	
	/**
	 * Closes all items in this renderer's {@link #itemsToRender} list.
	 */
	@GLContextRequired
	public void closeAllItems(){
		for(T item: itemsToRender)
			item.close();
	}
	
	@Override
	public void setEnabled(boolean enable) {
		this.isEnabled = enable;
	}
	
	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/**
	 * Enables/Disables GL rendering with double precision. Default is single precision.
	 * This requires support for GLSL 4.10 and OpenGL 4.1.
	 * <p>
	 * <u>Implementation Note</u><br>
	 * The GenericRenderer class provides two attributes of type {@link Shader}, where {@link #shaderF}
	 * is the default shader using single precision, and {@link #shaderD} is the double precision shader.
	 * An implementation of GenericRenderer has to support single precision but double precision is optional.
	 * In case there is no double precision support, the {@link #shaderD} attribute is never set (stays null).
	 * The method {@link #getShader()} returns shaderD iff it is non-null and {@link #isGLDoublePrecisionEnabled()}.
	 *  
	 * @param enable true when enabling
	 */
	@Override
	public void setGLDoublePrecisionEnabled(boolean enable) {
		this.isGLDoublePrecisionEnabled = enable;
	}
	
	/**
	 * @return true when GL double precision rendering is enabled.
	 * @see #setGLDoublePrecisionEnabled(boolean)
	 */
	public boolean isGLDoublePrecisionEnabled() {
		return isGLDoublePrecisionEnabled;
	}
	
	/**
	 * @return the list of items to render.
	 */
	public LinkedList<T> getItemsToRender() {
		return itemsToRender;
	}

	@Override
	public void setView(Rectangle2D view){
		this.view = Objects.isNull(view) ? null:Utils.copy(view);
	}
	
}
