package hageldave.jplotter.renderers;

import java.awt.Color;
import java.util.Objects;

import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.globjects.Shader;
import hageldave.jplotter.renderables.CharacterAtlas;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderables.Text;

/**
 * The TrianglesRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Text}.
 * It draws the vertex arrays of its Text objects and uses the texture of the
 * {@link CharacterAtlas} corresponding to the Text's font to texture
 * the drawn quads in order to display text.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class TextRenderer extends GenericRenderer<Text> {

	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec2 in_texcoords;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform mat4 viewMX;"
			+ NL + "uniform mat2 modelMX;"
			+ NL + "uniform vec2 origin;"
			+ NL + "uniform float rot;"
			+ NL + "out vec2 tex_Coords;"
			
			+ NL + "mat2 rotationMatrix(float angle){"
			+ NL + "   float s = sin(angle), c = cos(angle);"
			+ NL + "   return mat2(c,s,-s,c);"
			+ NL + "}"
			
			+ NL + "void main() {"
			+ NL + "   mat2 rotMX = rotationMatrix(rot);"
			+ NL + "   vec4 pos = viewMX*vec4((modelMX*rotMX*in_position)+origin, 1,1);"
			+ NL + "   gl_Position = projMX*vec4(int(pos.x), int(pos.y), pos.z, pos.w);"
			+ NL + "   tex_Coords = in_texcoords;"
			+ NL + "}"
			+ NL
			;
	protected static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "uniform sampler2D tex;"
			+ NL + "uniform vec4 fragColorToUse;"
			+ NL + "uniform vec4 pickColorToUse;"
			+ NL + "uniform bool useTex;"
			+ NL + "in vec2 tex_Coords;"
			+ NL + "void main() {"
			+ NL + "   frag_color = fragColorToUse;"
			+ NL + "   if(useTex){"
			+ NL + "      vec4 texColor = texture(tex, tex_Coords);"
			+ NL + "      frag_color = fragColorToUse*texColor;"
			+ NL + "   }"
			+ NL + "   pick_color = pickColorToUse;"
			+ NL + "}"
			;
	
	protected Matrix3f transMX = new Matrix3f();
	protected float[] viewmxarray = new float[16];
	protected float[] modelmxarray = new float[]{1,0,0,1};
	
	
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
	 * Disables {@link GL11#GL_DEPTH_TEST},
	 * enables {@link GL11#GL_BLEND}
	 * ,sets {@link GL11#GL_SRC_ALPHA}, {@link GL11#GL_ONE_MINUS_SRC_ALPHA}
	 * as blend function
	 * and activates the 0th texture unit {@link GL13#GL_TEXTURE0}.
	 */
	@Override
	@GLContextRequired
	protected void renderStart(int w, int h) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Text txt) {
		int loc;
		txt.bindVertexArray();
		
		// draw background if bg color is not 0
		if(txt.getBackground().getRGB() !=0){
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
			GL20.glUniformMatrix4fv(loc, false, orthoMX);
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
			GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
			GL20.glUniformMatrix2fv(loc, false, modelmxarray);
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "origin");
			GL20.glUniform2f(loc, (float)txt.getOrigin().getX(), (float)txt.getOrigin().getY());
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "rot");
			GL20.glUniform1f(loc, txt.getAngle());
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "fragColorToUse");
			Color bg = txt.getBackground();
			GL20.glUniform4f(loc, bg.getRed()/255f, bg.getGreen()/255f, bg.getBlue()/255f, bg.getAlpha()/255f);
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "pickColorToUse");
			GL20.glUniform4f(loc, 0,0,0,0);
			loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "useTex");
			GL20.glUniform1i(loc, 0);
			// draw things
			GL11.glDrawElements(GL11.GL_TRIANGLES, txt.getVertexArray().getNumIndices(), GL11.GL_UNSIGNED_INT, 0);
		}
		
		GL13.glBindTexture(GL11.GL_TEXTURE_2D, txt.getTextureID());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "tex");
		GL20.glUniform1i(loc, 0);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewMX");
		GL20.glUniformMatrix4fv(loc, false, viewMX.get(viewmxarray));
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelMX");
		GL20.glUniformMatrix2fv(loc, false, modelmxarray);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "origin");
		GL20.glUniform2f(loc, (float)txt.getOrigin().getX(), (float)txt.getOrigin().getY());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "rot");
		GL20.glUniform1f(loc, txt.getAngle());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "fragColorToUse");
		GL20.glUniform4f(loc, txt.getColorR(), txt.getColorG(), txt.getColorB(), txt.getColorA());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "pickColorToUse");
		GL20.glUniform4f(loc, txt.getPickColorR(), txt.getPickColorG(), txt.getPickColorB(), 1f);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "useTex");
		GL20.glUniform1i(loc, 1);
		// draw things
		GL11.glDrawElements(GL11.GL_TRIANGLES, txt.getVertexArray().getNumIndices(), GL11.GL_UNSIGNED_INT, 0);
		txt.releaseVertexArray();
	}
	
	/**
	 * disables {@link GL11#GL_BLEND},
	 * enables {@link GL11#GL_DEPTH_TEST},
	 * releases still bound texture.
	 */
	@Override
	@GLContextRequired
	protected void renderEnd() {
		GL13.glBindTexture(GL11.GL_TEXTURE_2D, 0);
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
	
	/**
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Text}s contained in this
	 * renderer.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(shader))
			shader.close();
		deleteAllItems();
	}

}
