package convex_hull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	private List<Point> points = new LinkedList<>();
	private double pointsWidth, pointsHeight;
	private double minX, minY;
	private ResizableCanvas canvas;
	private String areaText = null;
	private Thread calculationThread = null;
	private Point highlightPoint = null;

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Convex Hull");
		BorderPane root = new BorderPane();
		canvas = new ResizableCanvas(root);
		initCanvas();
		root.getChildren().add(canvas);

		Scene scene = new Scene(root, 400, 400, Color.BLACK);
		initDragAndDrop(scene);

		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void initCanvas() {
		canvas.addDrawListener((gc, width, height) -> {
			gc.clearRect(0, 0, width, height);

			double normalSize = Math.max(Math.min(width, height) * 0.01, 4);
			double border = 0.05;
			PointTransformer converter = new PointTransformer(pointsWidth, pointsHeight,
					width, height, minX, minY, border);

			// draw all points
			double size;
			for (Point p : points) {
				size = normalSize;
				if (p.isHull()) {
					if (p.equals(highlightPoint)) {
						size = 2 * normalSize;
						gc.setFill(Color.WHITE);
					} else {
						gc.setFill(Color.GREEN);
					}
					gc.setStroke(Color.GREEN);
				} else {
					gc.setFill(Color.WHITE);
				}
				Point2D pConv = converter.convert(p.getPoint());
				// draw line to next (if hull)
				if (p.hasNext()) {
					Point2D next = converter.convert(p.next().getPoint());
					gc.strokeLine(pConv.getX(), pConv.getY(), next.getX(), next.getY());
				}
				// draw point
				gc.fillOval(pConv.getX() - (size / 2), pConv.getY() - (size / 2), size, size);
			}

			if (areaText != null) {
				gc.setTextAlign(TextAlignment.CENTER);
				gc.setFill(Color.WHITE);
				gc.fillText(areaText, 0.5 * width, 0.5 * height);
			}
		});
		// schedule animation (cycle through hull points indefinitely)
		Timeline timeline = new Timeline();
		timeline.setCycleCount(Animation.INDEFINITE);
		timeline.getKeyFrames().add(new KeyFrame(Duration.millis(350), ae -> {
			if (highlightPoint != null && highlightPoint.hasNext()) {
				highlightPoint = highlightPoint.next();
				canvas.draw();
			}
		}));
		timeline.play();
	}

	private void initDragAndDrop(Scene scene) {
		scene.setOnDragOver((event) -> {
			Dragboard db = event.getDragboard();
			if (db.hasFiles()) {
				event.acceptTransferModes(TransferMode.COPY);
			} else {
				event.consume();
			}
		});
		// Dropping over surface
		scene.setOnDragDropped((event) -> {
			Dragboard db = event.getDragboard();
			boolean success = false;
			if (db.hasFiles()) {
				success = true;
				for (File file : db.getFiles()) {
					readPointsFromFile(file);
				}
			}
			event.setDropCompleted(success);
			event.consume();
		});
	}

	private void readPointsFromFile(File file) {
		try (Scanner scanner = new Scanner(file)) {
			List<Point> points = new LinkedList<>();
			Point oneMinXPoint = null;
			double minX = 0, minY = 0, maxX = 0, maxY = 0;

			double x = 0;
			boolean hasX = false;
			boolean first_values = true;
			while (scanner.hasNext()) {
				try {
					double value = Double.parseDouble(scanner.next());
					if (!hasX) {
						hasX = true;
						x = value;
					} else {
						hasX = false;
						double y = value;
						Point p = new Point(x, y);
						points.add(p);
						if (x < minX || first_values) {
							minX = x;
							oneMinXPoint = p;
						}
						if (x > maxX || first_values) {
							maxX = x;
						}
						if (y < minY || first_values) {
							minY = y;
						}
						if (y > maxY || first_values) {
							maxY = y;
						}
						first_values = false;
					}
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			// update fields
			this.minX = minX;
			this.minY = minY;
			this.pointsWidth = maxX - minX;
			this.pointsHeight = maxY - minY;
			this.points = points;
			areaText = null;
			highlightPoint = null;
			canvas.draw();

			final Point startPoint = oneMinXPoint;
			if (calculationThread != null) {
				calculationThread.interrupt();
			}
			calculationThread = new Thread(() -> findConvexHull(startPoint, points));
			calculationThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void findConvexHull(Point start, List<Point> points) {
		List<Point> hullPoints = new LinkedList<>();
		// we know where the starting point is -> choose a helper point which guarantees
		// the maximum angle (also defines the search direction)
		start.setPrev(new Point(start.getX(), start.getY() + 1));
		Point current = start;
		while (!current.hasNext()) {
			hullPoints.add(current);

			for (Point p : points) {
				if (p.equals(current)) {
					continue;
				} else if (!current.hasNext()) {
					current.setNext(p);
				} else {
					Point2D v = p.getPoint().subtract(current.getPoint());
					double currentAngle = current.angle();
					double newAngle = current.angle(v);
					if (newAngle > currentAngle) {
						current.setNext(p);
						// TODO save the angle? no need to recalculate for other checks
					}
				}
			}
			current.next().setPrev(current);
			current = current.next();
			// update UI
			if (!updateAndSleep(10)) {
				return;
			}
		}

		calculateConvexArea(hullPoints);
	}

	private boolean updateUI() {
		return updateAndSleep(0);
	}

	private boolean updateAndSleep(long millis) {
		try {
			Helper.runAndWait(() -> canvas.draw());
			Thread.sleep(millis);
		} catch (ExecutionException | InterruptedException e) {
			return false;
		}
		return true;
	}

	private void calculateConvexArea(List<Point> hullPoints) {
		List<Point2D> hullList = new LinkedList<>();
		hullPoints.forEach((p) -> hullList.add(p.getPoint()));
		// calculate area
		double area = convexPolygonArea(hullList);
		areaText = String.valueOf(Math.round(area));
		highlightPoint = hullPoints.get(0);
		updateUI();
	}

	/**
	 * calculates the area of a convex polygon /* (formula from
	 * http://www.mathwords.com/a/area_convex_polygon.htm)
	 * 
	 * @param vertices
	 *            of the polygon.
	 * @return area (positive if vertices in counterclockwise order; negative
	 *         otherwise)
	 */
	private double convexPolygonArea(List<Point2D> vertices) {
		Point2D first = vertices.get(0);
		double sum = 0;
		for (int i = 0; i < vertices.size(); i++) {
			Point2D cur = vertices.get(i);
			Point2D next = (i < vertices.size() - 1 ? vertices.get(i + 1) : first);
			sum += (cur.getX() * next.getY()) - (cur.getY() * next.getX());
		}
		return sum * 0.5;
	}
}
