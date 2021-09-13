package hageldave.jplotter.renderers;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.color.ColorOperations;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.GLUtils;
import hageldave.jplotter.util.ShaderRegistry;
import org.apache.batik.ext.awt.geom.Polygon2D;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.function.PDFunctionType2;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDShadingPattern;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType2;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

import static hageldave.jplotter.util.Utils.hypot;

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
	
	/*************************************** DOUBLE PRECISION ***********************************/
	
	protected static final String vertexShaderSrcD = ""
			+ "" + "#version 410"
			+ NL + "layout(location = 0) in dvec2 in_position;"
			+ NL + "layout(location = 1) in uint in_color;"
			+ NL + "layout(location = 2) in uint in_pick;"
			+ NL + "layout(location = 3) in float in_thickness;"
			+ NL + "layout(location = 4) in float in_pathlen;"
			+ NL + "uniform dvec4 viewTransform;"
			+ NL + "uniform float saturationScaling;"
			+ NL + "out vec4 vcolor;"
			+ NL + "out vec4 vpick;"
			+ NL + "out float vthickness;"
			+ NL + "out float vpathlen;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "vec4 scaleSaturation(vec4 rgba, float sat) {"
			+ NL + "   float l = rgba.x*0.2126 + rgba.y*0.7152 + rgba.z*0.0722; // luminance"
			+ NL + "   vec3 drgb = rgba.xyz-vec3(l);"
			+ NL + "   float s=sat;"
			+ NL + "   if(s > 1.0) {"
			+ NL + "      // find maximal saturation that will keep channel values in range [0,1]"
			+ NL + "      s = min(s, drgb.x<0.0 ? -l/drgb.x : (1-l)/drgb.x);" 
			+ NL + "      s = min(s, drgb.y<0.0 ? -l/drgb.y : (1-l)/drgb.y);" 
			+ NL + "      s = min(s, drgb.z<0.0 ? -l/drgb.z : (1-l)/drgb.z);"
			+ NL + "   }"
			+ NL + "   return vec4(vec3(l)+s*drgb, rgba.w);"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   dvec3 pos = dvec3(in_position,1);"
			+ NL + "   pos = pos - dvec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * dvec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = vec4(pos,1);"
			+ NL + "   vcolor = scaleSaturation(unpackARGB(in_color), saturationScaling);"
			+ NL + "   vpick =  unpackARGB(in_pick);"
			+ NL + "   vthickness = in_thickness;"
			+ NL + "   vpathlen = in_pathlen;"
			+ NL + "}"
			+ NL
			;

	/*************************************** SINGLE PRECISION ***********************************/
	
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in uint in_color;"
			+ NL + "layout(location = 2) in uint in_pick;"
			+ NL + "layout(location = 3) in float in_thickness;"
			+ NL + "layout(location = 4) in float in_pathlen;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "uniform float saturationScaling;"
			+ NL + "out vec4 vcolor;"
			+ NL + "out vec4 vpick;"
			+ NL + "out float vthickness;"
			+ NL + "out float vpathlen;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "vec4 scaleSaturation(vec4 rgba, float sat) {"
			+ NL + "   float l = rgba.x*0.2126 + rgba.y*0.7152 + rgba.z*0.0722; // luminance"
			+ NL + "   vec3 drgb = rgba.xyz-vec3(l);"
			+ NL + "   float s=sat;"
			+ NL + "   if(s > 1.0) {"
			+ NL + "      // find maximal saturation that will keep channel values in range [0,1]"
			+ NL + "      s = min(s, drgb.x<0.0 ? -l/drgb.x : (1-l)/drgb.x);" 
			+ NL + "      s = min(s, drgb.y<0.0 ? -l/drgb.y : (1-l)/drgb.y);" 
			+ NL + "      s = min(s, drgb.z<0.0 ? -l/drgb.z : (1-l)/drgb.z);"
			+ NL + "   }"
			+ NL + "   return vec4(vec3(l)+s*drgb, rgba.w);"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   vec3 pos = vec3(in_position,1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = vec4(pos,1);"
			+ NL + "   vcolor = scaleSaturation(unpackARGB(in_color), saturationScaling);"
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
		if(Objects.isNull(shaderF)){
			shaderF = ShaderRegistry.getOrCreateShader(this.getClass().getName()+"#F",()->new Shader(vertexShaderSrc, geometryShaderSrc, fragmentShaderSrc));
			itemsToRender.forEach(Renderable::initGL);
		}
		if(Objects.isNull(shaderD) && isGLDoublePrecisionEnabled) {
			shaderD = ShaderRegistry.getOrCreateShader(this.getClass().getName()+"#D",()->new Shader(vertexShaderSrcD, geometryShaderSrc, fragmentShaderSrc));
		}
	}

	@Override
	@GLContextRequired
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled()){
			return;
		}
		Shader shader = getShader();
		boolean useDoublePrecision = shader == shaderD;
		boolean vpHasChanged = w != preVpW || h != preVpH;
		if(Objects.nonNull(shader) && w>0 && h>0 && !itemsToRender.isEmpty()){
			// initialize all objects first
			for(Lines item: itemsToRender){
				item.initGL();
			}
			// bind shader
			shader.bind();
			// prepare for rendering (e.g. en/disable depth or blending and such)
			orthoMX = GLUtils.orthoMX(orthoMX, 0, w, 0, h);
			renderStart(w,h, shader);
			// render every item
			double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
			double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
			boolean viewHasChanged_ = this.viewHasChanged;
			this.viewHasChanged = false;
			for(Lines item: itemsToRender){
				if(	item.isDirty() 
					|| item.isGLDoublePrecision()!=useDoublePrecision 
					||((viewHasChanged_ || vpHasChanged) && item.hasStrokePattern() )
				){
					// update items gl state if necessary
					item.updateGL(useDoublePrecision, scaleX,scaleY);
				}
				if(!item.isHidden()){
					renderItem(item, shader);
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
	protected void renderStart(int w, int h, Shader shader) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
		int loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewTransform");

		if (shader == shaderD /* double precision shader */)
		{
			GL40.glUniform4d(loc, translateX, translateY, scaleX, scaleY);
		}
		else
		{
			GL20.glUniform4f(loc, (float)translateX, (float)translateY, (float)scaleX, (float)scaleY);
		}

		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Lines lines, Shader shader) {
		if(lines.numSegments() < 1) {
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "linewidthMultiplier");
		GL20.glUniform1f(loc, lines.getGlobalThicknessMultiplier());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "saturationScaling");
		GL20.glUniform1f(loc, lines.getGlobalSaturationMultiplier());
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
		if(Objects.nonNull(shaderF))
			ShaderRegistry.handbackShader(shaderF);
		shaderF = null;
		if(Objects.nonNull(shaderD))
			ShaderRegistry.handbackShader(shaderD);
		shaderD = null;
		closeAllItems();
	}

    @Override
    public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
        if (!isEnabled()) {
            return;
        }

        double translateX = Objects.isNull(view) ? 0 : view.getX();
        double translateY = Objects.isNull(view) ? 0 : view.getY();
        double scaleX = Objects.isNull(view) ? 1 : w / view.getWidth();
        double scaleY = Objects.isNull(view) ? 1 : h / view.getHeight();

        Rectangle2D viewportRect = new Rectangle2D.Float(0, 0, w, h);
        float[][] polygonCoords = new float[2][4];

        for (Lines lines : getItemsToRender()) {
            if (lines.isHidden() || lines.getStrokePattern() == 0 || lines.numSegments() == 0) {
                // line is invisible
                continue;
            }

            boolean hasVaryingThickness = false;
            double thick = lines.getSegments().get(0).thickness0.getAsDouble();
            for (int i = 0; i < lines.numSegments(); i++) {
                SegmentDetails seg = lines.getSegments().get(i);
                if (seg.thickness0.getAsDouble() != thick || seg.thickness1.getAsDouble() != thick) {
                    hasVaryingThickness = true;
                    break;
                }
            }

            if (hasVaryingThickness)
                renderFallbackLinesVT(g, p, lines, translateX, translateY, scaleX, scaleY, viewportRect, polygonCoords);
            else
                renderFallbackLinesCT(g, p, lines, translateX, translateY, scaleX, scaleY, viewportRect, (float) ( thick * lines.getGlobalThicknessMultiplier() ));

        }
    }

    private void renderFallbackLinesCT(
            Graphics2D g,
            Graphics2D p,
            Lines lines,
            double translateX,
            double translateY,
            double scaleX,
            double scaleY,
            Rectangle2D viewportRect,
            float thickness) {
        double dist = 0;
        double prevX = 0;
        double prevY = 0;

        float[] dash = lines.hasStrokePattern() ? strokePattern2dashPattern(lines.getStrokePattern(), lines.getStrokeLength()) : null;

        for (SegmentDetails seg : lines.getSegments()) {
            double x1, y1, x2, y2;
            x1 = seg.p0.getX();
            y1 = seg.p0.getY();
            x2 = seg.p1.getX();
            y2 = seg.p1.getY();

            x1 -= translateX;
            x2 -= translateX;
            y1 -= translateY;
            y2 -= translateY;
            x1 *= scaleX;
            x2 *= scaleX;
            y1 *= scaleY;
            y2 *= scaleY;

            // path length calculations
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = hypot(dx, dy);
            double l1;
            if (prevX == x1 && prevY == y1) {
                l1 = dist;
                dist += len;
                dist = dist % lines.getStrokeLength();
            } else {
                l1 = 0;
                dist = len;
            }
            prevX = x2;
            prevY = y2;

            if (lines.isVertexRoundingEnabled()) {
                x1 = (int) ( x1 + 0.5 );
                x2 = (int) ( x2 + 0.5 );
                y1 = (int) ( y1 + 0.5 );
                y2 = (int) ( y2 + 0.5 );
                if (thickness % 2 == 1f) {
                    x1 += .5f;
                    x2 += .5f;
                    y1 += .5f;
                    y2 += .5f;
                }
            }

            // visibility check
            if (!viewportRect.intersectsLine(x1, y1, x2, y2)) {
                continue;
            }

            Paint paint;

            //int c1, c2;
            int c1 = ColorOperations.changeSaturation(seg.color0.getAsInt(), lines.getGlobalSaturationMultiplier());
            c1 = ColorOperations.scaleColorAlpha(c1, lines.getGlobalAlphaMultiplier());
            //c1 = ColorOperations.scaleColorAlpha(seg.color0.getAsInt(), lines.getGlobalAlphaMultiplier());

            int c2 = ColorOperations.changeSaturation(seg.color1.getAsInt(), lines.getGlobalSaturationMultiplier());
            c2 = ColorOperations.scaleColorAlpha(c2, lines.getGlobalAlphaMultiplier());

            if (c1 != c2) {
                paint = new GradientPaint((float) x1, (float) y1, new Color(c1, true), (float) x2, (float) y2, new Color(c2, true));
            } else paint = new Color(c1, true);
            g.setPaint(paint);

            BasicStroke stroke;
            if (lines.hasStrokePattern()) {
                stroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, (float) l1);
            } else {
                stroke = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f);
            }
            g.setStroke(stroke);
            g.draw(new Line2D.Double(x1, y1, x2, y2));

            if (seg.pickColor != 0) {
                p.setStroke(stroke);
                p.setColor(new Color(seg.pickColor));
                p.draw(new Line2D.Double(x1, y1, x2, y2));
            }

        }
    }

    private void renderFallbackLinesVT(
            Graphics2D g,
            Graphics2D p,
            Lines lines,
            double translateX,
            double translateY,
            double scaleX,
            double scaleY,
            Rectangle2D viewportRect,
            float[][] polygonCoords) {
        double dist = 0;
        double prevX = 0;
        double prevY = 0;

        for (SegmentDetails seg : lines.getSegments()) {
            double x1, y1, x2, y2;
            x1 = seg.p0.getX();
            y1 = seg.p0.getY();
            x2 = seg.p1.getX();
            y2 = seg.p1.getY();

            x1 -= translateX;
            x2 -= translateX;
            y1 -= translateY;
            y2 -= translateY;
            x1 *= scaleX;
            x2 *= scaleX;
            y1 *= scaleY;
            y2 *= scaleY;

            // path length calculations
            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = hypot(dx, dy);
            double l1, l2;
            if (prevX == x1 && prevY == y1) {
                l1 = dist;
                l2 = dist + len;
                dist += len;
                dist = dist % lines.getStrokeLength();
            } else {
                l1 = 0;
                l2 = len;
                dist = len;
            }
            prevX = x2;
            prevY = y2;

            // visibility check
            if (!viewportRect.intersectsLine(x1, y1, x2, y2)) {
                continue;
            }

            // miter vector stuff
            double normalize = 1 / len;
            double miterX = dy * normalize * 0.5;
            double miterY = -dx * normalize * 0.5;
            double t1 = seg.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier();
            double t2 = seg.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier();


            Paint paint;
            /*int c1, c2;
            c1 = ColorOperations.scaleColorAlpha(seg.color0.getAsInt(), lines.getGlobalAlphaMultiplier());
            c2 = ColorOperations.scaleColorAlpha(seg.color1.getAsInt(), lines.getGlobalAlphaMultiplier());*/

            int c1 = ColorOperations.changeSaturation(seg.color0.getAsInt(), lines.getGlobalSaturationMultiplier());
            c1 = ColorOperations.scaleColorAlpha(c1, lines.getGlobalAlphaMultiplier());
            //c1 = ColorOperations.scaleColorAlpha(seg.color0.getAsInt(), lines.getGlobalAlphaMultiplier());

            int c2 = ColorOperations.changeSaturation(seg.color1.getAsInt(), lines.getGlobalSaturationMultiplier());
            c2 = ColorOperations.scaleColorAlpha(c2, lines.getGlobalAlphaMultiplier());

            if (c1 != c2) {
                paint = new GradientPaint((float) x1, (float) y1, new Color(c1, true), (float) x2, (float) y2, new Color(c2, true));
            } else paint = new Color(c1, true);
            g.setPaint(paint);

            if (!lines.hasStrokePattern()) {
                float[][] pc = polygonCoords;
                pc[0][0] = (float) ( x1 + miterX * t1 );
                pc[1][0] = (float) ( y1 + miterY * t1 );
                pc[0][1] = (float) ( x2 + miterX * t2 );
                pc[1][1] = (float) ( y2 + miterY * t2 );
                pc[0][2] = (float) ( x2 - miterX * t2 );
                pc[1][2] = (float) ( y2 - miterY * t2 );
                pc[0][3] = (float) ( x1 - miterX * t1 );
                pc[1][3] = (float) ( y1 - miterY * t1 );
                // vertex rounding
                if (lines.isVertexRoundingEnabled()) {
                    for (int i = 0; i < 4; i++) {
                        pc[0][i] = (int) ( pc[0][i] + .5f );
                        pc[1][i] = (int) ( pc[1][i] + .5f );
                    }
                }
                // drawing
                g.fill(new Polygon2D(pc[0], pc[1], 4));
                if (seg.pickColor != 0) {
                    p.setColor(new Color(seg.pickColor));
                    p.fill(new Polygon2D(pc[0], pc[1], 4));
                }
            } else {
                float[][] pc = polygonCoords;
                double[] strokeInterval = findStrokeInterval(l1, lines.getStrokeLength(), lines.getStrokePattern());
                while (strokeInterval[0] < l2) {
                    double start = strokeInterval[0];
                    double end = Math.min(strokeInterval[1], l2);
                    // interpolation factors
                    double m1 = Math.max(( start - l1 ) / ( l2 - l1 ), 0);
                    double m2 = ( end - l1 ) / ( l2 - l1 );
                    // interpolate miters
                    double t1_ = t1 * ( 1 - m1 ) + t2 * m1;
                    double t2_ = t1 * ( 1 - m2 ) + t2 * m2;
                    // interpolate segment
                    double x1_ = x1 + dx * m1;
                    double x2_ = x1 + dx * m2;
                    double y1_ = y1 + dy * m1;
                    double y2_ = y1 + dy * m2;

                    pc[0][0] = (float) ( x1_ + miterX * t1_ );
                    pc[1][0] = (float) ( y1_ + miterY * t1_ );
                    pc[0][1] = (float) ( x2_ + miterX * t2_ );
                    pc[1][1] = (float) ( y2_ + miterY * t2_ );
                    pc[0][2] = (float) ( x2_ - miterX * t2_ );
                    pc[1][2] = (float) ( y2_ - miterY * t2_ );
                    pc[0][3] = (float) ( x1_ - miterX * t1_ );
                    pc[1][3] = (float) ( y1_ - miterY * t1_ );
                    // vertex rounding
                    if (lines.isVertexRoundingEnabled()) {
                        for (int i = 0; i < 4; i++) {
                            pc[0][i] = (int) ( pc[0][i] + .5f );
                            pc[1][i] = (int) ( pc[1][i] + .5f );
                        }
                    }
                    // drawing
                    g.fill(new Polygon2D(pc[0], pc[1], 4));
                    if (seg.pickColor != 0) {
                        p.setColor(new Color(seg.pickColor));
                        p.fill(new Polygon2D(pc[0], pc[1], 4));
                    }

                    strokeInterval = findStrokeInterval(strokeInterval[2], lines.getStrokeLength(), lines.getStrokePattern());
                }
            }
        }
    }

    @Override
    public void renderSVG(Document doc, Element parent, int w, int h) {
        if (!isEnabled()) {
            return;
        }
        Element mainGroup = SVGUtils.createSVGElement(doc, "g");
        parent.appendChild(mainGroup);

        double translateX = Objects.isNull(view) ? 0 : view.getX();
        double translateY = Objects.isNull(view) ? 0 : view.getY();
        double scaleX = Objects.isNull(view) ? 1 : w / view.getWidth();
        double scaleY = Objects.isNull(view) ? 1 : h / view.getHeight();

        Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);

        for (Lines lines : getItemsToRender()) {
            if (lines.isHidden() || lines.getStrokePattern() == 0 || lines.numSegments() == 0) {
                // line is invisible
                continue;
            }
            Element linesGroup = SVGUtils.createSVGElement(doc, "g");
            linesGroup.setAttributeNS(null, "stroke-width", "0");
            mainGroup.appendChild(linesGroup);
            double dist = 0;
            double prevX = 0;
            double prevY = 0;
            for (SegmentDetails seg : lines.getSegments()) {
                double x1, y1, x2, y2;
                x1 = seg.p0.getX();
                y1 = seg.p0.getY();
                x2 = seg.p1.getX();
                y2 = seg.p1.getY();

                x1 -= translateX;
                x2 -= translateX;
                y1 -= translateY;
                y2 -= translateY;
                x1 *= scaleX;
                x2 *= scaleX;
                y1 *= scaleY;
                y2 *= scaleY;

                // path length calculations
                double dx = x2 - x1;
                double dy = y2 - y1;
                double len = hypot(dx, dy);
                double l1, l2;
                if (prevX == x1 && prevY == y1) {
                    l1 = dist;
                    l2 = dist + len;
                    dist += len;
                    dist = dist % lines.getStrokeLength();
                } else {
                    l1 = 0;
                    l2 = len;
                    dist = len;
                }
                prevX = x2;
                prevY = y2;

                if (lines.isVertexRoundingEnabled()) {
                    x1 = (int) ( x1 + 0.5 );
                    x2 = (int) ( x2 + 0.5 );
                    y1 = (int) ( y1 + 0.5 );
                    y2 = (int) ( y2 + 0.5 );
                }

                // visibility check
                if (!viewportRect.intersectsLine(x1, y1, x2, y2)) {
                    continue;
                }

                // miter vector stuff
                double normalize = 1 / len;
                double miterX = dy * normalize * 0.5;
                double miterY = -dx * normalize * 0.5;
                double t1 = seg.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier();
                double t2 = seg.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier();

                int c0 = ColorOperations.changeSaturation(seg.color0.getAsInt(), lines.getGlobalSaturationMultiplier());
                int c1 = ColorOperations.changeSaturation(seg.color1.getAsInt(), lines.getGlobalSaturationMultiplier());

                String defID = "";
                if (seg.color0.getAsInt() != seg.color1.getAsInt()) {
                    // create gradient for line
                    Node defs = SVGUtils.getDefs(doc);
                    Element gradient = SVGUtils.createSVGElement(doc, "linearGradient");
                    defs.appendChild(gradient);
                    defID = SVGUtils.newDefId();
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
                            "stop-color:" + SVGUtils.svgRGBhex(c0) + ";" +
                                    "stop-opacity:" + SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier() * Pixel.a_normalized(c0)));
                    Element stop2 = SVGUtils.createSVGElement(doc, "stop");
                    gradient.appendChild(stop2);
                    stop2.setAttributeNS(null, "offset", "100%");
                    stop2.setAttributeNS(null, "style",
                            "stop-color:" + SVGUtils.svgRGBhex(c1) + ";" +
                                    "stop-opacity:" + SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier() * Pixel.a_normalized(c1)));
                }

                if (!lines.hasStrokePattern()) {
                    Element segment = SVGUtils.createSVGElement(doc, "polygon");
                    linesGroup.appendChild(segment);
                    segment.setAttributeNS(null, "points", SVGUtils.svgPoints(
                            x1 + miterX * t1, y1 + miterY * t1, x2 + miterX * t2, y2 + miterY * t2,
                            x2 - miterX * t2, y2 - miterY * t2, x1 - miterX * t1, y1 - miterY * t1));
                    if (seg.color0.getAsInt() == seg.color1.getAsInt()) {
                        segment.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(c0));
                        segment.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier() * Pixel.a_normalized(c0)));
                    } else {
                        // use gradient for line stroke
                        segment.setAttributeNS(null, "fill", "url(#" + defID + ")");
                    }
                } else {
                    double[] strokeInterval = findStrokeInterval(l1, lines.getStrokeLength(), lines.getStrokePattern());
                    while (strokeInterval[0] < l2) {
                        double start = strokeInterval[0];
                        double end = Math.min(strokeInterval[1], l2);
                        // interpolation factors
                        double m1 = Math.max(( start - l1 ) / ( l2 - l1 ), 0);
                        double m2 = ( end - l1 ) / ( l2 - l1 );
                        // interpolate miters
                        double t1_ = t1 * ( 1 - m1 ) + t2 * m1;
                        double t2_ = t1 * ( 1 - m2 ) + t2 * m2;
                        // interpolate segment
                        double x1_ = x1 + dx * m1;
                        double x2_ = x1 + dx * m2;
                        double y1_ = y1 + dy * m1;
                        double y2_ = y1 + dy * m2;

                        Element segment = SVGUtils.createSVGElement(doc, "polygon");
                        linesGroup.appendChild(segment);
                        segment.setAttributeNS(null, "points", SVGUtils.svgPoints(
                                x1_ + miterX * t1_, y1_ + miterY * t1_, x2_ + miterX * t2_, y2_ + miterY * t2_,
                                x2_ - miterX * t2_, y2_ - miterY * t2_, x1_ - miterX * t1_, y1_ - miterY * t1_));

                        strokeInterval = findStrokeInterval(strokeInterval[2], lines.getStrokeLength(), lines.getStrokePattern());

                        if (seg.color0.getAsInt() == seg.color1.getAsInt()) {
                            segment.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(c0));
                            segment.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(lines.getGlobalAlphaMultiplier() * Pixel.a_normalized(c0)));
                        } else {
                            // use gradient for line stroke
                            segment.setAttributeNS(null, "fill", "url(#" + defID + ")");
                        }
                    }
                }

            }
        }
    }

    protected static double[] findStrokeInterval(double current, double strokeLen, short pattern) {
        double patternStart = current - ( current % strokeLen );
        double patternPos = ( current % strokeLen ) * ( 16 / strokeLen );
        int bit = (int) patternPos;
        int steps = bit;
        int[] pat = transferBits(pattern, new int[16]);
        // find next part of stroke pattern that is solid
        while (pat[bit] != 1) {
            bit = ( bit + 1 ) & 0xf;//%16;
            steps++;
        }
        double intervalStart = steps == 0 ? current : patternStart + steps * ( strokeLen / 16 );
        // find next part of stroke pattern that is empty
        while (pat[bit] == 1) {
            bit = ( bit + 1 ) & 0xf;//%16;
            steps++;
        }
        double intervalEnd = patternStart + steps * ( strokeLen / 16 );
        // find next solid again
        while (pat[bit] != 1) {
            bit = ( bit + 1 ) & 0xf;//%16;
            steps++;
        }
        double nextIntervalStart = patternStart + steps * ( strokeLen / 16 );
        return new double[]{intervalStart, intervalEnd, nextIntervalStart};
    }

    protected static int[] transferBits(short bits, int[] target) {
        for (int i = 0; i < 16; i++) {
            target[15 - i] = ( bits >> i ) & 0b1;
        }
        return target;
    }

    protected static float[] strokePattern2dashPattern(short pattern, float strokeLen) {
        int[] bits = transferBits(pattern, new int[16]);
        // shift pattern to a valid start
        while (bits[0] != 1 && bits[15] != 0) {
            int b0 = bits[0];
            for (int i = 0; i < 15; i++)
                bits[i] = bits[i + 1];
            bits[15] = b0;
        }

        float unit = strokeLen / 16f;
        int currentBit = bits[0];
        int currentLen = 1;
        int iDash = 0;
        float[] dash = new float[16];
        for (int i = 1; i < 16; i++) {
            if (currentBit == bits[i]) {
                currentLen++;
            } else {
                dash[iDash++] = currentLen * unit;
                currentLen = 1;
                currentBit = bits[i];
            }
            if (i == 15)
                dash[iDash] = currentLen * unit;
        }
        return Arrays.copyOf(dash, iDash + 1);
    }

    @Override
    public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
        if (!isEnabled()) {
            return;
        }
        double translateX = Objects.isNull(view) ? 0 : view.getX();
        double translateY = Objects.isNull(view) ? 0 : view.getY();
        double scaleX = Objects.isNull(view) ? 1 : w / view.getWidth();
        double scaleY = Objects.isNull(view) ? 1 : h / view.getHeight();

        Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);

        try {
            PDPageContentStream contentStream = new PDPageContentStream(doc, page,
                    PDPageContentStream.AppendMode.APPEND, false);
            for (Lines lines : getItemsToRender()) {
                if (lines.isHidden() || lines.getStrokePattern() == 0 || lines.numSegments() == 0) {
                    // line is invisible
                    continue;
                }

                double dist = 0;
                double prevX = 0;
                double prevY = 0;

                PDDocument glyphDoc = new PDDocument();
                PDPage rectPage = new PDPage();
                glyphDoc.addPage(rectPage);
                PDPageContentStream rectCont = new PDPageContentStream(glyphDoc, rectPage);
                rectCont.addRect(x, y, w, h);
                LayerUtility layerUtility = new LayerUtility(doc);
                rectCont.close();
                PDFormXObject rectForm = layerUtility.importPageAsForm(glyphDoc, 0);
                glyphDoc.close();

                // clipping area
                contentStream.saveGraphicsState();
                contentStream.drawForm(rectForm);
                contentStream.closePath();
                contentStream.clip();

                for (SegmentDetails seg : lines.getSegments()) {
                    double x1, y1, x2, y2;
                    x1 = seg.p0.getX();
                    y1 = seg.p0.getY();
                    x2 = seg.p1.getX();
                    y2 = seg.p1.getY();

                    x1 -= translateX;
                    x2 -= translateX;
                    y1 -= translateY;
                    y2 -= translateY;
                    x1 *= scaleX;
                    x2 *= scaleX;
                    y1 *= scaleY;
                    y2 *= scaleY;

                    // path length calculations
                    double dx = x2 - x1;
                    double dy = y2 - y1;
                    double len = hypot(dx, dy);
                    double l1, l2;
                    if (prevX == x1 && prevY == y1) {
                        l1 = dist;
                        l2 = dist + len;
                        dist += len;
                        dist = dist % lines.getStrokeLength();
                    } else {
                        l1 = 0;
                        l2 = len;
                        dist = len;
                    }
                    prevX = x2;
                    prevY = y2;

                    if (lines.isVertexRoundingEnabled()) {
                        x1 = (int) ( x1 + 0.5 );
                        x2 = (int) ( x2 + 0.5 );
                        y1 = (int) ( y1 + 0.5 );
                        y2 = (int) ( y2 + 0.5 );
                    }

                    // visibility check
                    if (!viewportRect.intersectsLine(x1, y1, x2, y2)) {
                        continue;
                    }

                    // miter vector stuff
                    double normalize = 1 / len;
                    double miterX = dy * normalize * 0.5;
                    double miterY = -dx * normalize * 0.5;

                    double t1 = seg.thickness0.getAsDouble() * lines.getGlobalThicknessMultiplier();
                    double t2 = seg.thickness1.getAsDouble() * lines.getGlobalThicknessMultiplier();

                    int c1 = ColorOperations.changeSaturation(seg.color0.getAsInt(), lines.getGlobalSaturationMultiplier());
                    c1 = ColorOperations.scaleColorAlpha(c1, lines.getGlobalAlphaMultiplier());

                    int c2 = ColorOperations.changeSaturation(seg.color1.getAsInt(), lines.getGlobalSaturationMultiplier());
                    c2 = ColorOperations.scaleColorAlpha(c2, lines.getGlobalAlphaMultiplier());

                    if (!lines.hasStrokePattern()) {
                        // create invisible rectangle so that elements outside w, h won't be rendered
                        if (seg.color0.getAsInt() == seg.color1.getAsInt()) {
							PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
							int color = ColorOperations.changeSaturation(seg.color0.getAsInt(), lines.getGlobalSaturationMultiplier());
							Color scaledColor = new Color(ColorOperations.scaleColorAlpha(color, lines.getGlobalAlphaMultiplier()), true);
							graphicsState.setStrokingAlphaConstant(scaledColor.getAlpha()/255F);
							graphicsState.setNonStrokingAlphaConstant(scaledColor.getAlpha()/255F);
							contentStream.setGraphicsStateParameters(graphicsState);

                            contentStream.setNonStrokingColor(new Color(color));
                        } else {
							PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
							graphicsState.setNonStrokingAlphaConstant(lines.getGlobalAlphaMultiplier());
							contentStream.setGraphicsStateParameters(graphicsState);

                            PDShadingType2 shading = createGradientColor(c1, c2, new Point2D.Double(( x1 + miterX * t1 ) + x, ( y1 + miterY * t1 ) + y),
                                    new Point2D.Double(( x2 - miterX * t2 ) + x, ( y2 - miterY * t2 ) + y));
                            PDShadingPattern pattern = new PDShadingPattern();
                            pattern.setShading(shading);
                            COSName name = page.getResources().add(pattern);
                            PDColor color = new PDColor(name, new PDPattern(null));
                            contentStream.setNonStrokingColor(color);
                        }
                        // create segments
                        PDFUtils.createPDFPolygon(contentStream, new double[]{( x1 + miterX * t1 ) + x, ( x2 + miterX * t2 ) + x,
                                ( x2 - miterX * t2 ) + x, ( x1 - miterX * t1 ) + x}, new double[]{( y1 + miterY * t1 ) + y, ( y2 + miterY * t2 ) + y,
                                ( y2 - miterY * t2 ) + y, ( y1 - miterY * t1 ) + y});

                        contentStream.fill();
                    } else {
                        double[] strokeInterval = findStrokeInterval(l1, lines.getStrokeLength(), lines.getStrokePattern());
                        while (strokeInterval[0] < l2) {
                            double start = strokeInterval[0];
                            double end = Math.min(strokeInterval[1], l2);
                            // interpolation factors
                            double m1 = Math.max(( start - l1 ) / ( l2 - l1 ), 0);
                            double m2 = ( end - l1 ) / ( l2 - l1 );
                            // interpolate miters
                            double t1_ = t1 * ( 1 - m1 ) + t2 * m1;
                            double t2_ = t1 * ( 1 - m2 ) + t2 * m2;
                            // interpolate segment
                            double x1_ = x1 + dx * m1;
                            double x2_ = x1 + dx * m2;
                            double y1_ = y1 + dy * m1;
                            double y2_ = y1 + dy * m2;

                            strokeInterval = findStrokeInterval(strokeInterval[2], lines.getStrokeLength(), lines.getStrokePattern());


                            if (seg.color0.getAsInt() == seg.color1.getAsInt()) {
								PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
								int color = ColorOperations.changeSaturation(seg.color0.getAsInt(), lines.getGlobalSaturationMultiplier());
								Color scaledColor = new Color(ColorOperations.scaleColorAlpha(color, lines.getGlobalAlphaMultiplier()), true);
								graphicsState.setStrokingAlphaConstant(scaledColor.getAlpha()/255F);
								graphicsState.setNonStrokingAlphaConstant(scaledColor.getAlpha()/255F);
								contentStream.setGraphicsStateParameters(graphicsState);

                                contentStream.setNonStrokingColor(new Color(color));
                            } else {
								PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
								graphicsState.setNonStrokingAlphaConstant(lines.getGlobalAlphaMultiplier());
								contentStream.setGraphicsStateParameters(graphicsState);

                                PDShadingType2 shading = createGradientColor(c1, c2, new Point2D.Double(( x1 + miterX * t1 ) + x, ( y1 + miterY * t1 ) + y),
                                        new Point2D.Double(( x2 - miterX * t2 ) + x, ( y2 - miterY * t2 ) + y));
                                graphicsState.setStrokingAlphaConstant(lines.getGlobalAlphaMultiplier());
                                contentStream.setGraphicsStateParameters(graphicsState);
                                PDShadingPattern pattern = new PDShadingPattern();
                                pattern.setShading(shading);
                                COSName name = page.getResources().add(pattern);
                                PDColor color = new PDColor(name, new PDPattern(null));
                                contentStream.setNonStrokingColor(color);
                            }
                            PDFUtils.createPDFPolygon(contentStream, new double[]{( x1_ + miterX * t1_ ) + x, ( x2_ + miterX * t2_ ) + x,
                                    ( x2_ - miterX * t2_ ) + x, ( x1_ - miterX * t1_ ) + x}, new double[]{( y1_ + miterY * t1_ ) + y, ( y2_ + miterY * t2_ ) + y,
                                    ( y2_ - miterY * t2_ ) + y, ( y1_ - miterY * t1_ ) + y});
                            contentStream.fill();
                        }
                    }

                }
                contentStream.restoreGraphicsState();
            }
            contentStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Error occurred!");
        }
	}

	protected static PDShadingType2 createGradientColor(int color1, int color2, Point2D p0, Point2D p1) throws IOException {
		Color startColor = new Color(color1, true);
		Color endColor = new Color(color2, true);

		COSDictionary fdict = new COSDictionary();
		fdict.setInt(COSName.FUNCTION_TYPE, 2);

		COSArray domain = new COSArray();
		domain.add(COSInteger.get(0));
		domain.add(COSInteger.get(1));

		COSArray c0 = new COSArray();
		c0.add(new COSFloat(startColor.getRed() / 255f));
		c0.add(new COSFloat(startColor.getGreen() / 255f));
		c0.add(new COSFloat(startColor.getBlue() / 255f));
		c0.add(new COSFloat(startColor.getAlpha() / 255f));

		COSArray c1 = new COSArray();
		c1.add(new COSFloat(endColor.getRed() / 255f));
		c1.add(new COSFloat(endColor.getGreen() / 255f));
		c1.add(new COSFloat(endColor.getBlue() / 255f));
		c1.add(new COSFloat(endColor.getAlpha() / 255f));

		fdict.setItem(COSName.DOMAIN, domain);
		fdict.setItem(COSName.C0, c0);
		fdict.setItem(COSName.C1, c1);
		fdict.setInt(COSName.N, 1);

		PDFunctionType2 func = new PDFunctionType2(fdict);

		PDShadingType2 axialShading = new PDShadingType2(new COSDictionary());

		axialShading.setColorSpace(PDDeviceRGB.INSTANCE);
		axialShading.setShadingType(PDShading.SHADING_TYPE2);

		COSArray coords1 = new COSArray();
		coords1.add(new COSFloat((float) p0.getX()));
		coords1.add(new COSFloat((float) p0.getY()));
		coords1.add(new COSFloat((float) p1.getX()));
		coords1.add(new COSFloat((float) p1.getY()));

		axialShading.setCoords(coords1);
		axialShading.setFunction(func);

		return axialShading;

	}
}
