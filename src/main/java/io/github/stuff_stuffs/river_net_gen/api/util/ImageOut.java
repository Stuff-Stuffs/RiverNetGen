package io.github.stuff_stuffs.river_net_gen.api.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public final class ImageOut {
    private ImageOut() {
    }

    public static void draw(final Drawer drawer, final int width, final int height, final String name) {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < width; i++) {
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
