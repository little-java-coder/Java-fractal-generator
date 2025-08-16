import java.awt.image.BufferedImage;

import java.math.BigDecimal;

import javax.swing.*;
import java.awt.*;

import java.math.MathContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Main extends JPanel {
    /**
     * you can change main parameters to get different images
     * */
    static class MainParameters {
        public static String real = "-1.732";
        public static String imaginary = "0";
        public static int iterations = 500;
        public static int magnification = 10;
        public static double width = 2560, height = 1440;
        public static double mantissa = 1; // 1 <= mantissa < 10
        public static Fractal fractal = Fractal.CELTIC;
    }

    static class CalculationParameters {
        public static int calculationThreshold = 8;
    }

    static class BigDecimalLogic {
        public static BigDecimal realConverted = new BigDecimal(MainParameters.real).setScale(Main.Regulation.accuracyRegulator.getPrecision(), Main.Regulation.accuracyRegulator.getRoundingMode());
        public static BigDecimal imaginaryConverted = new BigDecimal(MainParameters.imaginary).setScale(Main.Regulation.accuracyRegulator.getPrecision(), Main.Regulation.accuracyRegulator.getRoundingMode());
    }

    static class ImageContainer {
        /**
         *      contains image of the Mandelbrot set
         * */
        public static volatile BufferedImage image = new BufferedImage((int) MainParameters.width, (int) MainParameters.height, BufferedImage.TYPE_INT_ARGB);

        public static final int COLOR_SCHEME_SIZE = 500;
        
        public static final UnaryOperator<Float> HUE = iteration -> (float) Math.pow(Math.sin(iteration / COLOR_SCHEME_SIZE), 2);
    }

    static class Regulation {
        public static String parametersText = """
                        SELECTED PARAMETERS:\
                        
                        c = %s + %s i
                        iterations = %d
                        zoom = %s * 10^%d
                        resolution: %s x %s
                        """;

        public static JFrame mainWindow = new JFrame();

        public static double percentage = 0;

        /**
         * checking if image file has been generated
         * */
        public static boolean isImageGenerated = false;

        /**
         *      checking if calculations were completed in every thread
         * */
        public static boolean[] check = new boolean[CalculationParameters.calculationThreshold];

        /**
         *      executor service used to do many calculations at once
         * */
        public static ExecutorService executor = Executors.newFixedThreadPool(CalculationParameters.calculationThreshold);
        public static int accuracyOffset = 5;
        public static MathContext accuracyRegulator = new MathContext(MainParameters.magnification + 4);
    }

    static class Constants {
        public static BigDecimal FOUR = new BigDecimal(4);
        public static BigDecimal NEGATIVE_TWO = new BigDecimal(-2);
        public static BigDecimal INFINITY = BigDecimal.TEN.pow(308);
        public static BigDecimal NEGATIVE_ONE = new BigDecimal(-1);
        public static double LOG_2 = Math.log(2);
    }

    static class Conversion {
        /**
         * convert double to time format (example: 14111 -> 3h 55min, 2417 -> 40min 17sec)
         * */
        public static String doubleToTime(double value1) {
            double value = Math.abs(value1);
            if (0 <= value && 60 > value)
                return
                        (int)value + "sec";
            else if (60 <= value && 60 * 60 > value)
                return
                        (int)(value / 60 - (value % 60) / 60.0) + "min " + (int)(value % 60) + "sec";
            else if (60 * 60 <= value && 60 * 60 * 86400 > value)
                return
                        (int)(value / 3600) + "h " +
                                (int)((value - (int)(value / 3600) * 3600) / 60) + "min";
            return null;
        }
    }

    Main() {

        System.out.printf(
                Regulation.parametersText,
                MainParameters.real,
                MainParameters.imaginary,
                MainParameters.iterations,
                MainParameters.mantissa,
                MainParameters.magnification,
                MainParameters.width,
                MainParameters.height);

        Regulation.mainWindow.setTitle("Mandelbrot set generator");
        Regulation.mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);

        Regulation.mainWindow.setUndecorated(true);
        Regulation.mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Regulation.mainWindow.pack();
        Regulation.mainWindow.setSize((int) MainParameters.width, (int) MainParameters.height);
        Regulation.mainWindow.getContentPane().setLayout(new BorderLayout());
        Regulation.mainWindow.setLocationRelativeTo(null);
        Regulation.mainWindow.getContentPane().add(this);
        Regulation.mainWindow.setVisible(true);

        define(MainParameters.magnification, MainParameters.mantissa, MainParameters.fractal);
    }

    public void define(int zoom, double mantissa, Fractal fractal) {
        Thread[] threads = Threshold.createCalculators(zoom, mantissa, this, CalculationParameters.calculationThreshold, fractal);
        Threshold.startThreads(threads);
        System.out.println();
        Threshold.displayProgress(this, threads);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(ImageContainer.image, 0, 0, null);
    }

    public static void main(String[] args) {
        new Main();
    }
}
