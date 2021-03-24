package hageldave.jplotter.renderers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.stream.Collectors;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.color.ColorOperations;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderables.Curves;
import hageldave.jplotter.renderables.Curves.CurveDetails;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.ShaderRegistry;

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
			// screen space clip coordinates
			double xmin = Objects.isNull(view) ? 0:view.getMinX()*scaleX;
			double xmax = Objects.isNull(view) ? w:view.getMaxX()*scaleX;
			double ymin = Objects.isNull(view) ? 0:view.getMinY()*scaleY; 
			double ymax = Objects.isNull(view) ? h:view.getMaxY()*scaleY;
			boolean viewHasChanged_ = this.viewHasChanged;
			this.viewHasChanged = false;
			for(Curves item: itemsToRender){
				if(item.isDirty() || viewHasChanged_ || vpHasChanged){
					// update items gl state if necessary
					item.updateGL(scaleX, scaleY, xmin, xmax, ymin, ymax);
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
	
	@Override
	public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
		if(!isEnabled()){
			return;
		}

		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);

		for(Curves curves : getItemsToRender()){
			if(curves.isHidden() || curves.getStrokePattern()==0 || curves.numCurves() == 0){
				// line is invisible
				continue;
			}
			
			// find connected curves
			ArrayList<CurveDetails> currentStrip = new ArrayList<>(1);
			LinkedList<ArrayList<CurveDetails>> allStrips = new LinkedList<>();
			ArrayList<CurveDetails> allCurves = curves.streamIntersecting(Objects.isNull(view) ? viewportRect : view)
					.collect(Collectors.toCollection(ArrayList::new));
			if(!allCurves.isEmpty())
				currentStrip.add(allCurves.get(0));
			if(!currentStrip.isEmpty())
				allStrips.add(currentStrip);
			for(int i=1; i<allCurves.size(); i++){
				CurveDetails curr = allCurves.get(i);
				CurveDetails prev = currentStrip.get(currentStrip.size()-1);
				if(	!(prev.p1.equals(curr.p0)) || 
					!(prev.thickness.getAsDouble()==curr.thickness.getAsDouble()) ||
					!(prev.color.getAsInt()==curr.color.getAsInt())
				){
					currentStrip = new ArrayList<>(1);
					allStrips.add(currentStrip);
				}
				currentStrip.add(curr);
			}			
			
			for(ArrayList<CurveDetails> curvestrip : allStrips){
				double strokew = curvestrip.get(0).thickness.getAsDouble() * curves.getGlobalThicknessMultiplier();
				BasicStroke stroke;
				if(!curves.hasStrokePattern()) {
					stroke = new BasicStroke((float)strokew, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f);
				} else {
					stroke = new BasicStroke((float)strokew, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, 
							LinesRenderer.strokePattern2dashPattern(curves.getStrokePattern(), (float)curves.getStrokeLength()), 0f); 
				}
				g.setStroke(stroke);
				p.setStroke(stroke);
				g.setColor(new Color(ColorOperations.scaleColorAlpha(curvestrip.get(0).color.getAsInt(), curves.getGlobalAlphaMultiplier()), true));
				
				for(CurveDetails cd : curvestrip) {
					float x1 = (float)(scaleX*(cd.p0.getX()-translateX));
					float y1 = (float)(scaleY*(cd.p0.getY()-translateY));
					float x2 = (float)(scaleX*(cd.p1.getX()-translateX));
					float y2 = (float)(scaleY*(cd.p1.getY()-translateY));
					float ctrlx1 = (float)(scaleX*(cd.pc0.getX()-translateX));
					float ctrly1 = (float)(scaleY*(cd.pc0.getY()-translateY));
					float ctrlx2 = (float)(scaleX*(cd.pc1.getX()-translateX));
					float ctrly2 = (float)(scaleY*(cd.pc1.getY()-translateY));
					
					CubicCurve2D cc2d = new CubicCurve2D.Float(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
					g.draw(cc2d);
					if(cd.pickColor != 0) {
						p.setColor(new Color(cd.pickColor));
						p.draw(cc2d);
					}
				}
			}
		}
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

		for(Curves curves : getItemsToRender()){
			if(curves.isHidden() || curves.getStrokePattern()==0 || curves.numCurves() == 0){
				// line is invisible
				continue;
			}
			// find connected curves
			ArrayList<CurveDetails> currentStrip = new ArrayList<>(1);
			LinkedList<ArrayList<CurveDetails>> allStrips = new LinkedList<>();
			ArrayList<CurveDetails> allCurves = curves.streamIntersecting(Objects.isNull(view) ? viewportRect : view)
					.collect(Collectors.toCollection(ArrayList::new));
			currentStrip.add(allCurves.get(0));
			allStrips.add(currentStrip);
			for(int i=1; i<allCurves.size(); i++){
				CurveDetails curr = allCurves.get(i);
				CurveDetails prev = currentStrip.get(currentStrip.size()-1);
				if(	!(prev.p1.equals(curr.p0)) || 
					!(prev.thickness.getAsDouble()==curr.thickness.getAsDouble()) ||
					!(prev.color.getAsInt()==curr.color.getAsInt())
				){
					currentStrip = new ArrayList<>(1);
					allStrips.add(currentStrip);
				}
				currentStrip.add(curr);
			}
			
			
			Element linesGroup = SVGUtils.createSVGElement(doc, "g");
			linesGroup.setAttributeNS(null, "fill", "transparent");
			if(curves.hasStrokePattern()){
				linesGroup.setAttributeNS(null, "stroke-dasharray", strokePattern2dashArray(curves.getStrokePattern(), curves.getStrokeLength()));
			}
			mainGroup.appendChild(linesGroup);
			
			for(ArrayList<CurveDetails> curvestrip : allStrips){
				Element path = SVGUtils.createSVGElement(doc, "path");
				path.setAttributeNS(null, "d", pathSVGCoordinates(curvestrip, translateX, translateY, scaleX, scaleY));
				double strokew = curvestrip.get(0).thickness.getAsDouble() * curves.getGlobalThicknessMultiplier();
				path.setAttributeNS(null, "stroke-width", SVGUtils.svgNumber(strokew));
				int color = curvestrip.get(0).color.getAsInt();
				path.setAttributeNS(null, "stroke", SVGUtils.svgRGBhex(color));
				double opacity = Pixel.a_normalized(color)*curves.getGlobalAlphaMultiplier();
				if(opacity != 1.0){
					path.setAttributeNS(null, "stroke-opacity", SVGUtils.svgNumber(opacity));
				}
				linesGroup.appendChild(path);
			}
		}
	}
	
	protected static String pathSVGCoordinates(ArrayList<CurveDetails> curveStrip, double tx, double ty, double sx, double sy){
		double[] coordsX = new double[(1+curveStrip.size()*3)];
		double[] coordsY = coordsX.clone();
		// extract path coordinates
		coordsX[0]=curveStrip.get(0).p0.getX();
		coordsY[0]=curveStrip.get(0).p0.getY();
		for(int i=0; i<curveStrip.size(); i++){
			CurveDetails c = curveStrip.get(i);
			coordsX[i*3+1] = c.pc0.getX();
			coordsY[i*3+1] = c.pc0.getY();
			coordsX[i*3+2] = c.pc1.getX();
			coordsY[i*3+2] = c.pc1.getY();
			coordsX[i*3+3] = c.p1.getX();
			coordsY[i*3+3] = c.p1.getY();
		}
		// view transformation
		for(int i=0; i<coordsX.length; i++){
			double x = coordsX[i];
			double y = coordsY[i];
			x-=tx;
			y-=ty;
			x*=sx;
			y*=sy;
			coordsX[i] = x;
			coordsY[i] = y;
		}
		StringBuilder sb = new StringBuilder();
		sb.append('M');
		sb.append(SVGUtils.svgNumber(coordsX[0]));
		sb.append(' ');
		sb.append(SVGUtils.svgNumber(coordsY[0]));
		sb.append(" C ");
		for(int i=1; i<coordsX.length; i++){
			sb.append(SVGUtils.svgNumber(coordsX[i]));
			sb.append(' ');
			sb.append(SVGUtils.svgNumber(coordsY[i]));
			if(i < coordsX.length-1)
				sb.append(',');
		}
		return sb.toString();
	}

	protected static String strokePattern2dashArray(short pattern, double len){
		int[] onoff = transferBits(pattern, new int[16]);
		LinkedList<Integer> dashes = new LinkedList<>();
		int curr = onoff[0];
		int l=0;
		for(int i=0; i<onoff.length; i++){
			if(onoff[i]==curr){
				l++;
			} else {
				dashes.add(l);
				curr=onoff[i];
				l=1;
			}
		}
		dashes.add(l);
		if(onoff[0]==0)
			dashes.add(0, 0);
		if(dashes.size()%2==1)
			dashes.add(0);
		double scaling = len/16;
		return dashes.stream().map(i->SVGUtils.svgNumber(i*scaling)).reduce((a,b)->a+" "+b).get();
	}

	protected static int[] transferBits(short bits, int[] target){
		for(int i = 0; i < 16; i++){
			target[15-i] = (bits >> i) & 0b1;
		}
		return target;
	}
	
//	public static void main(String[] args) {
//		System.out.println(strokePattern2dashArray((short)0x0f0f, 16));
//	}
	
}
