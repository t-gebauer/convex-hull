package convex_hull;

import javafx.geometry.Point2D;

class Point {
	private Point2D p; // this points coordinates
	private Point prev, next; // neighbors
	private Point2D vPrev, vNext; // vectors to neighbors

	public Point(double x, double y) {
		p = new Point2D(x, y);
	}

	public double getX() {
		return p.getX();
	}

	public double getY() {
		return p.getY();
	}

	public Point2D getPoint() {
		return p;
	}

	public Point prev() {
		return prev;
	}

	public Point next() {
		return next;
	}

	public boolean hasPrev() {
		return prev != null;
	}

	public boolean hasNext() {
		return next != null;
	}

	public void setPrev(Point n) {
		prev = n;
		vPrev = n.getPoint().subtract(p);
	}

	public void setNext(Point n) {
		next = n;
		vNext = n.getPoint().subtract(p);
	}

	/**
	 * 
	 * @return current angle
	 */
	public double angle() {
		return vPrev.angle(vNext);
	}

	/**
	 * 
	 * @param v
	 *            vector from this point to any other point
	 * @return angle between previous hull point and the given vector
	 */
	public double angle(Point2D v) {
		return vPrev.angle(v);
	}

	/**
	 * 
	 * @return whether this point is part of the convex hull
	 */
	public boolean isHull() {
		return prev != null;
	}
}