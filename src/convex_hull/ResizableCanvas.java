package convex_hull;

import java.util.LinkedList;
import java.util.List;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;

class ResizableCanvas extends Canvas {
	
	interface DrawListener {
		abstract public void draw(GraphicsContext gc, double width, double height);
	}

	private List<DrawListener> drawListeners = new LinkedList<>();
	
	public void addDrawListener(DrawListener listener) {
		drawListeners.add(listener);
	}
	 
    public ResizableCanvas(Pane pane) {
		// Bind canvas size to stack pane size.
    	widthProperty().bind(pane.widthProperty());
		heightProperty().bind(pane.heightProperty());
		// Redraw canvas when size changes.
		widthProperty().addListener(evt -> draw());
		heightProperty().addListener(evt -> draw());
    }
    
    public void draw() {
		double width = getWidth();
		double height = getHeight();
		GraphicsContext gc = getGraphicsContext2D();
		drawListeners.forEach((l) -> l.draw(gc, width, height)); 
    }

    @Override
    public boolean isResizable() {
      return true;
    }

    @Override
    public double prefWidth(double height) {
      return getWidth();
    }

    @Override
    public double prefHeight(double width) {
      return getHeight();
    }
  }