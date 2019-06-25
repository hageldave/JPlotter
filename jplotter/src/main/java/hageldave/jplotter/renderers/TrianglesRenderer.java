package hageldave.jplotter.renderers;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Utils;

/**
 * The TrianglesRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Triangles}.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class TrianglesRenderer extends GenericRenderer<Triangles> {
	
	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   vec3 pos = vec3(in_position,1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = projMX*vec4(pos,1);"
			+ NL + "   vColor = unpackARGB(in_colors.x);"
			+ NL + "   vPickColor = unpackARGB(in_colors.y);"
			+ NL + "}"
			+ NL
			;
	protected static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "in vec4 vColor;"
			+ NL + "in vec4 vPickColor;"
			+ NL + "uniform float alphaMultiplier;"
			+ NL + "void main() {"
			+ NL + "   frag_color = vec4(vColor.rgb, vColor.a*alphaMultiplier);"
			+ NL + "   pick_color = vPickColor;"
			+ NL + "}"
			+ NL
			;

	/**
	 * Creates the shader if not already created and 
	 * calls {@link Renderable#initGL()} for all items 
	 * already contained in this renderer.
	 * Items that are added later on will be initialized during rendering.
	 */
	@Override
	@GLContextRequired
	public void glInit() {
		if(Objects.isNull(shader)){
			shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
			itemsToRender.forEach(Renderable::initGL);
		}
	}

	/** 
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Triangles} contained in this
	 * renderer.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(shader)){
			shader.close();
		}
		deleteAllItems();
	}

	/**
	 * Disables {@link GL11#GL_DEPTH_TEST},
	 * enables {@link GL11#GL_BLEND}
	 * and sets {@link GL11#GL_SRC_ALPHA}, {@link GL11#GL_ONE_MINUS_SRC_ALPHA}
	 * as blend function.
	 */
	@Override
	@GLContextRequired
	protected void renderStart(int w, int h) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
		int loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewTransform");
		GL20.glUniform4f(loc, (float)translateX, (float)translateY, (float)scaleX, (float)scaleY);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Triangles item) {
		if(item.numTriangles() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, item.getGlobalAlphaMultiplier());
		// draw things
		item.bindVertexArray();
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, item.numTriangles()*3);
		item.releaseVertexArray();
	}

	/**
	 * disables {@link GL11#GL_BLEND},
	 * enables {@link GL11#GL_DEPTH_TEST}
	 */
	@Override
	@GLContextRequired
	protected void renderEnd() {
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		String svgTriangleStrategy = "SUBDIVIDE";
		
		Element mainGroup = SVGUtils.createSVGElement(doc, "g");
		parent.appendChild(mainGroup);
		
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
		
		for(Triangles tris : getItemsToRender()){
			Element trianglesGroup = SVGUtils.createSVGElement(doc, "g");
			mainGroup.appendChild(trianglesGroup);
			for(TriangleDetails tri : tris.getTriangleDetails()){
				double x0,y0, x1,y1, x2,y2;
				x0=tri.x0; y0=tri.y0; x1=tri.x1; y1=tri.y1; x2=tri.x2; y2=tri.y2;
				
				x0-=translateX; x1-=translateX; x2-=translateX;
				y0-=translateY; y1-=translateY; y2-=translateY;
				x0*=scaleX; x1*=scaleX; x2*=scaleX;
				y0*=scaleY; y1*=scaleY; y2*=scaleY;
				
				addSVGTriangle(doc, trianglesGroup, new double[]{x0,y0,x1,y1,x2,y2}, new int[]{tri.c0,tri.c1,tri.c2}, tris.getGlobalAlphaMultiplier(), svgTriangleStrategy, viewportRect);
			}
		}
	}
	
	static void addSVGTriangle(
			Document doc, 
			Element trianglesGroup, 
			double[] coords, 
			int[] colors, 
			float alphaMultiplier, 
			String strategy,
			Rectangle2D viewportRect)
	{
		int c0=colors[0], c1=colors[1], c2=colors[2];
		double x0,y0, x1,y1, x2,y2;
		{
			int i=0;
			x0=coords[i++]; y0=coords[i++]; x1=coords[i++]; y1=coords[i++]; x2=coords[i++]; y2=coords[i++];
			if( !(
					viewportRect.intersectsLine(x0, y0, x1, y1) || 
					viewportRect.intersectsLine(x0, y0, x2, y2) ||
					viewportRect.intersectsLine(x2, y2, x1, y1) ||
					triangleMayContain(x0, y0, x1, y1, x2, y2, viewportRect)
				)
			){
				return;
			}
		}
		
		if(c0==c1 && c1==c2){
			strategy = "";
		}
		
		switch (strategy) {
		case "SUBDIVIDE":{
			// test if subdivision is needed
			int edge = getSubdivisionEdge(x0, y0, x1, y1, x2, y2);
			if(edge==0){
				coords = new double[6];
				int c = Utils.interpolateColor(c0, c1, 0.5);
				// first subdivided triangle
				int i=0;
				coords[i++]=x0;coords[i++]=y0; coords[i++]=x0+(x1-x0)/2;coords[i++]=y0+(y1-y0)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c0; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
				// second subdivided triangle
				i=0;
				coords[i++]=x1;coords[i++]=y1; coords[i++]=x0+(x1-x0)/2;coords[i++]=y0+(y1-y0)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c1; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
			} else if(edge==1){
				coords = new double[6];
				int c = Utils.interpolateColor(c1, c2, 0.5);
				// first subdivided triangle
				int i=0;
				coords[i++]=x0;coords[i++]=y0; coords[i++]=x1+(x2-x1)/2;coords[i++]=y1+(y2-y1)/2; coords[i++]=x1;coords[i++]=y1;
				colors[0]=c0; colors[1]=c; colors[2]=c1;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
				// second subdivided triangle
				i=0;
				coords[i++]=x0;coords[i++]=y0; coords[i++]=x1+(x2-x1)/2;coords[i++]=y1+(y2-y1)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c0; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
			} else if(edge==2){
				coords = new double[6];
				int c = Utils.interpolateColor(c0, c2, 0.5);
				// first subdivided triangle
				int i=0;
				coords[i++]=x1;coords[i++]=y1; coords[i++]=x0+(x2-x0)/2;coords[i++]=y0+(y2-y0)/2; coords[i++]=x0;coords[i++]=y0;
				colors[0]=c1; colors[1]=c; colors[2]=c0;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
				// second subdivided triangle
				i=0;
				coords[i++]=x1;coords[i++]=y1; coords[i++]=x0+(x2-x0)/2;coords[i++]=y0+(y2-y0)/2; coords[i++]=x2;coords[i++]=y2;
				colors[0]=c1; colors[1]=c; colors[2]=c2;
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "SUBDIVIDE", viewportRect);
			} else {
				addSVGTriangle(doc, trianglesGroup, coords, colors, alphaMultiplier, "AVG_COLOR", viewportRect);
			}
		}
		break;
		case "AVG_COLOR":{
			int c = Utils.averageColor(c0,c1,c2);
			addSVGTriangle(doc, trianglesGroup, coords, new int[]{c,c,c}, alphaMultiplier, "", viewportRect);
		}
		break;
		default:
			Element triangle = SVGUtils.createSVGElement(doc, "path");
			trianglesGroup.appendChild(triangle);
			triangle.setAttributeNS(null, "d", "M "+SVGUtils.svgPoints(x0,y0) +" L "+SVGUtils.svgPoints(x1,y1,x2,y2)+" Z");
			triangle.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(c0));
			if(alphaMultiplier*Pixel.a_normalized(c0) != 1){
				triangle.setAttributeNS(null, "fill-opacity", 
						SVGUtils.svgNumber(alphaMultiplier*Pixel.a_normalized(c0)));
			}
		}
	}
	
	static int getSubdivisionEdge(double x0,double y0, double x1,double y1, double x2,double y2){
		double e0 = Math.hypot(x0-x1, y0-y1);
		double e1 = Math.hypot(x2-x1, y2-y1);
		double e2 = Math.hypot(x2-x0, y2-y0);
		double max = e0;
		int edge = 0;
		if(e1 > max){
			max = e1;
			edge = 1;
		}
		if(e2 > max){
			max = e2;
			edge = 2;
		}
		if(10 > max){
			edge = 3;
		}
		return edge;
	}
	
	static boolean triangleMayContain(double x0,double y0, double x1,double y1, double x2,double y2, Rectangle2D rect){
		double minx = Math.min(Math.min(x0, x1), x2);
		double miny = Math.min(Math.min(y0, y1), y2);
		double maxx = Math.max(Math.max(x0, x1), x2);
		double maxy = Math.max(Math.max(y0, y1), y2);
		return new Rectangle2D.Double(minx, miny, maxx-minx, maxy-miny).intersects(rect);
	}

}
