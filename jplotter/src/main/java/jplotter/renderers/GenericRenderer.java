package jplotter.renderers;

import java.util.LinkedList;
import java.util.Objects;

import jplotter.globjects.Renderable;
import jplotter.globjects.Shader;

public abstract class GenericRenderer<T extends Renderable> implements Renderer {
	
	protected LinkedList<T> itemsToRender = new LinkedList<>();
	protected Shader shader;
	
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
			shader.unbind();
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
	
	
}
