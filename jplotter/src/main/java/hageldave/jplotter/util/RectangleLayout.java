package hageldave.jplotter.util;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class RectangleLayout {

	static abstract class LayoutComp {
		int x,y;
		abstract Dimension getSize();
		abstract void apply();
	}
	
	ArrayList<LayoutComp> comps = new ArrayList<>();
	int width, height;
	
	public Dimension getLayoutSize(){
		return new Dimension(width, height);
	}
	
	public RectangleLayout addComponent(Supplier<Dimension> dim, BiConsumer<Integer, Integer> setLocation){
		this.comps.add(new LayoutComp() {
			@Override
			Dimension getSize() {
				return dim.get();
			}
			
			@Override
			void apply() {
				setLocation.accept(x, y);
			}
		});
		return this;
	}
	
	public RectangleLayout calculateFlowLayout(final int maxWidth, int hspace, int vspace){
		int currX=0, currY=0, lineHeight=0;
		this.width=this.height=0;
		for(int i=0; i<comps.size(); i++){
			LayoutComp c = comps.get(i);
			Dimension size = c.getSize();
			if(i==0){
				c.x=currX;
				c.y=currY;
				currX+=size.width+hspace;
				lineHeight = size.height;
				this.width = currX-hspace;
			} else {
				// enough space
				if(maxWidth-currX >= size.width){
					c.x=currX;
					c.y=currY;
					currX+=size.width+hspace;
					lineHeight = Math.max(lineHeight, size.height);
					this.width = Math.max(this.width, currX-hspace);
				// next line
				} else {
					currX=0;
					currY+=lineHeight+vspace;
					c.x=currX;
					c.y=currY;
					currX+=size.width+hspace;
					lineHeight = size.height;
					this.width = Math.max(this.width, currX-hspace);
				}
			}
		}
		this.height = currY+lineHeight;
		return this;
	}
	
	public RectangleLayout flipYAxis(int height){
		comps.forEach(c->c.y=height-c.y);
		return this;
	}
	
	public RectangleLayout translate(int dx, int dy){
		comps.forEach(c->{c.x+=dx;c.y+=dy;}); 
		return this;
	}
	
	public RectangleLayout apply(){
		comps.forEach(LayoutComp::apply);
		return this;
	}
	
}
