package hageldave.jplotter.renderers;

import java.util.LinkedList;
import java.util.Objects;

import org.joml.Matrix3fc;
import org.joml.Matrix4f;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.globjects.Shader;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.util.GLUtils;

/**
 * The GenericRenderer class is an abstract {@link Renderer} intended
 * for rendering some kind of {@link Renderable} (e.g. Points).
 * It provides some typical attributes such as a List of Renderables to render,
 * a Shader object, a projection and view matrix.
 * It also implements the {@link #render(int, int)} method while exposing
 * a new interface for implementations of GenericRenderers that need
 * to implement {@link #renderStart(int, int)} {@link #renderItem(Renderable)}
 * and {@link #renderEnd()}.
 * 
 * @author hageldave
 * @param <T> the kind of renderable this GenericRenderer handles
 */
public abstract class GenericRenderer<T extends Renderable> implements Renderer, AdaptableView {
	
	protected LinkedList<T> itemsToRender = new LinkedList<>();
	protected Shader shader;
	protected float[] orthoMX = GLUtils.orthoMX(null,0, 1, 0, 1);
	protected Matrix4f viewMX = new Matrix4f();
	
	/**
	 * Executes the rendering procedure IF this {@link Renderer}s shader 
	 * is non null, view port width and height are greater zero and the
	 * list {@link #itemsToRender} is nonempty.
	 * The procedure goes as follows:
	 * <ol>
	 * <li>calling {@link Renderable#initGL()} on every Renderable</li>
	 * <li>binding the {@link #shader} ({@link Shader#bind()})</li>
	 * <li>setting the projection matrix {@link #orthoMX} to an
	 * orthographic projection on the area (0,0) to (w,h)</li> 
	 * <li>calling {@link #renderStart(int, int)}</li>
	 * <li>iterating over every Renderable, calling {@link Renderable#updateGL()}
	 * if {@link Renderable#isDirty()}, and then passing it to {@link #renderItem(Renderable)}</li>
	 * <li>calling {@link #renderEnd()} after iteration</li>
	 * <li>releasing the shader</li>
	 * </ol>
	 */
	@Override
	@GLContextRequired
	public void render(int w, int h) {
		if(Objects.nonNull(shader) && w>0 && h>0 && !itemsToRender.isEmpty()){
			// initialize all objects first
			for(T item: itemsToRender){
				item.initGL();
			}
			// bind shader
			shader.bind();
			// prepare for rendering (e.g. en/disable depth or blending and such)
			orthoMX = GLUtils.orthoMX(orthoMX, 0, w, 0, h);
			renderStart(w,h);
			// render every item
			for(T item: itemsToRender){
				if(item.isDirty()){
					// update items gl state if necessary
					item.updateGL();
				}
				renderItem(item);
			}
			// clean up after renering (e.g. en/disable depth or blending and such)
			renderEnd();
			shader.release();
		}
	}
	
	/**
	 * Is called during the {@link #render(int, int)} routine before 
	 * {@link #renderItem(Renderable)} is called.
	 * At this stage the {@link #shader} has already been bound, 
	 * the projection matrix {@link #orthoMX} been set
	 * and the items to render been GL initialized ({@link Renderable#initGL()}).
	 * <p>
	 * This method can for example be used to set GL properties like blending
	 * and corresponding blend function that is to be active during rendering items.
	 * 
	 * @param w view port width in pixels
	 * @param h view port height in pixels
	 */
	@GLContextRequired
	protected abstract void renderStart(int w, int h);

	/**
	 * Is called during the {@link #render(int, int)} routine after
	 * {@link #renderStart(int, int)} for every item contained in this renderer.
	 * <p>
	 * This method should take care of rendering the specified item, i.e.
	 * setting relevant shader parameters and issuing a GL draw call that
	 * will draw the item.
	 * 
	 * @param item to render
	 */
	@GLContextRequired
	protected abstract void renderItem(T item);
	
	/**
	 * Is called during the {@link #render(int, int)} routine after
	 * all items have been rendered ({@link #renderItem(Renderable)}).
	 * This will be called before the {@link #shader} is closed.
	 * <p>
	 * This method can for example be used to revert any GL property changes made
	 * during {@link #renderStart(int, int)}.
	 */
	@GLContextRequired
	protected abstract void renderEnd();

	/**
	 * Adds an item to this renderer's {@link #itemsToRender} list.
	 * The renderer will take care of calling {@link Renderable#initGL()} during
	 * its {@link #render(int, int)} method and will as well call
	 * {@link Renderable#updateGL()} if {@link Renderable#isDirty()}.
	 * 
	 * @param item to add
	 * @return this for chaining
	 */
	public GenericRenderer<T> addItemToRender(T item){
		if(!itemsToRender.contains(item))
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
	 * Closes and removes all items from this renderer's {@link #itemsToRender} list.
	 */
	@GLContextRequired
	public void deleteAllItems(){
		for(T item: itemsToRender)
			item.close();
		itemsToRender.clear();
	}
	
	/**
	 * @return the list of items to render.
	 */
	public LinkedList<T> getItemsToRender() {
		return itemsToRender;
	}
	
	/**
	 * Sets this renderers {@link #viewMX}.
	 */
	@Override
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx){
		this.viewMX.set(viewmx);
	}
	
}
