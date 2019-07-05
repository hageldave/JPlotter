package hageldave.jplotter.renderers;

import java.awt.geom.Rectangle2D;
import java.util.Objects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The PointsRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Points}.
 * It uses an instanced GL draw call for each of the {@link Points} objects
 * to render.
 * There is an third glyph scaling parameter in this renderer that is multiplied
 * to the existing 2 scalings of a single point instance
 * (see {@link Points#setGlobalScaling(double)}) to control the scaling on a
 * per renderer basis.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class PointsRenderer extends GenericRenderer<Points> {

	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec2 in_pointpos;"
			+ NL + "layout(location = 2) in vec2 in_rotAndScale;"
			+ NL + "layout(location = 3) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "uniform vec2 modelScaling;"
			+ NL + "uniform float globalScaling;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"

			+ NL + "mat2 rotationMatrix(float angle){"
			+ NL + "   float s = sin(angle), c = cos(angle);"
			+ NL + "   return mat2(c,s,-s,c);"
			+ NL + "}"

			+ NL + "mat2 scalingMatrix(float s){"
			+ NL + "   return mat2(s,0,0,s);"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   mat2 rotMX = rotationMatrix(in_rotAndScale.x);"
			+ NL + "   mat2 scaleMX = scalingMatrix(in_rotAndScale.y);"
			+ NL + "   vec3 pos = vec3(globalScaling*(scaleMX*rotMX*in_position)*modelScaling+in_pointpos, 1);"
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

	protected float glyphScaling = 1f;

	/**
	 * Sets the renderers glyph scaling value. 
	 * Default is 1.0.
	 * @param glyphScaling scaling for the glyphs that are rendered
	 * @return this for chaining
	 */
	public PointsRenderer setGlyphScaling(double glyphScaling) {
		this.glyphScaling = (float) glyphScaling;
		return this;
	}
	
	/**
	 * @return the renderers glyph scaling value
	 */
	public float getGlyphScaling() {
		return glyphScaling;
	}
	
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
			this.shader = new Shader(vertexShaderSrc, fragmentShaderSrc);
			itemsToRender.forEach(Renderable::initGL);;
		}
	}

	/**
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Points} contained in this
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
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelScaling");
		GL20.glUniform2f(loc, (float)(1/scaleX), (float)(1/scaleY));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Points item) {
		if(item.numPoints() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "globalScaling");
		GL20.glUniform1f(loc, this.glyphScaling * item.glyph.pixelSize() * item.getGlobalScaling());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, item.getGlobalAlphaMultiplier());
		// draw things
		item.bindVertexArray();
		if(item.glyph.useElementsDrawCall()){
			GL31.glDrawElementsInstanced(item.glyph.primitiveType(), item.glyph.numVertices(), GL11.GL_UNSIGNED_INT, 0, item.numPoints());
		} else {
			GL31.glDrawArraysInstanced(item.glyph.primitiveType(), 0, item.glyph.numVertices(), item.numPoints());
		}
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
		Element mainGroup = SVGUtils.createSVGElement(doc, "g");
		parent.appendChild(mainGroup);
		
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
		
		for(Points points : getItemsToRender()){
			Element pointsGroup = SVGUtils.createSVGElement(doc, "g");
			mainGroup.appendChild(pointsGroup);
			Glyph glyph = points.glyph;
			String symbolID = SVGUtils.createGlyphSymbolDef(doc, glyph, "glyph_"+glyph.glyphName());
			for(PointDetails point : points.getPointDetails()){
				double x1,y1;
				x1=point.location.getX(); y1=point.location.getY();
				
				x1-=translateX;
				y1-=translateY;
				x1*=scaleX;
				y1*=scaleY;
				
				if(!viewportRect.intersects(
						x1-glyph.pixelSize()/2, 
						y1-glyph.pixelSize()/2, 
						glyph.pixelSize(), 
						glyph.pixelSize()))
				{
					continue;
				}
				
				Element pointElement = SVGUtils.createSVGElement(doc, "use");
				pointsGroup.appendChild(pointElement);
				pointElement.setAttributeNS(null, "xlink:href", "#"+symbolID);
				if(glyph.isFilled()){
					pointElement.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(point.color));
					pointElement.setAttributeNS(null, "fill-opacity", SVGUtils.svgNumber(points.getGlobalAlphaMultiplier()*Pixel.a_normalized(point.color)));
				} else {
					pointElement.setAttributeNS(null, "stroke", SVGUtils.svgRGBhex(point.color));
					pointElement.setAttributeNS(null, "stroke-opacity", SVGUtils.svgNumber(points.getGlobalAlphaMultiplier()*Pixel.a_normalized(point.color)));
					pointElement.setAttributeNS(null, "fill-opacity", "0");
				}
				String transform = "";
				transform += "translate("+SVGUtils.svgNumber(x1)+","+SVGUtils.svgNumber(y1)+")";
				if(point.rot != 0){
					transform += " rotate("+SVGUtils.svgNumber(point.rot*180/Math.PI)+")";
				}
				if(glyphScaling*point.scale != 1){
					transform += " scale("+SVGUtils.svgPoints(glyphScaling*point.scale, glyphScaling*point.scale)+")";
				}
				
				pointElement.setAttributeNS(null, "transform", transform);
			}
		}
	}

}
