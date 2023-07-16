package io.github.stuff_stuffs.river_net_gen.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.IntConsumer;

public final class ImageOut {
    private static final NumberFormat FORMAT = new DecimalFormat("#0.0");

    private ImageOut() {
    }

    public static void draw(final Drawer drawer, final int width, final int height, final String... names) {
        final BufferedImage[] images = new BufferedImage[names.length];
        final Painter[] painters = new Painter[names.length];
        for (int i = 0; i < names.length; i++) {
            final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            images[i] = image;
            painters[i] = new Painter(image);
        }
        final int step = width / 100;
        int prog = 0;
        for (int i = 0; i < width; i++) {
            prog++;
            if (prog >= step) {
                prog = 0;
                System.out.println("Progress: " + FORMAT.format((i / (double) width) * 100));
            }
            for (int j = 0; j < height; j++) {
                for (Painter painter : painters) {
                    painter.x = i;
                    painter.y = j;
                }
                drawer.draw(i,j, painters);
            }
        }
        for (int i = 0; i < images.length; i++) {
            BufferedImage image = images[i];
            String name = names[i];
            try {
                ImageIO.write(image, "png", Path.of("./", name).toFile());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class Painter implements IntConsumer {
        private final BufferedImage image;
        private int x, y;

        private Painter(final BufferedImage image) {
            this.image = image;
        }

        @Override
        public void accept(final int value) {
            image.setRGB(x, y, value);
        }
    }

    public interface Drawer {
        void draw(int x, int y, IntConsumer[] painters);
    }
}
