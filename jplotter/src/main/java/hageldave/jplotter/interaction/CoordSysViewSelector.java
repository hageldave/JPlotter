package hageldave.jplotter.interaction;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * The CoordSysViewSelector class realizes a rectangular 
 * selection tool for {@link CoordSysRenderer}.
 * This enables to drag a selection region within the
 * coordinate area of a {@link CoordSysRenderer} using SHIFT+LMB.
 * The action to be performed with the selected region is up to
 * the implementation of the methods {@link #areaSelected(double, double, double, double)}
 * and {@link #areaSelectedOnGoing(double, double, double, double)}.
 * <p>
 * Intended use, (for example rectangular zooming):
 * <pre>
 * new CoordSysViewSelector(canvas, coordsys) {
 *    public void areaSelected(double minX, double minY, double maxX, double maxY) {
 *       coordsys.setCoordinateView(minX, minY, maxX, maxY);
 *    }
 * }.register();
 * </pre>
 * <p>
 * Per default the extended modifier mask for a dragging mouse event to trigger
 * selection is {@link InputEvent#SHIFT_DOWN_MASK}. 
 * If this is undesired the {@link #extModifierMask} has to be overridden.<br>
 * You may also want to not trigger selection when other modifiers are present. E.g.
 * when CTRL {@link InputEvent#CTRL_DOWN_MASK} is pressed, don't select because CTRL 
 * is already meant for panning.
 * In this case you need to add these modifiers to the exclude list {@link #extModifierMaskExcludes}.
 * For example to not need to press any key:
 * <pre>
 * new CoordSysViewSelector(canvas, coordsys) {
 *    {
 *       extModifierMask=0;
 *       extModifierMaskExcludes.add(InputEvent.CTRL_DOWN_MASK);
 *    }
 *    
 *    public void areaSelected(double minX, double minY, double maxX, double maxY) {
 *       coordsys.setCoordinateView(minX, minY, maxX, maxY);
 *    }
 * }.register();
 * </pre>
 * 
 * @author hageldave
 * @deprecated Replaced by {@link hageldave.jplotter.interaction.kml.CoordSysViewSelector}
 */
@Deprecated
public abstract class CoordSysViewSelector extends MouseAdapter {
	
	protected Component canvas;
	protected CoordSysRenderer coordsys;
	protected CompleteRenderer overlay;
	protected Lines areaBorder = new Lines().setVertexRoundingEnabled(true);
	protected Point2D start,end;
	protected int extModifierMask = InputEvent.SHIFT_DOWN_MASK;
	protected final LinkedList<Integer> extModifierMaskExcludes = new LinkedList<Integer>();
	
	
	public CoordSysViewSelector(JPlotterCanvas canvas, CoordSysRenderer coordsys) {
		this.canvas = canvas.asComponent();
		this.coordsys = coordsys;
		Renderer presentRenderer;
		if((presentRenderer = coordsys.getOverlay()) == null){
			coordsys.setOverlay(this.overlay = new CompleteRenderer());
		} else if(presentRenderer instanceof CompleteRenderer){
			this.overlay = (CompleteRenderer) presentRenderer;
		} else {
			throw new IllegalStateException(
					"The canvas' current overlay renderer is not an instance of CompleteRenderer but "
					+ presentRenderer.getClass().getName() + " which cannot be used with CoordSysAreaSelector.");
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(isTriggerMouseEvent(e, MouseEvent.MOUSE_PRESSED)){
			start = coordsys.transformAWT2CoordSys(e.getPoint(), canvas.getHeight());
			overlay.addItemToRender(areaBorder);
		}
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(start == null || !isTriggerMouseEvent(e, MouseEvent.MOUSE_DRAGGED)){	
			return;
		}
		{
			Rectangle2D coordSysArea = Utils.swapYAxis(coordsys.getCoordSysArea(),canvas.getHeight());
			Point end = e.getPoint();
			// clamp end point to area
			double endX = Utils.clamp(coordSysArea.getMinX(), end.getX(), coordSysArea.getMaxX());
			double endY = Utils.clamp(coordSysArea.getMinY(), end.getY(), coordSysArea.getMaxY());
			this.end = coordsys.transformAWT2CoordSys(new Point((int)endX, (int)endY), canvas.getHeight());
		}
		createSelectionAreaBorder();

		this.areaSelectedOnGoing(
				Math.min(this.start.getX(), this.end.getX()),
				Math.min(this.start.getY(), this.end.getY()),
				Math.max(this.start.getX(), this.end.getX()),
				Math.max(this.start.getY(), this.end.getY())
		);
		if(canvas instanceof JPlotterCanvas)
			((JPlotterCanvas) canvas).scheduleRepaint();
		else
			canvas.repaint();
	}
	
	protected void createSelectionAreaBorder() {
		areaBorder.removeAllSegments();
		int color = coordsys.getColorScheme().getColor2();

		areaBorder.addSegment(this.start.getX(), this.start.getY(), this.start.getX(), this.end.getY()).setColor(color);
		areaBorder.addSegment(this.end.getX(), this.start.getY(), this.end.getX(), this.end.getY()).setColor(color);
		areaBorder.addSegment(this.start.getX(), this.start.getY(), this.end.getX(), this.start.getY()).setColor(color);
		areaBorder.addSegment(this.start.getX(), this.end.getY(), this.end.getX(), this.end.getY()).setColor(color);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		areaBorder.removeAllSegments();
		overlay.lines.removeItemToRender(areaBorder);
		if(start != null && end != null){
			this.areaSelected(
					Math.min(this.start.getX(), this.end.getX()),
					Math.min(this.start.getY(), this.end.getY()),
					Math.max(this.start.getX(), this.end.getX()),
					Math.max(this.start.getY(), this.end.getY())
			);
		}
		canvas.repaint();
		start = null;
		end = null;
	}
	
	protected boolean isTriggerMouseEvent(MouseEvent e, int method){
		if(!SwingUtilities.isLeftMouseButton(e)) {
			return false;
		}
		int modifiers = e.getModifiersEx();
		if(
			(modifiers&extModifierMask) != extModifierMask
			|| 
			extModifierMaskExcludes.stream().anyMatch(mask->(modifiers&mask) == mask))
		{
			return false;
		}
		if(method == MouseEvent.MOUSE_PRESSED){
			Rectangle2D coordSysArea = Utils.swapYAxis(coordsys.getCoordSysArea(),canvas.getHeight());
			return coordSysArea.contains(e.getPoint() );
		}
		return true;
	}
	
	/**
	 * Adds this {@link CoordSysViewSelector} as {@link MouseListener} and
	 * {@link MouseMotionListener} to the associated canvas.
	 * @return this for chaining
	 */
	public CoordSysViewSelector register(){
		if( ! Arrays.asList(canvas.getMouseListeners()).contains(this))
			canvas.addMouseListener(this);
		if( ! Arrays.asList(canvas.getMouseMotionListeners()).contains(this))
			canvas.addMouseMotionListener(this);
		return this;
	}
	
	/**
	 * Removes this {@link CoordSysViewSelector} from the associated canvas'
	 * mouse and mouse motion listeners.
	 * @return this for chaining
	 */
	public CoordSysViewSelector deRegister(){
		canvas.removeMouseListener(this);
		canvas.removeMouseMotionListener(this);
		return this;
	}
	
	/**
	 * Reports on the area currently selected during dragging.
	 * When selection is finished (mouse button released),
	 * {@link #areaSelected(double, double, double, double)}
	 * will be called.
	 * @param minX left boundary of selection
	 * @param minY bottom boundary of selection
	 * @param maxX right boundary of selection
	 * @param maxY top boundary of selection
	 */
	public void areaSelectedOnGoing(double minX, double minY, double maxX, double maxY){
		
	}
	
	/**
	 * Will be called when selection is done (mouse button released).
	 * @param minX left boundary of selection
	 * @param minY bottom boundary of selection
	 * @param maxX right boundary of selection
	 * @param maxY top boundary of selection
	 */
	public abstract void areaSelected(double minX, double minY, double maxX, double maxY);
	
}
