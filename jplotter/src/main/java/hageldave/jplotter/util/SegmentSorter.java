package hageldave.jplotter.util;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;

import hageldave.jplotter.misc.Contours;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.Lines.SegmentDetails;

/**
 * This is a utility class that provides the {@link #sortSegments(Iterable)} method.
 * It is intended to be used with the segments of {@link Lines} objects and will
 * bring a collection of segments in order, meaning that segments with matching coordinates 
 * on either end are next to each other in the list.
 * This is primarily useful for segments created for example by {@link Contours}, where 
 * the segment generation process does not take care of the segment order.
 * 
 * @author hageldave
 */
public class SegmentSorter {

	/**
	 * Sorts the provided segments.
	 * Sorting means that the algorithm tries to find sequences of segments that form a polyline.
	 * I.e., segments that have matching coordinates (segA.p1 == segB.p0) will be adjacent in the
	 * ordered list.
	 * The algorithm may also turn around segments if needed (segA.p1 == segB.p1) so that matching
	 * coordinates will be p0 and p1.
	 * 
	 * @param segments to sort
	 * @return list of segments where the segments form polylines.
	 */
	public static LinkedList<SegmentDetails> sortSegments(Iterable<SegmentDetails> segments){
		TreeMap<Point2D, LinkedList<SegmentDetails>> map = new TreeMap<>(SegmentSorter::compare);
		// put all segments into the map with their p0 and p1 as keys
		for(SegmentDetails s:segments) {
			addToMap(s.p0, s, map);
			addToMap(s.p1, s, map);
		}
		LinkedList<LinkedList<SegmentDetails>> allLists = new LinkedList<>();
		LinkedList<SegmentDetails> currList = new LinkedList<>();
		allLists.add(currList);
		// retrieve the first segment, then add matching segments
		SegmentDetails seg = map.firstEntry().getValue().getFirst();
		removeFromMap(seg, map);
		currList.add(seg);
		// find matching other segments
		while(!map.isEmpty()) {
			LinkedList<SegmentDetails> matching;
			if((matching=map.get(currList.getFirst().p0)) != null) {
				seg = matching.getFirst();
				removeFromMap(seg, map);
				// may need to turn segment around so that p0 and p1 match
				if(compare(seg.p1, currList.getFirst().p0) != 0) {
					turnAround(seg);
				}
				currList.addFirst(seg);
			} else if((matching=map.get(currList.getLast().p1)) != null){
				seg = matching.getFirst();
				removeFromMap(seg, map);
				// may need to turn segment around so that p0 and p1 match
				if(compare(seg.p0, currList.getLast().p1) != 0) {
					turnAround(seg);
				}
				currList.addLast(seg);
			} else {
				currList = new LinkedList<>();
				allLists.add(currList);
				// retrieve the first segment, then add matching segments
				seg = map.firstEntry().getValue().getFirst();
				removeFromMap(seg, map);
				currList.add(seg);
			}
		}
		return allLists.stream().reduce(new LinkedList<>(),(l1,l2)->{l1.addAll(l2); return l1;});
	}
	
	private static void addToMap(Point2D key, SegmentDetails value, TreeMap<Point2D, LinkedList<SegmentDetails>> map) {
		LinkedList<SegmentDetails> list = map.get(key);
		if(list == null) {
			map.put(key, list=new LinkedList<>());
		}
		list.add(value);
	}
	
	private static void removeFromMap(SegmentDetails value, TreeMap<Point2D, LinkedList<SegmentDetails>> map) {
		LinkedList<SegmentDetails> list = map.get(value.p0);
		if(list != null) {
			list.remove(value);
			if(list.isEmpty()) {
				map.remove(value.p0);
			}
		}
		list = map.get(value.p1);
		if(list != null) {
			list.remove(value);
			if(list.isEmpty()) {
				map.remove(value.p1);
			}
		}
	}
	
	private static void turnAround(SegmentDetails seg) {
		Point2D p0 = seg.p0;
		seg.p0 = seg.p1;
		seg.p1 = p0;
		IntSupplier color0 = seg.color0;
		seg.color0 = seg.color1;
		seg.color1 = color0;
		DoubleSupplier thickness0 = seg.thickness0;
		seg.thickness0 = seg.thickness1;
		seg.thickness1 = thickness0;
	}
	
	
	
	private static int compare(Point2D o1, Point2D o2) {
		int x = Double.compare(o1.getX(), o2.getX());
		return x == 0 ? Double.compare(o1.getY(), o2.getY()) : x; 
	}
	
}
