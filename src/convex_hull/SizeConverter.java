package convex_hull;

import javafx.geometry.Point2D;

class SizeConverter {
	private double originWidth, originHeight, targetWidth, targetHeight, offsetX, offsetY, border;

	public SizeConverter(double originWidth, double originHeight, double targetWidth, double targetHeight,
			double offsetX, double offsetY, double border) {
		this.originWidth = originWidth;
		this.originHeight = originHeight;
		this.targetWidth = targetWidth;
		this.targetHeight = targetHeight;
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.border = border;
	}

	public Point2D convert(Point2D point) {
		double x = ((point.getX() - offsetX) * (targetWidth - (2 * border)) / originWidth) + border;
		double y = ((point.getY() - offsetY) * (targetHeight - (2 * border)) / originHeight) + border;
		return new Point2D(x, y);
	}
}