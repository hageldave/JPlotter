package jplotter;

import java.util.LinkedHashMap;

import jplotter.renderers.Renderer;

public class RendererCanvas extends FBOCanvas {

	LinkedHashMap<String, Renderer> renderQueue;
	
	
	public boolean addRenderer(String rid, Renderer r){
		if(renderQueue.containsKey(rid)){
			return false;
		} else {
			renderQueue.put(rid, r);
			return true;
		}
	}
	
	public Renderer removeRenderer(String rid, Renderer r){
		return renderQueue.remove(rid);
	}
	
	@Override
	public void paintToFBO(int width, int height) {
		renderQueue.forEach((String rid, Renderer r)->{
			r.render(width, height);
		});
	}

}
