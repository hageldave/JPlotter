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
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.ShaderRegistry;
import hageldave.jplotter.util.Utils;

/**
 * The CurvesRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Curves}.
 * This renderer uses a geometry shader that extends a line primitive
 * into a quad of width that corresponds to the line width of the Curves
 * object. The whole Bezier curve is approximated by a sequence of such quads.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class CurvesRenderer extends GenericRenderer<Curves> {

	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec4 in_position;"
			+ NL + "layout(location = 1) in uint in_color;"
			+ NL + "layout(location = 2) in uint in_pick;"
			+ NL + "layout(location = 3) in float in_thickness;"
			+ NL + "layout(location = 4) in float in_pathlen;"
			+ NL + "layout(location = 5) in float in_param;"
			+ NL + "out vec4 vcolor;"
			+ NL + "out vec4 vpick;"
			+ NL + "out float vthickness;"
			+ NL + "out float vpathlen;"
			+ NL + "out float vparam;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"

			+ NL + "void main() {"
			/* we forward in_position to geometry shader since in_position contains two vec2 */
			+ NL + "   gl_Position = in_position;"
			+ NL + "   vcolor = unpackARGB(in_color);"
			+ NL + "   vpick =  unpackARGB(in_pick);"
			+ NL + "   vthickness = in_thickness;"
			+ NL + "   vpathlen = in_pathlen;"
			+ NL + "   vparam = in_param;"
			+ NL + "}"
			+ NL
			;
	protected static final String geometryShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(lines) in;"
			+ NL + "/* maxcomps=1024, we write 4+4+4+1+1=14 per vertex, so we can output 1024/14=73 vertices */"
			+ NL + "layout(triangle_strip,max_vertices=73) out;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform float linewidthMultiplier;"
			+ NL + "in vec4 vcolor[];"
			+ NL + "in vec4 vpick[];"
			+ NL + "in float vthickness[];"
			+ NL + "in float vpathlen[];"
			+ NL + "in float vparam[];"
			+ NL + "out vec4 gcolor;"
			+ NL + "out vec4 gpick;"
			+ NL + "out float gpathlen;"
			+ NL + ""
			+ NL + "vec2 transformToView(vec2 v){"
			+ NL + "   vec3 pos = vec3(v,1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   return pos.xy;"
			+ NL + "}"
			+ NL + "vec4 bezier(vec2 p1, vec2 p2, vec2 cp1, vec2 cp2, float t){"
			+ NL + "   float t_ = 1-t;"
			+ NL + "   float t2 = t*t;"
			+ NL + "   float t2_= t_*t_;"
			+ NL + "   float t23 = 3*t2;"
			+ NL + "   float t23_= 3*t2_;"
			+ NL + "   vec2 v1 = (t2_* t_) * p1;"
			+ NL + "   vec2 v2 = (t2 * t ) * p2;"
			+ NL + "   vec2 v3 = (t23_* t) * cp1;"
			+ NL + "   vec2 v4 = (t_* t23) * cp2;"
			+ NL + "   vec2 dv1 = (t23_) * (cp1-p1);"
			+ NL + "   vec2 dv2 = (t23 ) * (p2-cp2);"
			+ NL + "   vec2 dv3 = (6 * t_* t ) * (cp2-cp1);"
			+ NL + "   return vec4(v1+v2+v3+v4, dv1+dv2+dv3);"
			+ NL + "}"
			+ NL + ""
			+ NL + "vec2 d_bezier(vec2 p1, vec2 p2, vec2 cp1, vec2 cp2, float t){"
			+ NL + "   float t_ = 1-t;"
			+ NL + "   float t2 = t*t;"
			+ NL + "   float t2_= t_*t_;"
			+ NL + "   vec2 dv1 = (3*t2_) * (cp1-p1);"
			+ NL + "   vec2 dv2 = (3*t2 ) * (p2-cp2);"
			+ NL + "   vec2 dv3 = (6 * t_* t ) * (cp2-cp1);"
			+ NL + "   return dv1+dv2+dv3;"
			+ NL + "}"
			+ NL + ""
			+ NL + "void main() {"
			+ NL + "   vec2 p1 = gl_in[0].gl_Position.xy;"
			+ NL + "   vec2 p2 = gl_in[0].gl_Position.zw;"
			+ NL + "   vec2 cp1 = gl_in[1].gl_Position.xy;"
			+ NL + "   vec2 cp2 = gl_in[1].gl_Position.zw;"
			+ NL + "   p1 = transformToView(p1);"
			+ NL + "   p2 = transformToView(p2);"
			+ NL + "   cp1 = transformToView(cp1);"
			+ NL + "   cp2 = transformToView(cp2);"
			+ NL + "   "
			+ NL + "   vec4 start = bezier(p1,p2,cp1,cp2, vparam[0]);"
			+ NL + "   vec4 end   = bezier(p1,p2,cp1,cp2, vparam[1]);"
			+ NL + "   p1 = start.xy; p2=end.xy;"
			+ NL + "   float extend = 0.5*linewidthMultiplier;"
			+ NL + "   "
			+ NL + "   vec2 dir1 = normalize(start.zw);"
			+ NL + "   vec2 dir2 = normalize(p2-p1);"
			+ NL + "   vec2 q = p1;"
			+ NL + "   "
			+ NL + "   // curve normal will be used to form the joint,"
			+ NL + "   // but has to be elongated to achieve target thickness"
			+ NL + "   vec2 normal = vec2(dir1.y, -dir1.x);"
			+ NL + "   vec2 miter = vec2(dir2.y, -dir2.x);"
			+ NL + "   float extra = dot(normal,miter);"
			+ NL + "   extra = abs(extra) < 0.4 ? 1.0:1.0/extra;"
			+ NL + "   "
			+ NL + "   vec2 p = q+extend*extra*normal;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[0];"
			+ NL + "   gpick = vpick[0];"
			+ NL + "   gpathlen = vpathlen[0];"
			+ NL + "   EmitVertex();"
			+ NL + "   "
			+ NL + "   p = q-extend*extra*normal;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[0];"
			+ NL + "   gpick = vpick[0];"
			+ NL + "   gpathlen = vpathlen[0];"
			+ NL + "   EmitVertex();"
			+ NL + "   "
			+ NL + "   dir1 = -normalize(end.zw);"
			+ NL + "   q = p2;"
			+ NL + "   "
			+ NL + "   "
			+ NL + "   normal = vec2(dir1.y, -dir1.x);"
			+ NL + "   extra = dot(normal,miter);"
			+ NL + "   extra = abs(extra) < 0.4 ? 1.0:1.0/extra;"
			+ NL + "   "
			+ NL + "   "
			+ NL + "   p = q+extend*extra*normal;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[1];"
			+ NL + "   gpick = vpick[1];"
			+ NL + "   gpathlen = vpathlen[1];"
			+ NL + "   EmitVertex();"
			+ NL + "   "
			+ NL + "   p = q-extend*extra*normal;"
			+ NL + "   gl_Position = projMX*vec4(p,0,1);"
			+ NL + "   gcolor = vcolor[1];"
			+ NL + "   gpick = vpick[1];"
			+ NL + "   gpathlen = vpathlen[1];"
			+ NL + "   EmitVertex();"
			+ NL + "   "
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
	protected int preVpW = 0;
	protected int preVpH = 0;
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
			shader = ShaderRegistry.getOrCreateShader(this.getClass().getName(),()->new Shader(vertexShaderSrc, geometryShaderSrc, fragmentShaderSrc));
			itemsToRender.forEach(Renderable::initGL);
		}
	}

	@Override
	@GLContextRequired
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled()){
			return;
		}
		boolean vpHasChanged = w != preVpW || h != preVpH;
		if(Objects.nonNull(shader) && w>0 && h>0 && !itemsToRender.isEmpty()){
			// initialize all objects first
			for(Curves item: itemsToRender){
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
			for(Curves item: itemsToRender){
				if(item.isDirty() || viewHasChanged_ || vpHasChanged){
					// update items gl state if necessary
					item.updateGL(scaleX,scaleY);
				}
				if(!item.isHidden()){
					renderItem(item);
				}
			}
			// clean up after renering (e.g. en/disable depth or blending and such)
			renderEnd();
			shader.release();
		}
		preVpW = w;
		preVpH = h;
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
	protected void renderItem(Curves lines) {
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "linewidthMultiplier");
		GL20.glUniform1f(loc, lines.getGlobalThicknessMultiplier());
		// set projection matrix in shader
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, lines.getGlobalAlphaMultiplier());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "strokePattern");
		GL20.glUniform1iv(loc, transferBits(lines.getStrokePattern(), strokePattern));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "strokeLength");
		GL20.glUniform1f(loc, lines.hasStrokePattern() ? lines.getStrokeLength():0);
		// draw things
		lines.bindVertexArray();
		GL11.glDrawArrays(GL11.GL_LINES, 0, lines.getNumEffectiveSegments()*2);
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
		boolean sameView = Objects.equals(view, this.view);
		super.setView(view);
		this.viewHasChanged = !sameView;
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
			ShaderRegistry.handbackShader(shader);
		shader = null;
		closeAllItems();
	}


