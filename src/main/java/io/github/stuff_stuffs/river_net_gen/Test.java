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
        final int seed = 777431342;
        final int layerCount = 6;
        final Layer.Basic<PlateType> base = RiverLayers.enclaveDestructor(layerCount + 1, RiverLayers.base(seed, layerCount + 1));
        Layer.Basic<RiverData> riverBase = RiverLayers.riverBase(seed, layerCount, base);
        for (int i = 0; i < 2; i++) {
            riverBase = RiverLayers.grow(seed, layerCount, riverBase);
        }
        for (int i = 0; i < 2; i++) {
            riverBase = RiverLayers.propagate(seed, layerCount, riverBase);
        }
        Layer<RiverData> layer = riverBase;
        for (int i = layerCount - 1; i >= 0; i--) {
            final Layer.Basic<RiverData> zoom = RiverLayers.zoom(i, seed, layer);
            layer = zoom;
        }
        final double scale = 1 / 4.0;
        draw(scale, "triver", layer, true, true, true);
    }

    private static void draw(final double scale, final String prefix, final Layer<RiverData> layer, final boolean terrain, final boolean heightMap, final boolean tiles) {
        final SHM shm = SHM.create();
        final SHM.LevelCache baseLevel = SHM.createCache(0);
        final int count = (terrain ? 1 : 0) + (heightMap ? 1 : 0) + (tiles ? 1 : 0);
        final String[] files = new String[count];
        int id = 0;
        final int terrainId;
        if (terrain) {
            files[id] = prefix + "Terrain.png";
            terrainId = id++;
        } else {
            terrainId = -1;
        }
        final int heightmapId;
        if (heightMap) {
            files[id] = prefix + "HeightMap.png";
            heightmapId = id++;
        } else {
            heightmapId = -1;
        }
        final int tilesId;
        if (tiles) {
            files[id] = prefix + "Tiles.png";
            tilesId = id++;
        } else {
            tilesId = -1;
        }
        ImageOut.draw((x, y, painters) -> {
            final Hex.Coordinate coordinate = Hex.fromCartesian(x * scale, y * scale);
            final SHM.Coordinate shmCoord = shm.fromHex(coordinate);
            final RiverData data = layer.get(shmCoord);
            final int effectiveLevel = data.level();
            if (terrainId != -1 || tilesId != -1) {
                final SHMImpl.CoordinateImpl centerSHM = SHMImpl.outerTruncate(shmCoord, effectiveLevel);
                final Hex.Coordinate center = shm.toHex(centerSHM);
                final double x0 = center.x();
                final double y0 = center.y();
                boolean terrainAccepted = terrainId == -1;
                boolean tileAccepted = tilesId == -1;
                if (data.outgoing() != null) {
                    final SHM.Coordinate neighbourCoordinate = shm.add(shmCoord, SHM.shift(baseLevel.offset(data.outgoing()), effectiveLevel));
                    final RiverData outgoingData = layer.get(neighbourCoordinate);
                    final Hex.Coordinate neighbourCenterCoordinate = shm.toHex(SHM.outerTruncate(neighbourCoordinate, outgoingData.level()));
                    final double x1 = neighbourCenterCoordinate.x();
                    final double y1 = neighbourCenterCoordinate.y();
                    final double flowWidth = flowRemap(data.flowRate());
                    if (lineSegDist(x0, y0, x1, y1, x * scale, y * scale) < flowWidth / 255.0) {
                        final int val = (int) (flowWidth);
                        final int clamped = Math.max(Math.min(val, 255), 0);
                        final int color = (clamped) | (clamped << 8) | (clamped << 16);
                        if (!terrainAccepted) {
                            painters[terrainId].accept(color);
                            terrainAccepted = true;
                        }
                        if (!tileAccepted) {
                            painters[tilesId].accept(color);
                            tileAccepted = true;
                        }
                    }
                }
                for (final Hex.Direction direction : data.incoming().keySet()) {
                    Hex.Direction offsetDir = direction;
                    SHM.Coordinate offset = shm.add(centerSHM, SHM.shift(baseLevel.offset(offsetDir), effectiveLevel));
                    RiverData neighbourData = layer.get(offset);
                    final Hex.Coordinate incoming;
                    offsetDir = offsetDir.opposite().rotateC();
                    for (int i = effectiveLevel - 1; i >= neighbourData.level(); i--) {
                        offset = shm.add(offset, SHM.shift(baseLevel.offset(offsetDir), i));
                        neighbourData = layer.get(offset);
                    }
                    incoming = shm.toHex(SHMImpl.outerTruncate(offset, neighbourData.level()));
                    final double x1 = incoming.x();
                    final double y1 = incoming.y();
                    final double flowWidth = flowRemap(neighbourData.flowRate());
                    if (lineSegDist(x0, y0, x1, y1, x * scale, y * scale) < flowWidth / 255.0) {
                        final int val = (int) (flowWidth);
                        final int clamped = Math.max(Math.min(val, 255), 0);
                        final int color = (clamped) | (clamped << 8) | (clamped << 16);
                        if (!terrainAccepted) {
                            painters[terrainId].accept(color);
                            terrainAccepted = true;
                        }
                        if (!tileAccepted) {
                            painters[tilesId].accept(color);
                            tileAccepted = true;
                        }
                    }
                }
                if (!terrainAccepted) {
                    painters[terrainId].accept(data.type() == PlateType.CONTINENT ? 0xFF00 : 0xFF);
                }
                if (!tileAccepted) {
                    painters[tilesId].accept(SHM.outerHash(centerSHM, 0));
                }
            }
            if (heightmapId != -1) {
                if (data.type() == PlateType.CONTINENT) {
                    final double height = data.height();
                    final int i = Math.min(Math.max((int) (height * 24), 0), 255);
                    painters[heightmapId].accept(i | (i << 8) | (i << 16));
                }
            }
        }, 4096, 4096, files);
    }

    private static double flowRemap(final double x) {
        if (x < 0.00075) {
            return 0;
        }
        return Math.pow(x, 1 / 3.0) * 255;
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
