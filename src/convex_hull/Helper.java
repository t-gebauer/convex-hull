package convex_hull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;

public class Helper {

	public static void runAndWait(Runnable run) throws InterruptedException, ExecutionException {
		FutureTask<Void> task = new FutureTask<>(run, null);
		Platform.runLater(task);
		task.get();
	}

}
