package jplotter.renderers;

import java.util.LinkedList;

import jplotter.globjects.Lines;
import jplotter.globjects.Renderable;
import jplotter.globjects.Shader;

public abstract class GenericRenderer<T extends Renderable> implements Renderer {
	
	protected LinkedList<T> itemsToRender = new LinkedList<>();
	protected Shader shader;
	
	@Override
	public void render(int w, int h) {
		// TODO Auto-generated method stub
		
	}
	
	
	public GenericRenderer<T> addItemToRender(T item){
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
