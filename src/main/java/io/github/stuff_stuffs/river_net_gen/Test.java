package io.github.stuff_stuffs.river_net_gen;

import io.github.stuff_stuffs.river_net_gen.layer.Layer;
import io.github.stuff_stuffs.river_net_gen.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.layer.RiverData;
import io.github.stuff_stuffs.river_net_gen.layer.RiverLayer;
import io.github.stuff_stuffs.river_net_gen.util.Hex;
import io.github.stuff_stuffs.river_net_gen.util.ImageOut;
import io.github.stuff_stuffs.river_net_gen.util.SHM;

public class Test {
    public static void main(final String[] args) {
        final int seed = 12;
        final int layerCount = 7;
        final Layer.Basic<PlateType> base = RiverLayer.base(seed, layerCount);
        final Layer.Basic<RiverData> riverBase = RiverLayer.riverBase(seed, layerCount, base);
        Layer<RiverData> layer = riverBase;
        for (int i = layerCount - 1; i >= 0; i--) {
            Layer.Basic<RiverData> zoom = RiverLayer.zoom(i, seed, layer);
            Layer.Basic<RiverData> expand = RiverLayer.expand(i, seed, zoom);
            layer = expand;
        }
        final double scale = 3 / 1.0;
        draw(scale, 0, "triver0.png", layer, true);
        //draw(scale, 0, "triver1.png", layer, false);
    }

    private static void draw(final double scale, final int level, final String filename, final Layer<RiverData> layer, final boolean heightMap) {
        final SHM shm = new SHM();
        final SHM.LevelCache cache = new SHM.LevelCache(level);
        ImageOut.draw((x, y) -> {
            final Hex.Coordinate coordinate = Hex.fromCartesian(x * scale, y * scale);
            final SHM.Coordinate shmCoord = shm.fromHex(coordinate);
            final RiverData data = layer.get(shmCoord);
            if (!heightMap && data.outgoing() != null) {
                final Hex.Coordinate outgoing = shm.toHex(SHM.outerTruncate(shm.add(shmCoord, cache.offset(data.outgoing())), level));
                final Hex.Coordinate center = shm.toHex(SHM.outerTruncate(shmCoord, level));
                final double x0 = center.x();
                final double y0 = center.y();

                final double x1 = outgoing.x();
                final double y1 = outgoing.y();

                if (lineSegDist(x0, y0, x1, y1, x * scale, y * scale) < 0.25) {
                    return 0xFFFFFF;
                }
            }
            if (!heightMap) {
                final Hex.Coordinate center = shm.toHex(SHM.outerTruncate(shmCoord, level));
                final double x0 = center.x();
                final double y0 = center.y();

                for (final Hex.Direction direction : data.incoming().keySet()) {
                    final Hex.Coordinate incoming = shm.toHex(SHM.outerTruncate(shm.add(shmCoord, cache.offset(direction)), level));
                    final double x1 = incoming.x();
                    final double y1 = incoming.y();
                    if (lineSegDist(x0, y0, x1, y1, x * scale, y * scale) < 0.25) {
                        return 0xFFFFFF;
                    }
                }
            }
            if (heightMap) {
                if (data.type() == PlateType.CONTINENT) {
                    final double height = data.height();
                    if (!Double.isFinite(height)) {
                        return 0xFF0000;
                    }
                    final int i = Math.min(Math.max((int) (height * 24), 0), 255);
                    return i | (i << 8) | (i << 16);
                }
                return 0xFF;
            } else {
                return data.type() == PlateType.CONTINENT ? 0xFF00 : 0xFF;
            }
        }, 2048, 2048, filename);
    }

    private static double lineSegDist(final double vx, final double vy, final double wx, final double wy, final double px, final double py) {
        final double l2 = (vx - wx) * (vx - wx) + (vy - wy) * (vy - wy);
        final double pvx = px - vx;
        final double pvy = py - vy;
        final double wvx = wx - vx;
        final double wvy = wy - vy;
        final double t = Math.max(0, Math.min(1, (pvx * wvx + pvy * wvy) / l2));
        final double projX = vx + t * wvx;
        final double projY = vy + t * wvy;
        final double dx = px - projX;
        final double dy = py - projY;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
