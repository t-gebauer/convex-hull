package convex_hull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	interface DrawListener {
		abstract public void draw(GraphicsContext gc, double width, double height);
	}

	class Point {
		private Point2D p; // this points coordinates
		private Point n1, n2; // neighbors
		private Point2D v1, v2; // vectors p->n1; p->n2

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

		public Point get1() {
			return n1;
		}

		public Point get2() {
			return n2;
		}

		public boolean has1() {
			return n1 != null;
		}

		public boolean has2() {
			return n2 != null;
		}

		public void set1(Point n) {
			n1 = n;
			v1 = n.getPoint().subtract(p);
		}

		public void set2(Point n) {
			n2 = n;
			v2 = n.getPoint().subtract(p);
		}

		public Point2D getV1() {
			return v1;
		}

		public Point2D getV2() {
			return v2;
		}
	}

	private List<Point> points = new LinkedList<>();
	private Set<Point> hullPoints = new HashSet<>();
	private double pointsWidth, pointsHeight;
	private double minX, minY;
	private ResizableCanvas canvas;

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

	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Convex Hull");
		BorderPane root = new BorderPane();
		canvas = new ResizableCanvas(root);

		canvas.addDrawListener((gc, width, height) -> {
			gc.clearRect(0, 0, width, height);

			double size = Math.max(height * 0.01, 1);
			double border = width * 0.1;
			SizeConverter converter = new SizeConverter(pointsWidth, pointsHeight, width, height, minX, minY, border);

			for (Point p : points) {
				if (hullPoints.contains(p)) {
					gc.setFill(Color.RED);
				} else {
					gc.setFill(Color.YELLOW);
				}
				Point2D pConv = converter.convert(p.getPoint());
				gc.fillOval(pConv.getX() - (size / 2), pConv.getY() - (size / 2), size, size);
			}

			for (Point hPoint : hullPoints) {
				if (currentH != null && hPoint.equals(currentH)) {
					gc.setStroke(Color.GREEN);
				} else {
					gc.setStroke(Color.RED);
				}

				if (hPoint.has1()) {
					Point2D h = converter.convert(hPoint.getPoint());
					Point2D n1 = converter.convert(hPoint.get1().getPoint());
					gc.beginPath();
					gc.moveTo(n1.getX(), n1.getY());
					gc.lineTo(h.getX(), h.getY());
					if (hPoint.has2()) {
						Point2D n2 = converter.convert(hPoint.get2().getPoint());
						gc.lineTo(n2.getX(), n2.getY());
					}
					gc.stroke();
				}
			}
		});

		root.getChildren().add(canvas);
		Scene scene = new Scene(root, 400, 400, Color.BLACK);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

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

		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void readPointsFromFile(File file) {
		try (Scanner scanner = new Scanner(file)) {
			List<Point> points = new LinkedList<>();
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
			// these points are definitely part of the hull
			Set<Point> hullPoints = new HashSet<>();
			hullPoints.addAll(minXPoints);
			hullPoints.addAll(maxXPoints);
			hullPoints.addAll(minYPoints);
			hullPoints.addAll(maxYPoints);
			// update fields
			this.hullPoints = hullPoints;
			this.minX = minX;
			this.minY = minY;
			this.pointsWidth = maxX - minX;
			this.pointsHeight = maxY - minY;
			this.points = points;

			canvas.draw();
			if (calculationThread != null) {
				calculationThread.interrupt();
			}
			calculationThread = new Thread(() -> findConvexHull());
			calculationThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private Thread calculationThread = null;

	private Point currentH;

	public static void runAndWait(Runnable run) throws InterruptedException, ExecutionException {
		FutureTask<Void> task = new FutureTask<>(run, null);
		Platform.runLater(task);
		task.get();
	}

	private void findConvexHull() {
		LinkedList<Point> pointQueue = new LinkedList<>(hullPoints);
		while (!pointQueue.isEmpty()) {
			Point h = pointQueue.removeFirst();
			currentH = h;
			boolean changed;
			for (Point p : points) {
				changed = false;

				if (p.equals(h)) {
					continue;
				} else if (!h.has1()) {
					h.set1(p);
				} else if (!h.has2()) {
					h.set2(p);
				} else {
					Point2D v = p.getPoint().subtract(h.getPoint());
					double angleH = h.getV1().angle(h.getV2());
					double angleP1 = h.getV1().angle(v);
					double angleP2 = h.getV2().angle(v);
					if (angleP1 > angleH && angleP1 > angleP2) {
						changed = true;
						h.set2(p);
					} else if (angleP2 > angleH) {
						changed = true;
						h.set1(p);
					}
				}
				// update UI
				if (changed) {
					try {
						runAndWait(() -> canvas.draw());
						Thread.sleep(10);
					} catch (ExecutionException | InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}
			}
			// enqueue new hull points
			if (!hullPoints.contains(h.get1())) {
				hullPoints.add(h.get1());
				pointQueue.addLast(h.get1());
			}
			if (!hullPoints.contains(h.get2())) {
				hullPoints.add(h.get2());
				pointQueue.addLast(h.get2());
			}

			// update UI
			currentH = null;
			try {
				runAndWait(() -> canvas.draw());
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		calculateConvexHull(hullPoints);
	}

	private void calculateConvexHull(Collection<Point> hullPoints) {
		// TODO implement
	}
}