//	@Override
//	public void renderSVG(Document doc, Element parent, int w, int h) {
//		if(!isEnabled()){
//			return;
//		}
//		Element mainGroup = SVGUtils.createSVGElement(doc, "g");
//		parent.appendChild(mainGroup);
//
//		double translateX = Objects.isNull(view) ? 0:view.getX();
//		double translateY = Objects.isNull(view) ? 0:view.getY();
//		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
//		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
//
//		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
//
//		for(Curves lines : getItemsToRender()){
//			if(lines.isHidden() || lines.getStrokePattern()==0){
//				// line is invisible
//				continue;
//			}
//			Element linesGroup = SVGUtils.createSVGElement(doc, "g");
//			linesGroup.setAttributeNS(null, "stroke-width", "0");
//			mainGroup.appendChild(linesGroup);
//			double dist = 0;
//			double prevX = 0;
//			double prevY = 0;
//			for(SegmentDetails seg : lines.getSegments()){
//				double x1,y1,x2,y2;
//				x1=seg.p0.getX(); y1=seg.p0.getY(); x2=seg.p1.getX(); y2=seg.p1.getY();
//
//				x1-=translateX; x2-=translateX;
//				y1-=translateY; y2-=translateY;
//				x1*=scaleX; x2*=scaleX;
//				y1*=scaleY; y2*=scaleY;
//
//				// path length calculations
//				double dx = x2-x1;
//				double dy = y2-y1;
//				double len = Utils.hypot(dx, dy);
//				double l1,l2;
//				if(prevX==x1 && prevY==y1){
//					l1 = dist;
//					l2 = dist+len;
//					dist += len;
//					dist = dist % lines.getStrokeLength();
//				} else {
//					l1 = 0;
//					l2 = len;
//					dist = len;
//				}
//				prevX = x2;
//				prevY = y2;
//				
//				if(lines.isVertexRoundingEnabled()){
//					x1 = (int)(x1+0.5);
//					x2 = (int)(x2+0.5);
//					y1 = (int)(y1+0.5);
//					y2 = (int)(y2+0.5);
//				}
//
//				// visibility check
//				if(!viewportRect.intersectsLine(x1, y1, x2, y2)){
//					continue;
//				}
//
//				// miter vector stuff
//				double normalize = 1/len;
//				double miterX =  dy*normalize*0.5;
//				double miterY = -dx*normalize*0.5;
//				double t1 = seg.thickness0.getAsDouble()*lines.getGlobalThicknessMultiplier();
//				double t2 = seg.thickness1.getAsDouble()*lines.getGlobalThicknessMultiplier();
//
//				
//				String defID = "";
//				if(seg.color0.getAsInt() != seg.color1.getAsInt()){
//					// create gradient for line
//					Node defs = SVGUtils.getDefs(doc);
//					Element gradient = SVGUtils.createSVGElement(doc, "linearGradient");
//					defs.appendChild(gradient);
//					defID = SVGUtils.newDefId();
//					gradient.setAttributeNS(null, "id", defID);
//					gradient.setAttributeNS(null, "x1", SVGUtils.svgNumber(x1));
//					gradient.setAttributeNS(null, "y1", SVGUtils.svgNumber(y1));
//					gradient.setAttributeNS(null, "x2", SVGUtils.svgNumber(x2));
//					gradient.setAttributeNS(null, "y2", SVGUtils.svgNumber(y2));
//					gradient.setAttributeNS(null, "gradientUnits", "userSpaceOnUse");
//					Element stop1 = SVGUtils.createSVGElement(doc, "stop");
//					gradient.appendChild(stop1);
//					stop1.setAttributeNS(null, "offset", "0%");
//					stop1.setAttributeNS(null, "style", 
//							"stop-color:"+SVGUtils.svgRGBhex(seg.color0.getAsInt())+";"+
//									"stop-opacity:"+SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color0.getAsInt())));
//					Element stop2 = SVGUtils.createSVGElement(doc, "stop");
//					gradient.appendChild(stop2);
//					stop2.setAttributeNS(null, "offset", "100%");
//					stop2.setAttributeNS(null, "style", 
//							"stop-color:"+SVGUtils.svgRGBhex(seg.color1.getAsInt())+";"+
//									"stop-opacity:"+SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color1.getAsInt())));
//				}
//				
//				if(!lines.hasStrokePattern()){
//					Element segment = SVGUtils.createSVGElement(doc, "polygon");
//					linesGroup.appendChild(segment);
//					segment.setAttributeNS(null, "points", SVGUtils.svgPoints(
//							x1+miterX*t1,y1+miterY*t1, x2+miterX*t2,y2+miterY*t2, 
//							x2-miterX*t2,y2-miterY*t2, x1-miterX*t1,y1-miterY*t1));
//					if(seg.color0.getAsInt() == seg.color1.getAsInt()){
//						segment.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(seg.color0.getAsInt()));
//						segment.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color0.getAsInt())));
//					} else {
//						// use gradient for line stroke
//						segment.setAttributeNS(null, "fill", "url(#"+defID+")");
//					}
//				} else {
//					double[] strokeInterval = findStrokeInterval(l1, lines.getStrokeLength(), lines.getStrokePattern());
//					while(strokeInterval[0] < l2){
//						double start = strokeInterval[0];
//						double end = Math.min(strokeInterval[1], l2);
//						// interpolation factors
//						double m1 = Math.max((start-l1)/(l2-l1), 0);
//						double m2 = (end-l1)/(l2-l1);
//						// interpolate miters
//						double t1_ = t1*(1-m1)+t2*m1;
//						double t2_ = t1*(1-m2)+t2*m2;
//						// interpolate segment
//						double x1_ = x1 + dx*m1;
//						double x2_ = x1 + dx*m2;
//						double y1_ = y1 + dy*m1;
//						double y2_ = y1 + dy*m2;
//
//						Element segment = SVGUtils.createSVGElement(doc, "polygon");
//						linesGroup.appendChild(segment);
//						segment.setAttributeNS(null, "points", SVGUtils.svgPoints(
//								x1_+miterX*t1_,y1_+miterY*t1_, x2_+miterX*t2_,y2_+miterY*t2_, 
//								x2_-miterX*t2_,y2_-miterY*t2_, x1_-miterX*t1_,y1_-miterY*t1_));
//
//						strokeInterval = findStrokeInterval(strokeInterval[2], lines.getStrokeLength(), lines.getStrokePattern());
//
//						if(seg.color0.getAsInt() == seg.color1.getAsInt()){
//							segment.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(seg.color0.getAsInt()));
//							segment.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier()*Pixel.a_normalized(seg.color0.getAsInt())));
//						} else {
//							// use gradient for line stroke
//							segment.setAttributeNS(null, "fill", "url(#"+defID+")");
//						}
//					}
//				}
//
//			}
//		}
//	}

	protected static double[] findStrokeInterval(double current, double strokeLen, short pattern){
		double patternStart = current - (current%strokeLen);
		double patternPos = (current%strokeLen) * (16/strokeLen);
		int bit = (int)patternPos;
		int steps = bit;
		int[] pat = transferBits(pattern, new int[16]);
		// find next part of stroke pattern that is solid
		while( pat[bit] != 1 ){
			bit = (bit+1)%16;
			steps++;
		}
		double intervalStart = steps==0 ? current : patternStart+steps*(strokeLen/16);
		// find next part of stroke pattern that is empty
		while( pat[bit] == 1 ){
			bit = (bit+1)%16;
			steps++;
		}
		double intervalEnd = patternStart+steps*(strokeLen/16);
		// find next solid again
		while( pat[bit] != 1 ){
			bit = (bit+1)%16;
			steps++;
		}
		double nextIntervalStart = patternStart+steps*(strokeLen/16);
		return new double[]{intervalStart,intervalEnd,nextIntervalStart};
	}

	protected static int[] transferBits(short bits, int[] target){
		for(int i = 0; i < 16; i++){
			target[15-i] = (bits >> i) & 0b1;
		}
		return target;
	}
	
//	public static void main(String[] args) {
//		System.out.println(geometryShaderSrc);
//	}
	
}
