package hageldave.jplotter.howto;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import hageldave.jplotter.canvas.CoordSysCanvas;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.LinesRenderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PickingRegistry;

public class ParallelCoordinates {

	public static void main(String[] args) {
		// have some multidimensional data
		int dim=5, numSamples=100;
		double[][] samples = new double[numSamples][dim];
		for(int i=0; i<numSamples; i++){
			for(int d=0; d<dim; d++){
				samples[i][d] = Math.random()+d;
			}
		}
		String[] dimNames = IntStream.range(0, dim)
				.mapToObj(i->"Dim"+i).toArray(String[]::new);
		// determine min,max of data
		double[] min = new double[dim];
		double[] max = new double[dim];
		for(int d=0; d<dim; d++){
			int d_=d;
			min[d] = Arrays.stream(samples).mapToDouble(sample->sample[d_])
					.min().getAsDouble();
			max[d] = Arrays.stream(samples).mapToDouble(sample->sample[d_])
					.max().getAsDouble();
		}
		// define axis order
		List<Integer> axisOrder = IntStream.range(0, dim).boxed()
				.collect(Collectors.toList());
		Collections.shuffle(axisOrder);
		// build lines and put into appropriate renderer
		LinesRenderer lineContent = new LinesRenderer();
		PickingRegistry<Lines> picking = new PickingRegistry<>();
		int defaultLineColor = 0xff666666; 
		int highlightColor = 0xffff0000;
		for(int i=0; i<numSamples; i++){
			@SuppressWarnings("resource")
			Lines line = new Lines();
			for(int j=0; j<dim-1; j++){
				int d1=axisOrder.get(j), d2=axisOrder.get(j+1);
				double v1=samples[i][d1], v2=samples[i][d2];
				// values to unitRange
				v1 = (v1-min[d1])/(max[d1]-min[d1]);
				v2 = (v2-min[d2])/(max[d2]-min[d2]);
				line.addSegment(j, v1, j+1, v2).setColor(defaultLineColor);
			}
			lineContent.addItemToRender(line.setGlobalAlphaMultiplier(0.5));
			int pickid = picking.register(line);
			line.getSegments().forEach(seg->seg.setPickColor(pickid));
		}
		
		// use a coordinate system for display
		CoordSysCanvas canvas = new CoordSysCanvas();
		canvas.setPreferredSize(new Dimension(700, 400));
		canvas.setBackground(Color.WHITE);
		canvas.setCoordinateView(0,0,dim-1,1);
		canvas.setContent(lineContent);
		// need to change axis tick marks
		canvas.setTickMarkGenerator((vmin,vmax,desired,vert)->{
			if(vert)
				return Pair.of(new double[0], new String[0]);
			String[] labels = axisOrder.stream()
				.map(d->dimNames[d]).toArray(String[]::new);
			double[] ticks = IntStream.range(0, dim)
				.mapToDouble(i->i*1.0).toArray();
			return Pair.of(ticks,labels);
		});
		
		// interaction
		canvas.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if(!SwingUtilities.isLeftMouseButton(e))
					return;
				// make all lines grey
				lineContent.getItemsToRender().forEach(l->l
					.setGlobalThicknessMultiplier(1)
					.setGlobalAlphaMultiplier(0.5)
					.setDirty()
					.getSegments().forEach(s->s.setColor(defaultLineColor)));
				canvas.scheduleRepaint();
				// get picked obj
				int id = canvas.getPixel(e.getX(), e.getY(), true, 3);
				Lines lines = picking.lookup(id);
				if(lines==null)
					return;
				// highlight
				lines
					.setGlobalThicknessMultiplier(2)
					.setGlobalAlphaMultiplier(1)
					.setDirty()
					.getSegments().forEach(s->s.setColor(highlightColor));
				// bring to front
				lineContent.removeItemToRender(lines);
				lineContent.addItemToRender(lines);
				canvas.scheduleRepaint();
			};
		});
		
		
		// display within a JFrame
		JFrame frame = new JFrame();
		frame.getContentPane().add(canvas);
		frame.setTitle("parallel coordinates");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// code to clean up opengl resources
				canvas.runInContext(()->canvas.close());
			}
		});
		// make visible on AWT event dispatch thread
		SwingUtilities.invokeLater(()->{
			frame.pack();
			frame.setVisible(true);
		});

	}

}
