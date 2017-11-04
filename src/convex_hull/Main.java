package convex_hull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

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

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Convex Hull");
		BorderPane root = new BorderPane();
		canvas = new ResizableCanvas(root);
		initDrawListener();
		root.getChildren().add(canvas);

		Scene scene = new Scene(root, 400, 400, Color.BLACK);
		initDragAndDrop(scene);

		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void initDrawListener() {
		canvas.addDrawListener((gc, width, height) -> {
			gc.clearRect(0, 0, width, height);

			double size = Math.max(height * 0.01, 1);
			double border = width * 0.1;
			SizeConverter converter = new SizeConverter(pointsWidth, pointsHeight,
					width, height, minX, minY, border);

			// draw all points
			for (Point p : points) {
				if (p.isHull()) {
					gc.setFill(Color.RED);
					gc.setStroke(Color.RED);
				} else {
					gc.setFill(Color.YELLOW);
				}
				// draw point
				Point2D pConv = converter.convert(p.getPoint());
				gc.fillOval(pConv.getX() - (size / 2), pConv.getY() - (size / 2),
						size, size);
				// draw line to next (if hull)
				if (p.hasNext()) {
					Point2D next = converter.convert(p.next().getPoint());
					gc.strokeLine(pConv.getX(), pConv.getY(), next.getX(), next.getY());
				}
			}

			if (areaText != null) {
				gc.setTextAlign(TextAlignment.CENTER);
				gc.setFill(Color.RED);
				gc.fillText(areaText, 0.5 * height, 0.5 * width);
			}
		});
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
			// TODO we don't really need these lists: save one minX point as the start
			List<Point> minXPoints = new LinkedList<>();
			List<Point> maxXPoints = new LinkedList<>();
			List<Point> minYPoints = new LinkedList<>();
			List<Point> maxYPoints = new LinkedList<>();
			double minX = 0, minY = 0, maxX = 0, maxY = 0;

			double x = 0;
			boolean hasX = false;
			boolean first_values = true;
			while (scanner.hasNext()) {
				double value;
				try {
					value = Double.parseDouble(scanner.next());
					if (!hasX) {
						hasX = true;
						x = value;
					} else {
						hasX = false;
						double y = value;
						Point p = new Point(x, y);
						points.add(p);
						if (x <= minX || first_values) {
							if (x < minX || first_values) {
								minX = x;
								minXPoints.clear();
							}
							minXPoints.add(p);
						}
						if (x >= maxX || first_values) {
							if (x > maxX || first_values) {
								maxX = x;
								maxXPoints.clear();
							}
							maxXPoints.add(p);
						}
						if (y <= minY || first_values) {
							if (y < minY || first_values) {
								minY = y;
								minYPoints.clear();
							}
							minYPoints.add(p);
						}
						if (y >= maxY || first_values) {
							if (y > maxY || first_values) {
								maxY = y;
								maxYPoints.clear();
							}
							maxYPoints.add(p);
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

			canvas.draw();
			if (calculationThread != null) {
				calculationThread.interrupt();
			}
			calculationThread = new Thread(() -> findConvexHull(minXPoints.get(0), points));
			calculationThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void findConvexHull(Point start, List<Point> points) {
		areaText = null;
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
