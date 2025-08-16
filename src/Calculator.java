import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Calculator {
    public static void calculate(int offset, int zoom, int place, double mantissa, JPanel panel, Fractal fractal) {
        // executor service used for faster calculations
        Main.Regulation.executor.execute(() -> {
            for (int x = (int) (offset * Main.MainParameters.width / Main.CalculationParameters.calculationThreshold); x < (int) ((offset + 1) * Main.MainParameters.width / Main.CalculationParameters.calculationThreshold); x++) {
                for (int y = (int) (Main.MainParameters.height - 1); y > -1; y--) {
                    // calculate amount of iterations
                    double iteration = getIterationAndTime(x, y, zoom, mantissa, fractal);

                    int color = iteration >= Main.MainParameters.iterations ? 0 : Color.HSBtoRGB(
                            Main.ImageContainer.HUE.apply((float) iteration),
                            (float) Math.pow(Math.cos((5f * iteration % Main.ImageContainer.COLOR_SCHEME_SIZE) / Main.ImageContainer.COLOR_SCHEME_SIZE * Math.TAU), 2),
                            (float) Math.pow(Math.sin((5f * iteration % Main.ImageContainer.COLOR_SCHEME_SIZE) / Main.ImageContainer.COLOR_SCHEME_SIZE * Math.TAU), 2)
                    );

                    // fill the pixel with black if calculation has reached the limit, otherwise use the color scheme
                    if (iteration == Main.MainParameters.iterations) Main.ImageContainer.image.setRGB(x, y, Color.BLACK.getRGB());
                    else Main.ImageContainer.image.setRGB(x, y, color);

                    // increase percentage
                    Main.Regulation.percentage += 100.0 / Main.MainParameters.width / Main.MainParameters.height;

                    // image gets updated with every pixel that has been generated
                    panel.repaint();

                    // check if thread has completed generating image in its area
                    if (x == (int)((offset + 1) * Main.MainParameters.width / Main.CalculationParameters.calculationThreshold) - 1 && y == 0) Main.Regulation.check[place] = true;
                }
            }
        });
    }

    /**
     * convert frame coordinates to complex plane coordinates with limited accuracy
     * */
    private static double getIterationAndTime(int x, int y, int zoom, double mantissa, Fractal fractal) {
        BigDecimal[] destination = getDestination(x, y, zoom, mantissa);
        return getIterationAndTime(destination[0], destination[1], zoom, fractal);
    }

    private static BigDecimal[] getDestination(int x, int y, int zoom, double mantissa) {
        BigDecimal ZOOM1 = new BigDecimal("10").pow(zoom).setScale(zoom + Main.Regulation.accuracyOffset, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(mantissa)).setScale(zoom + Main.Regulation.accuracyOffset, RoundingMode.HALF_UP);
        BigDecimal cx = ((new BigDecimal(x).subtract(new BigDecimal(Main.MainParameters.width / 2.0))).divide(BigDecimal.valueOf(Math.min(Main.MainParameters.width, Main.MainParameters.height) / 4.0), zoom + Main.Regulation.accuracyOffset, RoundingMode.HALF_UP))
                .divide(ZOOM1, zoom + Main.Regulation.accuracyOffset, RoundingMode.HALF_UP)
                .add(Main.BigDecimalLogic.realConverted);
        BigDecimal cy = ((new BigDecimal(y).subtract(new BigDecimal(Main.MainParameters.height / 2.0))).divide(BigDecimal.valueOf(Math.min(Main.MainParameters.width, Main.MainParameters.height) / 4.0), zoom + Main.Regulation.accuracyOffset, RoundingMode.HALF_UP))
                .divide(ZOOM1, zoom + Main.Regulation.accuracyOffset, RoundingMode.HALF_UP)
                .add(Main.BigDecimalLogic.imaginaryConverted);
        return new BigDecimal[]{cx, cy};
    }

    /**
     * calculate amount of iterations for cx + i * cy. Zoom needed to limit the accuracy of the calculations
     * */
    private static double getIterationAndTime(BigDecimal cx, BigDecimal cy, int zoom, Fractal fractal) {
        BigDecimal scale = new BigDecimal("0").setScale(zoom + 4, RoundingMode.HALF_UP);
        BigDecimal zy = scale;
        BigDecimal zx = scale;
        int iteration = 0;

        // abs(f(f(f(...(f((zx, zy)))...)))) < 2 ^ 1024 (could be bigger)
        while ((zx.multiply(zx)).add(zy.multiply(zy)).compareTo(Main.Constants.INFINITY) < 0 && iteration < Main.MainParameters.iterations) {
            BigDecimal[] output = iterateByFractalType(fractal, zoom, zx, zy, cx, cy);
            assert output != null;
            zx = output[0];
            zy = output[1];
            iteration++;
        }
        return iteration + 1 - Math.log(Math.log(zx.pow(2).add(zy.pow(2)).sqrt(Main.Regulation.accuracyRegulator).doubleValue() * 0.5) / Main.Constants.LOG_2) / Main.Constants.LOG_2;
    }

    private static BigDecimal[] iterateByFractalType(Fractal fractal, int zoom, BigDecimal ...params /*zx, zy, cx, cy*/) {
        switch (fractal) {
            case MANDELBROT -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case CELTIC -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).abs().add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case BURNING_SHIP -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.abs().multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case BUFFALO -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).abs().add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.abs().multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case TRICORN -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(Main.Constants.NEGATIVE_TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)
                };
            }

            case BURNING_SHIP_MANDLEBAR -> {
                BigDecimal xt = params[0].abs().multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case CELTIC_MANDLEBAR -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).abs().add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(Main.Constants.NEGATIVE_TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case BUFFALO_MANDLEBAR -> {
                BigDecimal xt = params[0].abs().multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).abs().add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case PERPENDICULAR_MANDELBROT -> {
                BigDecimal xt = params[0].abs().multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(Main.Constants.NEGATIVE_TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case PERPENDICULAR_BURNING_SHIP -> {
                BigDecimal xt = params[0].multiply(params[1].abs());
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case PERPENDICULAR_CELTIC -> {
                BigDecimal xt = params[0].abs().multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).abs().add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(Main.Constants.NEGATIVE_TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case PERPENDICULAR_BUFFALO -> {
                BigDecimal xt = params[0].multiply(params[1].abs());
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1])).abs().add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(Main.Constants.NEGATIVE_TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case AIRSHIP -> {
                BigDecimal xt = params[0].multiply(params[1].abs());
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1].abs())).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case TAIL -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0].abs().multiply(Main.Constants.NEGATIVE_ONE)
                                .multiply(params[0]))
                                .subtract(params[1]
                                        .multiply(params[1].abs())).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case QUILL -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0].abs()
                                .multiply(params[0]))
                                .subtract(params[1].abs()
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            case SHARK_FIN -> {
                BigDecimal xt = params[0].multiply(params[1]);
                return new BigDecimal[] {
                        (params[0]
                                .multiply(params[0]))
                                .subtract(params[1].abs()
                                        .multiply(params[1])).add(params[2])
                                .setScale(zoom + 4, RoundingMode.HALF_UP),
                        (xt.multiply(BigDecimal.TWO))
                                .add(params[3])
                                .setScale(zoom + 4, RoundingMode.HALF_UP)

                };
            }

            default -> {
                return null;
            }
        }
    }
}

