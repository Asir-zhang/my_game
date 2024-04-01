import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Run {
    static class Task implements Runnable {
        private static final ScopedValue<String> name = ScopedValue.newInstance();

        @Override
        public void run() {
            System.out.println(STR."名字是：\{name.get()}");
        }
    }


    public static void main(String[] args) {
        try (ExecutorService task = Executors.newVirtualThreadPerTaskExecutor()) {
            task.submit(() -> ScopedValue.runWhere(Task.name, "Jack", new Task()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try (ExecutorService task = Executors.newVirtualThreadPerTaskExecutor()) {
            task.submit(() -> ScopedValue.runWhere(Task.name, "Jill", new Task()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}