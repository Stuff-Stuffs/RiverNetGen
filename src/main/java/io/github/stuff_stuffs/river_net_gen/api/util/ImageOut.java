package io.github.stuff_stuffs.river_net_gen.api.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public final class ImageOut {
    private static final NumberFormat FORMAT = new DecimalFormat("#0.0");

    private ImageOut() {
    }

    public static void draw(final Drawer drawer, final int width, final int height, final String name) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final int step = width / 100;
        int prog = 0;
        for (int i = 0; i < width; i++) {
            prog++;
            if (prog >= step) {
                prog = 0;
                System.out.println("Progress: " + FORMAT.format((i / (double) width) * 100));
            }
            for (int j = 0; j < height; j++) {
                image.setRGB(i, j, drawer.draw(i, j));
            }
        }
        try {
            ImageIO.write(image, "png", Path.of("./", name).toFile());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Drawer {
        int draw(int x, int y);
    }
}
