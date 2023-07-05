package io.github.stuff_stuffs.river_net_gen;

import io.github.stuff_stuffs.river_net_gen.api.layer.Layer;
import io.github.stuff_stuffs.river_net_gen.api.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.api.layer.RiverData;
import io.github.stuff_stuffs.river_net_gen.api.layer.RiverLayers;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.ImageOut;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import io.github.stuff_stuffs.river_net_gen.impl.util.SHMImpl;

public class Test {
    public static void main(final String[] args) {
        final int seed = 41231;
        final int layerCount = 4;
        final Layer.Basic<PlateType> base = RiverLayers.enclaveDestructor(layerCount + 1, RiverLayers.base(seed, layerCount + 1));
        final Layer.Basic<RiverData> riverBase = RiverLayers.riverBase(seed, layerCount, new Layer.CachingOuter<>(base, 8, layerCount));
        Layer<RiverData> layer = riverBase;
        for (int i = layerCount - 1; i >= 0; i--) {
            final Layer.Basic<RiverData> zoom = RiverLayers.zoom(i, seed, layer);
            layer = new Layer.CachingOuter<>(zoom, 8, i);
        }
        final double scale = 1 / 3.0;
        draw(scale, 0, "triver0.png", layer, false);
    }

    private static void draw(final double scale, final int level, final String filename, final Layer<RiverData> layer, final boolean heightMap) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        ImageOut.draw((x, y) -> {
            final Hex.Coordinate coordinate = Hex.fromCartesian(x * scale, y * scale);
            final SHM.Coordinate shmCoord = shm.fromHex(coordinate);
            final RiverData data = layer.get(shmCoord);
            if (!heightMap && data.outgoing() != null && data.flowRate() > 0.01) {
                final Hex.Coordinate outgoing = shm.toHex(SHMImpl.outerTruncate(shm.add(shmCoord, cache.offset(data.outgoing())), level));
                final Hex.Coordinate center = shm.toHex(SHMImpl.outerTruncate(shmCoord, level));
                final double x0 = center.x();
                final double y0 = center.y();

                final double x1 = outgoing.x();
                final double y1 = outgoing.y();

                if (lineSegDist(x0, y0, x1, y1, x * scale, y * scale) < 0.25) {
                    final int val = (int) (data.flowRate() * 10000);
                    final int clamped = Math.max(Math.min(val, 255), 0);
                    return (clamped) | (clamped << 8) | (clamped << 16);
                }
            }
            if (!heightMap) {
                final Hex.Coordinate center = shm.toHex(SHMImpl.outerTruncate(shmCoord, level));
                final double x0 = center.x();
                final double y0 = center.y();

                for (final Hex.Direction direction : data.incoming().keySet()) {
                    final SHM.Coordinate offset = shm.add(shmCoord, cache.offset(direction));
                    final Hex.Coordinate incoming = shm.toHex(SHMImpl.outerTruncate(offset, level));
                    final RiverData riverData = layer.get(offset);
                    if(riverData.flowRate() < 0.01) {
                        continue;
                    }
                    final double x1 = incoming.x();
                    final double y1 = incoming.y();
                    if (lineSegDist(x0, y0, x1, y1, x * scale, y * scale) < 0.25) {
                        final int val = (int) (riverData.flowRate() * 10000);
                        final int clamped = Math.max(Math.min(val, 255), 0);
                        return (clamped) | (clamped << 8) | (clamped << 16);
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

    public static double lineSegDist(final double vx, final double vy, final double wx, final double wy, final double px, final double py) {
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
