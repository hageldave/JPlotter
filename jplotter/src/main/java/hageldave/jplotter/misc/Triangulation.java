package hageldave.jplotter.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Triangulation {
	
	static <T> double coordDistBetween(T a, T b, CoordinateGetter<T> coord) {
		return Math.abs(coord.get(b) - coord.get(a));
	}
	
	static <T> double angleBetween(T a, T b, CoordinateGetter<T> x, CoordinateGetter<T> y) {
		return Math.atan2(y.get(b) - y.get(a), x.get(b) - x.get(a));
	}
	
	static <T> double distBetween(T a, T b, CoordinateGetter<T> x, CoordinateGetter<T> y) {
		return square(y.get(b) - y.get(a)) + square(x.get(b) - x.get(a));
	}
	
	static <T> double dist(double x, double y, T dat, CoordinateGetter<T> xg, CoordinateGetter<T> yg) {
		return Math.sqrt(square(x - xg.get(dat)) + square(y - yg.get(dat)));
	}
	
	static double square(double d) {
		return d * d;
	}
	
	public static <T> ArrayList<T[]> getTriangulation(ArrayList<T> points, CoordinateGetter<T> getx, CoordinateGetter<T> gety, T[] emptyArray) {
		// sort locations
		Comparator<T> compX = (a, b) -> {
			return (int) Math.signum(getx.get(a) - getx.get(b));
		};
		Comparator<T> compY = (a, b) -> {
			return (int) Math.signum(gety.get(a) - gety.get(b));
		};
		ArrayList<T> locations = new ArrayList<>(points);
		locations.sort(compX.thenComparing(compY));
		// initial triangulation
		ArrayList<int[]> cells = sweepline(locations, getx, gety);
		// delaunay triangulation
		try {
			cells = delaunayTriangulation(locations, cells, getx, gety);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// cell indices to objects
		ArrayList<T[]> pointcells = cells.stream()
		.map(cell->{
			T[] pointcell = Arrays.copyOf(emptyArray, cell.length);
			for(int i=0; i<cell.length; i++)
				pointcell[i] = locations.get(cell[i]);
			return pointcell;
		})
		.collect(Collectors.toCollection(ArrayList::new));
		return pointcells;
	}
	
	
	
	private static <T> ArrayList<int[]> sweepline(ArrayList<T> datapoints, CoordinateGetter<T> getx, CoordinateGetter<T> gety) {
		ArrayList<int[]> cells = new ArrayList<>(datapoints.size());
		Ringlist<Integer> convexhull = new Ringlist<>();
		cells.add(new int[] { 0, 1, 2 });
		convexhull.add(0);
		convexhull.addAll(gety.get(datapoints.get(1)) >= gety.get(datapoints.get(2)) ? Arrays.asList(1, 2) : Arrays.asList(2, 1));
		for (int i = 3; i < datapoints.size(); i++) {
			// System.out.println("now using point i:" + i);
			T p = datapoints.get(i);
			// get nearest
			int hullIdx = 0;
			double dist = coordDistBetween(p, datapoints.get(convexhull.get(hullIdx)), getx);
			for (int j = 0; j < convexhull.size(); j++) {
				double currentDist = coordDistBetween(p, datapoints.get(convexhull.get(j)), getx);
				if (currentDist < dist) {
					hullIdx = j;
					dist = currentDist;
				}
			}
			// System.out.format("forward min dist:%f for hullpoint:%d%n",dist,
			// convexhull.get(hullIdx));
			// march forward on hull (angle increases)
			convexhull.setCursorAt(hullIdx);
			double lastAngle = angleBetween(datapoints.get(convexhull.get()), p, getx, gety);
			double currentAngle = angleBetween(datapoints.get(convexhull.peekNext()), p, getx, gety);
			int numNewCells = 0;
			while (currentAngle > lastAngle) {
				// System.out.format("forward current:%d, next:%d%n",
				// convexhull.get(), convexhull.peekNext());
				cells.add(new int[] { i, convexhull.get(), convexhull.peekNext() });
				numNewCells++;
				if (numNewCells > 1) { // remove last hull point
					int hp = convexhull.remove(); // sets cursor to next
					convexhull.next();
					// System.out.format("i:%d forward removed:%d now cursor
					// at:%d %n", i,hp, convexhull.get());
				} else { // move forward on hull
					convexhull.insert(i);
					convexhull.next();
					convexhull.next();
					// System.out.format("i:%d forward added to hull:%d now
					// cursor at:%d %n", i,i, convexhull.get());
				}
				// update angles
				lastAngle = currentAngle;
				currentAngle = angleBetween(datapoints.get(convexhull.peekNext()), p, getx, gety);
			}
			// get nearest again (we do not know the index anymore as we changed
			// the hull)
			hullIdx = 0;
			dist = coordDistBetween(p, datapoints.get(convexhull.get(hullIdx)), getx);
			for (int j = 0; j < convexhull.size(); j++) {
				double currentDist = coordDistBetween(datapoints.get(convexhull.get(j)), p, getx);
				if (currentDist < dist) {
					hullIdx = j;
					dist = currentDist;
				}
			}
			// System.out.format("backward min dist:%f for hullpoint:%d%n",dist,
			// convexhull.get(hullIdx));
			boolean alreadyInserted = false;
			convexhull.setCursorAt(hullIdx);
			if (convexhull.get() == i) {
				convexhull.prev();
				alreadyInserted = true;
			}
			// System.out.println("already inserted:" +alreadyInserted +" now
			// cursor at " + convexhull.get());
			// march backward on hull (angle decreases)
			lastAngle = angleBetween(datapoints.get(convexhull.get()), p, getx, gety);
			currentAngle = angleBetween(datapoints.get(convexhull.peekPrev()), p, getx, gety);
			// System.out.println("angles: " + lastAngle*360/3.14f + " " +
			// currentAngle*360/3.14f);
			numNewCells = 0;
			while (currentAngle < lastAngle) {
				// System.out.format("backward current:%d, prev:%d%n",
				// convexhull.get(), convexhull.peekPrev());
				cells.add(new int[] { i, convexhull.get(), convexhull.peekPrev() });
				numNewCells++;
				if (numNewCells > 1 || alreadyInserted) { // remove last hull
															// point
					int hp = convexhull.remove(); // sets cursor to prev
					// System.out.format("i:%d backward removed:%d now cursor
					// at:%d%n", i,hp, convexhull.get());
				} else if (!alreadyInserted) {
					convexhull.add(hullIdx, i);
					convexhull.prev();
					convexhull.prev();
					// System.out.format("i:%d backward added to hull:%d now
					// cursor at:%d%n", i,i, convexhull.get());
					alreadyInserted = true;
				}
				// move backward on hull
				// update angles
				lastAngle = currentAngle;
				currentAngle = angleBetween(datapoints.get(convexhull.peekPrev()), p, getx, gety);
			}
		}
		return cells;
	}
	
	private static <T> ArrayList<int[]> delaunayTriangulation(
			ArrayList<T> locations, 
			ArrayList<int[]> cells, 
			CoordinateGetter<T> getx, 
			CoordinateGetter<T> gety) {
		// execute edge flip alorithm
		HashMap<Integer, Set<Integer>> edgeMap = getEdgeMap(cells);
		HashMap<Integer, Set<int[]>> cellMap = getCellMap(cells);
		boolean triangualtionChanged = true;
		while (triangualtionChanged) {
			triangualtionChanged = false;
			// System.out.println("delaunay: iterating over cells");
			for (int i = 0; i < cells.size(); i++) {
				int[] currentCell = cells.get(i);
				// calculate circumcircle
				double circX = 0;
				double circY = 0;
				double circR = 0;
				{
					double x1 = getx.get(locations.get(currentCell[0]));
					double y1 = gety.get(locations.get(currentCell[0]));
					double x2 = getx.get(locations.get(currentCell[1]));
					double y2 = gety.get(locations.get(currentCell[1]));
					double x3 = getx.get(locations.get(currentCell[2]));
					double y3 = gety.get(locations.get(currentCell[2]));
					double a = x2 - x1;
					double b = y2 - y1;
					double c = x3 - x1;
					double d = y3 - y1;
					double e = a * (x1 + x2) + b * (y1 + y2);
					double f = c * (x1 + x3) + d * (y1 + y3);
					double g = 2 * (a * (y3 - y2) - b * (x3 - x2));
					if (g < 0.000001 && g > -0.000001) {
						// throw new RuntimeException("circumcircle could not be
						// calculated!");
						System.err.println("WARNING: circumcircle may be bad: " + g);
					}
					circX = (d * e - b * f) / g;
					circY = (a * f - c * e) / g;
					circR = dist(circX, circY, locations.get(currentCell[0]), getx,gety);
				}
				// test surrounding points for circumcircle property
				Set<Integer> neighbourPoints = getNeighbouringPointsOfCells(currentCell, edgeMap);
				Set<int[]> neighbourCells = getNeighbouringCellsOfCells(currentCell, cellMap);
				for (Integer neighbor : neighbourPoints) {
					if (dist(circX, circY, locations.get(neighbor), getx,gety) < circR) {
						// find neighbouring cell and flip edge
						// System.out.println("neighbour is in circumcircle: " +
						// neighbor);
						int[] adjCell = findAdjacentCell(currentCell, neighbor, neighbourCells);
						if (adjCell != null) { // point in range and part of
												// adjacent cell
							// System.out.format("flipping: %s %s -> ",
							// Arrays.toString(currentCell),
							// Arrays.toString(adjCell));
							flipEdge(currentCell, adjCell);
							// System.out.format("%s %s%n",
							// Arrays.toString(currentCell),
							// Arrays.toString(adjCell));
							// update neighbours (inefficient)
							edgeMap = getEdgeMap(cells);
							cellMap = getCellMap(cells);
							triangualtionChanged = true;
							break;
						}
					}
				}
				if (triangualtionChanged)
					break;
			}
		}
		return cells;
	}
	
	private static int[] findAdjacentCell(int[] cell, int adjacentPoint, Set<int[]> adjacentCells) {
		for (int[] adjCell : adjacentCells) {
			int index = 0;
			if (adjCell[index++] == adjacentPoint || adjCell[index++] == adjacentPoint
					|| adjCell[index++] == adjacentPoint) {
				index--;
				if (adjCell[(index + 1) % 3] == cell[0] || adjCell[(index + 1) % 3] == cell[1]
						|| adjCell[(index + 1) % 3] == cell[2]) {
					if (adjCell[(index + 2) % 3] == cell[0] || adjCell[(index + 2) % 3] == cell[1]
							|| adjCell[(index + 2) % 3] == cell[2]) {
						return adjCell;
					}
				}
			}
		}
		return null;
	}
	
	private static HashMap<Integer, Set<int[]>> getCellMap(ArrayList<int[]> cells) {
		HashMap<Integer, Set<int[]>> cellMap = new HashMap<>();
		for (int[] cell : cells) {
			// point 0
			Set<int[]> adjacentCells = cellMap.get(cell[0]);
			if (adjacentCells == null) {
				adjacentCells = new HashSet<>();
				cellMap.put(cell[0], adjacentCells);
			}
			adjacentCells.add(cell);
			// point 1
			adjacentCells = cellMap.get(cell[1]);
			if (adjacentCells == null) {
				adjacentCells = new HashSet<>();
				cellMap.put(cell[1], adjacentCells);
			}
			adjacentCells.add(cell);
			// point 1
			adjacentCells = cellMap.get(cell[2]);
			if (adjacentCells == null) {
				adjacentCells = new HashSet<>();
				cellMap.put(cell[2], adjacentCells);
			}
			adjacentCells.add(cell);
		}
		return cellMap;
	}
	
	private static HashMap<Integer, Set<Integer>> getEdgeMap(ArrayList<int[]> cells) {
		HashMap<Integer, Set<Integer>> edgeMap = new HashMap<>();
		for (int[] cell : cells) {
			// point 0
			int p = cell[0];
			Set<Integer> edges = edgeMap.get(p);
			if (edges == null) {
				edges = new HashSet<>();
				edgeMap.put(p, edges);
			}
			edges.add(cell[1]);
			edges.add(cell[2]);
			// point 1
			p = cell[1];
			edges = edgeMap.get(p);
			if (edges == null) {
				edges = new HashSet<>();
				edgeMap.put(p, edges);
			}
			edges.add(cell[0]);
			edges.add(cell[2]);
			// point 2
			p = cell[2];
			edges = edgeMap.get(p);
			if (edges == null) {
				edges = new HashSet<>();
				edgeMap.put(p, edges);
			}
			edges.add(cell[0]);
			edges.add(cell[1]);
		}
		return edgeMap;
	}
	
	private static void flipEdge(int[] cell1, int[] cell2) {
		int index1 = 0;
		int index2 = 0;
		if (arrayContains(cell2, cell1[index1++]) && arrayContains(cell2, cell1[index1++])
				&& arrayContains(cell2, cell1[index1++]))
			throw new RuntimeException("bad flip ");
		index1--;
		if (arrayContains(cell1, cell2[index2++]) && arrayContains(cell1, cell2[index2++])
				&& arrayContains(cell1, cell2[index2++]))
			throw new RuntimeException("bad flip");
		index2--;
		int seperatePoint1 = cell1[index1];
		int seperatePoint2 = cell2[index2];
		int commonPoint1 = cell1[(index1 + 1) % 3];
		int commonPoint2 = cell1[(index1 + 2) % 3];
		cell1[0] = cell2[0] = seperatePoint1;
		cell1[1] = cell2[1] = seperatePoint2;
		cell1[2] = commonPoint1;
		cell2[2] = commonPoint2;
	}
	
	private static Set<int[]> getNeighbouringCellsOfCells(int[] cell, HashMap<Integer, Set<int[]>> cellMap) {
		Set<int[]> neighbours = new HashSet<>();
		neighbours.addAll(cellMap.get(cell[0]));
		neighbours.addAll(cellMap.get(cell[1]));
		neighbours.addAll(cellMap.get(cell[2]));
		neighbours.remove(cell);
		return neighbours;
	}
	
	private static Set<Integer> getNeighbouringPointsOfCells(int[] cell, HashMap<Integer, Set<Integer>> edgeMap) {
		Set<Integer> neighbours = new HashSet<>();
		neighbours.addAll(edgeMap.get(cell[0]));
		neighbours.addAll(edgeMap.get(cell[1]));
		neighbours.addAll(edgeMap.get(cell[2]));
		neighbours.remove(cell[0]);
		neighbours.remove(cell[1]);
		neighbours.remove(cell[2]);
		return neighbours;
	}
	
	private static boolean arrayContains(int[] array, int element) {
		for (int el : array) {
			if (el == element)
				return true;
		}
		return false;
	}
	
	private static class Ringlist<T> extends ArrayList<T> {
		private static final long serialVersionUID = 1L;
		
		private int cursor = 0;
		
		public void resetCursor() {
			cursor = 0;
		}
		
		public void setCursorAt(int i) {
			checkIndex(i);
			cursor = i;
		}
		
		public int getCursor() {
			return cursor;
		}
		
		public T get() {
			return this.get(cursor);
		}
		
		public void set(T el) {
			set(cursor, el);
		}
		
		public void insert(T el) {
			add(cursor + 1, el);
		}
		
		public T remove() {
			return remove(cursor);
		}
		
		public T next() {
			return this.get(incrementCursor());
		}
		
		public T prev() {
			return this.get(decrementCursor());
		}
		
		public T peekNext() {
			return get((cursor + 1) % size());
		}
		
		public T peekPrev() {
			return get((cursor + size() - 1) % size());
		}
		
		@Override
		public void add(int index, T element) {
			if (index <= cursor && size() > 0)
				cursor++;
			super.add(index, element);
		}
		
		@Override
		public boolean addAll(int index, Collection<? extends T> c) {
			if (index <= cursor && size() > 0)
				cursor += c.size();
			return super.addAll(index, c);
		}
		
		@Override
		public T remove(int index) {
			T el = super.remove(index);
			if (size() == 0)
				resetCursor();
			else if (index <= cursor)
				decrementCursor();
			return el;
		}
		
		@Override
		public boolean remove(Object o) {
			if (o == null) {
				for (int index = 0; index < size(); index++)
					if (get(index) == null) {
						remove(index);
						return true;
					}
			} else {
				for (int index = 0; index < size(); index++)
					if (o.equals(get(index))) {
						remove(index);
						return true;
					}
			}
			return false;
		}
		
		@Override
		public boolean removeAll(Collection<?> c) {
			Objects.requireNonNull(c);
			boolean modified = false;
			Iterator<T> iter = this.iterator();
			while (iter.hasNext()) {
				if (c.contains(iter.next())) {
					iter.remove();
					modified = true;
				}
			}
			return modified;
		}
		
		private void checkIndex(int index) {
			if (index < 0 || index >= this.size()) {
				throw new IndexOutOfBoundsException("index not in range: " + index);
			}
		}
		
		protected int incrementCursor() {
			cursor = (cursor + 1) % this.size();
			return cursor;
		}
		
		protected int decrementCursor() {
			cursor = (cursor + this.size() - 1) % this.size();
			return cursor;
		}
	}
	
	public static interface CoordinateGetter<T> {
		public double get(T obj);
	}
}
