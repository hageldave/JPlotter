package hageldave.jplotter.renderers;

import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The LinesRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Lines}.
 * This renderer uses a geometry shader that extends a line primitive
 * into a quad of width that corresponds to the line width of the Lines
 * object.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class LinesRenderer extends GenericRenderer<Lines> {

	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in uint in_color;"
			+ NL + "layout(location = 2) in uint in_pick;"
			+ NL + "layout(location = 3) in float in_thickness;"
			+ NL + "layout(location = 4) in float in_pathlen;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "out vec4 vcolor;"
			+ NL + "out vec4 vpick;"
			+ NL + "out float vthickness;"
			+ NL + "out float vpathlen;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "void main() {"
			+ NL + "   vec3 pos = vec3(in_position,1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = vec4(pos,1);"
			+ NL + "   vcolor = unpackARGB(in_color);"
			+ NL + "   vpick =  unpackARGB(in_pick);"
			+ NL + "   vthickness = in_thickness;"
			+ NL + "   vpathlen = in_pathlen;"
			+ NL + "}"
			+ NL
			;
	protected static final String geometryShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(lines) in;"
			+ NL + "layout(triangle_strip,max_vertices=4) out;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform float linewidthMultiplier;"
			+ NL + "uniform bool roundposition;"
			+ NL + "in vec4 vcolor[];"
			+ NL + "in vec4 vpick[];"
			+ NL + "in float vthickness[];"
			+ NL + "in float vpathlen[];"
			+ NL + "out vec4 gcolor;"
			+ NL + "out vec4 gpick;"
			+ NL + "out float gpathlen;"
			
			+ NL + "float rnd(float f){return float(int(f+0.5));}"
			
			+ NL + "vec2 roundToIntegerValuedVec(vec2 v){"
			+ NL + "   return vec2(rnd(v.x),rnd(v.y));"
			+ NL + "}"
			
			+ NL + "void main() {"
			+ NL + "   vec2 p1 = gl_in[0].gl_Position.xy;"
			+ NL + "   vec2 p2 = gl_in[1].gl_Position.xy;"
			+ NL + "   vec2 dir = p1-p2;"
			+ NL + "   vec2 miterDir = normalize(vec2(dir.y, -dir.x));"
			+ NL + "   miterDir = miterDir * 0.5*linewidthMultiplier;"
			+ NL + "   vec2 p;"
			
			+ NL + "   p = p1+miterDir*vthickness[0];"
			+ NL + "   if(roundposition){p = roundToIntegerValuedVec(p);}"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[0];"
			+ NL + "   gpick = vpick[0];"
			+ NL + "   gpathlen = vpathlen[0];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   p = p1-miterDir*vthickness[0];"
			+ NL + "   if(roundposition){p = roundToIntegerValuedVec(p);}"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[0];"
			+ NL + "   gpick = vpick[0];"
			+ NL + "   gpathlen = vpathlen[0];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   p = p2+miterDir*vthickness[1];"
			+ NL + "   if(roundposition){p = roundToIntegerValuedVec(p);}"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[1];"
			+ NL + "   gpick = vpick[1];"
			+ NL + "   gpathlen = vpathlen[1];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   p = p2-miterDir*vthickness[1];"
			+ NL + "   if(roundposition){p = roundToIntegerValuedVec(p);}"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[1];"
			+ NL + "   gpick = vpick[1];"
			+ NL + "   gpathlen = vpathlen[1];"
			+ NL + "   EmitVertex();"
			
			+ NL + "   EndPrimitive();"
			+ NL + "}"
			+ NL
			;
	protected static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "in vec4 gcolor;"
			+ NL + "in vec4 gpick;"
			+ NL + "in float gpathlen;"
			+ NL + "uniform float alphaMultiplier;"
			+ NL + "uniform int[16] strokePattern;"
			+ NL + "uniform float strokeLength;"
			+ NL + "void main() {"
			+ NL + "   if(strokeLength > 0){"
			+ NL + "      float m = mod(gpathlen,strokeLength) / strokeLength;"
			+ NL + "      int idx = int(m*16);"
			+ NL + "      if(strokePattern[idx]==0){discard;}"
			+ NL + "   }"
			+ NL + "   frag_color = vec4(gcolor.rgb, gcolor.a*alphaMultiplier);"
			+ NL + "   pick_color = gpick;"
			+ NL + "}"
			+ NL
			;
	
	protected boolean viewHasChanged = true;
	private final int[] strokePattern = new int[16];
	
	
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
			shader = new Shader(vertexShaderSrc, geometryShaderSrc, fragmentShaderSrc);
			itemsToRender.forEach(Renderable::initGL);
		}
	}
	
	@GLContextRequired
	public void render(int w, int h) {
		if(!isEnabled()){
			return;
		}
		if(Objects.nonNull(shader) && w>0 && h>0 && !itemsToRender.isEmpty()){
			// initialize all objects first
			for(Lines item: itemsToRender){
				item.initGL();
			}
			// bind shader
			shader.bind();
			// prepare for rendering (e.g. en/disable depth or blending and such)
			orthoMX = GLUtils.orthoMX(orthoMX, 0, w, 0, h);
			renderStart(w,h);
			// render every item
			double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
			double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
			boolean viewHasChanged_ = this.viewHasChanged;
			this.viewHasChanged = false;
			for(Lines item: itemsToRender){
				if(item.isDirty() || (viewHasChanged_ && item.hasStrokePattern() )){
					// update items gl state if necessary
					item.updateGL(scaleX,scaleY);
				}
				renderItem(item);
			}
			// clean up after renering (e.g. en/disable depth or blending and such)
			renderEnd();
			shader.release();
		}
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
	protected void renderItem(Lines lines) {
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "linewidthMultiplier");
		GL20.glUniform1f(loc, lines.getGlobalThicknessMultiplier());
		// set projection matrix in shader
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, lines.getGlobalAlphaMultiplier());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "roundposition");
		GL20.glUniform1i(loc, lines.isVertexRoundingEnabled() ? 1:0);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "strokePattern");
		GL20.glUniform1iv(loc, transferBits(lines.getStrokePattern(), strokePattern));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "strokeLength");
		GL20.glUniform1f(loc, lines.hasStrokePattern() ? lines.getStrokeLength():0);
		// draw things
		lines.bindVertexArray();
		GL11.glDrawArrays(GL11.GL_LINES, 0, lines.numSegments()*2);
		lines.releaseVertexArray();
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
	public void setView(Rectangle2D view) {
		super.setView(view);
		this.viewHasChanged = true;
	}

	/**
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Lines} contained in this
	 * renderer.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(shader))
			shader.close();
		deleteAllItems();
	}

	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		Element mainGroup = SVGUtils.createSVGElement(doc, "g");
		parent.appendChild(mainGroup);
		
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
		
		for(Lines lines : getItemsToRender()){
			Element linesGroup = SVGUtils.createSVGElement(doc, "g");
			// TODO: adapt SVG to variable segment sizes
			linesGroup.setAttributeNS(null, "stroke-width", ""+lines.getGlobalThicknessMultiplier());
			mainGroup.appendChild(linesGroup);
			for(SegmentDetails seg : lines.getSegments()){
				double x1,y1,x2,y2;
				x1=seg.p0.getX(); y1=seg.p0.getY(); x2=seg.p1.getX(); y2=seg.p1.getY();
				
				x1-=translateX; x2-=translateX;
				y1-=translateY; y2-=translateY;
				x1*=scaleX; x2*=scaleX;
				y1*=scaleY; y2*=scaleY;
				
				if(!viewportRect.intersectsLine(x1, y1, x2, y2)){
					continue;
				}
				
				Element segment = SVGUtils.createSVGElement(doc, "polyline");
				linesGroup.appendChild(segment);
				
				segment.setAttributeNS(null, "points", SVGUtils.svgPoints(x1,y1,x2,y2));
				if(seg.color0 == seg.color1){
					segment.setAttributeNS(null, "stroke", SVGUtils.svgRGBhex(seg.color0.getAsInt()));
					segment.setAttributeNS(null, "stroke-opacity", SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color0.getAsInt())));
				} else {
					// create gradient for line
					Node defs = SVGUtils.getDefs(doc);
					Element gradient = SVGUtils.createSVGElement(doc, "linearGradient");
					defs.appendChild(gradient);
					String defID = SVGUtils.newDefId();
					gradient.setAttributeNS(null, "id", defID);
					gradient.setAttributeNS(null, "x1", SVGUtils.svgNumber(x1));
					gradient.setAttributeNS(null, "y1", SVGUtils.svgNumber(y1));
					gradient.setAttributeNS(null, "x2", SVGUtils.svgNumber(x2));
					gradient.setAttributeNS(null, "y2", SVGUtils.svgNumber(y2));
					gradient.setAttributeNS(null, "gradientUnits", "userSpaceOnUse");
					Element stop1 = SVGUtils.createSVGElement(doc, "stop");
					gradient.appendChild(stop1);
					stop1.setAttributeNS(null, "offset", "0%");
					stop1.setAttributeNS(null, "style", 
							"stop-color:"+SVGUtils.svgRGBhex(seg.color0.getAsInt())+";"+
							"stop-opacity:"+SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color0.getAsInt())));
					Element stop2 = SVGUtils.createSVGElement(doc, "stop");
					gradient.appendChild(stop2);
					stop2.setAttributeNS(null, "offset", "100%");
					stop2.setAttributeNS(null, "style", 
							"stop-color:"+SVGUtils.svgRGBhex(seg.color1.getAsInt())+";"+
							"stop-opacity:"+SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color1.getAsInt())));
					
					// use gradient for line stroke
					segment.setAttributeNS(null, "stroke", "url(#"+defID+")");
				}
			}
		}
	}
	
	protected static int[] transferBits(short bits, int[] target){
		for(int i = 0; i < 16; i++){
			target[15-i] = (bits >> i) & 0b1;
		}
		return target;
	}
}
