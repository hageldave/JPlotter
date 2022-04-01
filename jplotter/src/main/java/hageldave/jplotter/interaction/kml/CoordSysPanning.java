package hageldave.jplotter.interaction.kml;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.InteractionConstants;
import hageldave.jplotter.renderers.CoordSysRenderer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

/**
 * The CoordSysPanning class implements a {@link MouseListener}
 * and {@link MouseMotionListener} that realize panning functionality
 * for the coordinate view of the {@link CoordSysRenderer}.
 * When registering this with an {@link JPlotterCanvas} and CoordSysRenderer dragging with the left mouse
 * button over the Canvas while holding down CTRL will set the coordinate view accordingly.
 * <p>
 * Intended use: {@code CoordSysPanning pan = new CoordSysPanning(canvas, coordsys).register(); }
 * <p>
 * Per default the extended modifier mask for a dragging mouse event to trigger
 * panning is {@link InputEvent#CTRL_DOWN_MASK}. 
 * If this is undesired the {@link #extModifierMask} has to be overridden.<br>
 * For example to not need to press any key:
 * <pre>new CoordSysPanning(canvas){{extModifierMask=0;}}.register();</pre>
 * 
 * @author hageldave
 */
public class CoordSysPanning extends MouseAdapter implements InteractionConstants {
	
	protected Point startPoint;
	protected Component canvas;
	protected CoordSysRenderer coordsys;
	protected KeyMaskListener keyMaskListener;
	protected int axes = X_AXIS | Y_AXIS;

	/**
	 * Creates a new {@link CoordSysPanning} for the specified canvas and corresponding coordinate system.
	 * @param canvas displaying the coordsys
	 * @param coordsys the coordinate system to apply the panning in
	 */
	public CoordSysPanning(JPlotterCanvas canvas, CoordSysRenderer coordsys, KeyMaskListener keyMaskListener) {
		this.canvas = canvas.asComponent();
		this.coordsys = coordsys;
		this.keyMaskListener = keyMaskListener;
	}

	public CoordSysPanning(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
		this(canvas, coordsys, new KeyMaskListener(KeyEvent.VK_CONTROL));
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (keyMaskListener.areKeysPressed())
			this.startPoint = e.getPoint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(startPoint!= null && keyMaskListener.areKeysPressed()){
			Point dragPoint = e.getPoint();
			double mouseTx = 0;
			double mouseTy = 0;
			if((axes & X_AXIS) != 0)
				mouseTx = dragPoint.getX()-startPoint.getX();
			if((axes & Y_AXIS) != 0)
				mouseTy = dragPoint.getY()-startPoint.getY();
			startPoint = dragPoint;
			Rectangle2D coordSysFrame = coordsys.getCoordSysArea();
			Rectangle2D coordinateArea = coordsys.getCoordinateView();
			double relativeTx = mouseTx/coordSysFrame.getWidth();
			double relativeTy = mouseTy/coordSysFrame.getHeight();
			double areaTx = relativeTx*coordinateArea.getWidth();
			double areaTy = relativeTy*coordinateArea.getHeight();
			coordsys.setCoordinateView(
					coordinateArea.getMinX()-areaTx, 
					coordinateArea.getMinY()+areaTy,  
					coordinateArea.getMaxX()-areaTx, 
					coordinateArea.getMaxY()+areaTy
			);
			canvas.repaint();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		startPoint = null;
	}

	/**
	 * Sets the axes to which this panning is applied. 
	 * Default are both x and y axis.
	 * @param axes {@link InteractionConstants#X_AXIS}, {@link InteractionConstants#Y_AXIS} or {@code X_AXIS|Y_AXIS}
	 * @return this for chaining
	 */
	public CoordSysPanning setPannedAxes(int axes){
		this.axes = axes;
		return this;
	}
	
	/**
	 * @return the axes this panning applies to, i.e.
	 * {@link InteractionConstants#X_AXIS}, {@link InteractionConstants#Y_AXIS} or {@code X_AXIS|Y_AXIS}
	 */
	public int getPannedAxes() {
		return axes;
	}

	public void setKeyMaskListener(KeyMaskListener keyMaskListener) {
		canvas.removeKeyListener(this.keyMaskListener);
		this.keyMaskListener = keyMaskListener;
		if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
			canvas.addKeyListener(this.keyMaskListener);
	}

	/**
	 * Adds this {@link CoordSysPanning} as {@link MouseListener} and
	 * {@link MouseMotionListener} to the associated canvas.
	 * @return this for chaining
	 */
	public CoordSysPanning register(){
		if( ! Arrays.asList(canvas.getMouseListeners()).contains(this))
			canvas.addMouseListener(this);
		if( ! Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
			canvas.addMouseMotionListener(this);
		if (!Arrays.asList(canvas.getKeyListeners()).contains(this.keyMaskListener))
			canvas.addKeyListener(this.keyMaskListener);
		return this;
	}
	
	/**
	 * Removes this {@link CoordSysPanning} from the associated canvas'
	 * mouse and mouse motion listeners.
	 * @return this for chaining
	 */
	public CoordSysPanning deRegister(){
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		canvas.removeKeyListener(this.keyMaskListener);
		return this;
	}
	
	
}
