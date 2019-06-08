package hageldave.jplotter.renderers;

import java.util.Objects;

import org.joml.Matrix3fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Renderable;
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
			+ NL + "uniform mat4 viewMX;"
			+ NL + "uniform mat2 modelMX;"
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
			+ NL + "   gl_Position = projMX*viewMX*vec4(globalScaling*(modelMX*scaleMX*rotMX*in_position)+in_pointpos, 1,1);"
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

	protected float[] viewmxarray = new float[16];
	protected float[] modelmxarray = new float[]{1,0,0,1};
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
	}

	@Override
	@GLContextRequired
	protected void renderItem(Points item) {
		if(item.numPoints() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
		GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
		GL20.glUniformMatrix2fv(loc, false, modelmxarray);
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
	
	/**
	 * Sets the view matrix of this renderer.
	 * In order to not zoom the glyphs an inverse scaling
	 * is derived as model matrix for the glyphs to counteract
	 * the view matrix.
	 */
	@Override
	public void setViewMX(Matrix3fc viewmx, Matrix3fc scalemx, Matrix3fc transmx) {
		super.setViewMX(viewmx, scalemx, transmx);
		this.modelmxarray[0] = 1f/scalemx.m00();
		this.modelmxarray[1] = this.modelmxarray[2] = 0;
		this.modelmxarray[3] = 1f/scalemx.m11();
	}

}
