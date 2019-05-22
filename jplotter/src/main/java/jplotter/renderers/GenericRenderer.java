package jplotter.renderers;

import java.util.LinkedList;
import java.util.Objects;

import org.joml.Matrix3fc;
import org.joml.Matrix4f;

import jplotter.globjects.Shader;
import jplotter.renderables.Renderable;
import jplotter.util.GLUtils;


public abstract class GenericRenderer<T extends Renderable> implements Renderer, AdaptableView {
	
	protected LinkedList<T> itemsToRender = new LinkedList<>();
	protected Shader shader;
	protected float[] orthoMX = GLUtils.orthoMX(null,0, 1, 0, 1);
	protected Matrix4f viewMX = new Matrix4f();
	
	@Override
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
	
	
	protected abstract void renderStart(int w, int h);

	protected abstract void renderItem(T item);
	
	protected abstract void renderEnd();



	public GenericRenderer<T> addItemToRender(T item){
		if(!itemsToRender.contains(item))
			itemsToRender.add(item);
		return this;
	}
	
	public boolean removeItemToRender(T item){
		return itemsToRender.remove(item);
	}
	
	public void deleteAllItems(){
		for(T item: itemsToRender)
			item.close();
		itemsToRender.clear();
	}
	
	public LinkedList<T> getItemsToRender() {
		return itemsToRender;
	}
	
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx){
		this.viewMX.set(viewmx);
	}
	
}
