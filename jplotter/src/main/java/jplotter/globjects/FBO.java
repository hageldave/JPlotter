package jplotter.globjects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import jplotter.util.GLUtils;

/**
 * FrameBufferObject
 */
public class FBO implements AutoCloseable {

	public final int width;
	public final int height;
	
	int fboid;
	int colortexid;
	int pickingtexid;
	int depthtexid;
	
	public FBO(int width, int height) {
		this.width = width;
		this.height = height;
		this.fboid = GL30.glGenFramebuffers();
		this.colortexid = GLUtils.create2DTexture(width, height, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
		this.pickingtexid = GLUtils.create2DTexture(width, height, GL11.GL_RGBA8, GL11.GL_RGBA, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
		this.depthtexid = GLUtils.create2DTexture(width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_DEPTH_COMPONENT, GL11.GL_LINEAR, GL12.GL_CLAMP_TO_EDGE);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboid);
		{
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, colortexid, 0);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, pickingtexid, 0);
			GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthtexid, 0);
			String status = GLUtils.checkFBOstatus();
			if(!status.isEmpty()){
				close();
				throw new GLUtils.GLRuntimeException("Bad framebuffer status: " + status);
			}
		}
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
	
	public int getFBOid() {
		return fboid;
	}
	
	public int getMainColorTexId() {
		return colortexid;
	}
	
	public int getPickingColorTexId() {
		return pickingtexid;
	}
	
	public int getDepthTexId() {
		return depthtexid;
	}
	
	@Override
	public void close() {
		 GL11.glDeleteTextures(colortexid);
		 GL11.glDeleteTextures(pickingtexid);
		 GL11.glDeleteTextures(depthtexid);
		 GL30.glDeleteFramebuffers(fboid);
		 fboid = colortexid = pickingtexid = depthtexid = 0;
	}

}
