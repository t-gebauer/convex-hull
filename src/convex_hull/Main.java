package convex_hull;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;


public class Main extends Application {
	
	public static void main(String[] args) {
		launch(args);
	}
	
	interface DrawListener {
		abstract public void draw(GraphicsContext gc, double width, double height);
	}
	
	class Point {
		private double x, y;
		private Point n1, n2; // neighbors
		
		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
		
		public double getX() {
			return x;
		}
		
		public double getY() {
			return y;
		}
	}
	
	private List<Point> points = new LinkedList<>();
	private double pointsWidth, pointsHeight;
	private double minX, minY, maxX, maxY;
	
	@Override
	public void start(Stage primaryStage) {
		primaryStage.setTitle("Convex Hull");
		BorderPane root = new BorderPane();
		ResizableCanvas canvas = new ResizableCanvas(root);
		
		canvas.addDrawListener((gc, width, height) -> {
			gc.clearRect(0, 0, width, height);
			
			gc.setStroke(Color.RED);
			gc.strokeLine(0, 0, width, height);
			gc.strokeLine(0, height, width, 0);
			
			double size = Math.max(height * 0.01, 1);
			double border = width * 0.1;
			
			gc.setFill(Color.YELLOW);
			for (Point p: points) {
				double x = ((p.getX() - minX) * (width - (2 * border)) / pointsWidth) + border;
				double y = ((p.getY() - minY) * (height- (2 * border)) / pointsHeight) + border;
				gc.fillOval(x - size , y - size, size, size);
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
                String filePath = null;
                for (File file: db.getFiles()) {
                    filePath = file.getAbsolutePath();
                    System.out.println(filePath);
                    readPointsFromFile(file);
                    canvas.draw();
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
        	double minX = 0, minY = 0, maxX = 0, maxY = 0; // initialized but never used
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
            			if (first_values) {
            				first_values = false;
            				minX = x;
            				maxX = x;
            				minY = value;
            				maxY = value;
            			} else {
            				minX = Math.min(x, minX);
                			maxX = Math.max(x, maxX);
                			minY = Math.min(value, minY);
                			maxY = Math.max(value, maxY);	
            			}
                		points.add(new Point(x, value));
            		}
        		} catch (NumberFormatException e) {
        			e.printStackTrace();
        		}
        	}
        	// update fields
        	this.minX = minX;
        	this.maxX = maxX;
        	this.minY = minY;
        	this.maxY = maxY;
        	pointsWidth = maxX - minX;
        	System.out.println("X: " + minX + " " + maxX + " " + pointsWidth);
        	pointsHeight = maxY - minY;
        	System.out.println("Y: " + minY + " " + maxY + " " + pointsHeight);
        	this.points = points;
        } catch (IOException e) {
        	e.printStackTrace();
        }
	}
}
