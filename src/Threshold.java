import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Threshold {
    public static Thread[] createCalculators(int zoom, double mantissa, JPanel panel, int calculationThreshold, Fractal fractal) {
        Thread[] threads = new Thread[calculationThreshold];
        Main.Regulation.check = new boolean[calculationThreshold];
        for (int i = 0; i < threads.length; i++) {
            int finalI = i;
            threads[finalI] = new Thread(() -> Calculator.calculate(finalI, zoom, finalI, mantissa, panel, fractal));
            try {
                threads[finalI].join();
            } catch (InterruptedException e) {
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
            }
        }
        return threads;
    }

    public static void startThreads(Thread[] threads) {
        new Thread(() -> {
            boolean temp;
            for (Thread thread : threads) thread.start();
            while (true) {
                temp = true;
                for (int i = 0; i < threads.length; i++)
                    temp = temp && Main.Regulation.check[i];
                if (!temp) continue;
                try {
                    ImageIO.write(Main.ImageContainer.image, "png", new File("images\\test9.png"));
                    System.out.println("\rimage written");
                    Main.Regulation.isImageGenerated = true;

                    // here u can do recursive function call to create multiple images (define)

                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void displayProgress(JPanel panel, Thread[] threads) {
        Thread printThread = new Thread(() -> {
            boolean temp = false;
            try {
                double lastPercent = 0;
                double currentPercent;
                double time;
                String estimatedTime;
                while (!temp) {
                    currentPercent = Main.Regulation.percentage;
                    time = 50 / (Math.abs(currentPercent - lastPercent)) * (1 - Main.Regulation.percentage * 0.01);
                    estimatedTime = Main.Conversion.doubleToTime(time);

                    if (!Main.Regulation.isImageGenerated) {
                        panel.repaint();
                        System.out.printf("\rimage was generated: %.2f%%. Estimated time: %s", Main.Regulation.percentage, estimatedTime);
                    }
                    else {
                        Main.Regulation.isImageGenerated = false;
                        Main.Regulation.percentage = 0;
                    }

                    temp = true;
                    for (int i = 0; i < threads.length; i++)
                        temp = temp && Main.Regulation.check[i];

                    lastPercent = Main.Regulation.percentage;
                    Thread.sleep(500);
                }
            } catch (Exception ignored) {

            }
        });

        printThread.start();
    }
}
