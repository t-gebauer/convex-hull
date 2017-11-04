package convex_hull;

import javafx.geometry.Point2D;

class PointTransformer {
	private double targetHeight, offsetX, offsetY, scale;

	public PointTransformer(double originWidth, double originHeight, double targetWidth, double targetHeight,
			double originOffsetX, double originOffsetY, double border) {
		this.targetHeight = targetHeight;
		// add a small border
		double borderScale = 1 - (2 * border);
		offsetX = border * targetWidth;
		offsetY = border * targetHeight;
		// scale to maximum possible size
		double ratioOrigin = originWidth / originHeight;
		double ratioTarget = targetWidth / targetHeight;
		if (ratioOrigin > ratioTarget) {
			scale = targetWidth / originWidth * borderScale;
			offsetY = targetHeight / 2 - scale * (originHeight / 2);
		} else {
			scale = targetHeight / originHeight * borderScale;
			offsetX = targetWidth / 2 - scale * (originWidth / 2);
		}
		// translate origin origin...
		offsetX += -scale * originOffsetX;
		offsetY += -scale * originOffsetY;
	}

	public Point2D convert(Point2D point) {
		double x = point.getX() * scale + offsetX;
		double y = point.getY() * scale + offsetY;
		// invert y (screen coordinate origin is top/left)
		return new Point2D(x, targetHeight - y);
	}
}