package kotowski.mm.backend.ajaj.chat;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Thread.sleep;

public class Spinner {
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final Thread spinnerThread;
    public void stop() {
        active.set(false);
        try {
            spinnerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    Spinner() {
        spinnerThread = new Thread(() -> {
            String[] frames = {"-", "\\", "|", "/"};
            int i = 0;
            while (active.get()) {
                System.out.print("\rMyślę... " + frames[i % frames.length]);
                i++;
                try {
                    sleep(100);
                } catch (InterruptedException ignored) {
                }
            }
            System.out.print("\r"); // clear line when done
        });
        spinnerThread.start();
        active.set(true);
    }
}
