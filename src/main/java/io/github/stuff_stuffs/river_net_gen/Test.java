package io.github.stuff_stuffs.river_net_gen;

import io.github.stuff_stuffs.river_net_gen.river.impl.util.SHMImpl;
import io.github.stuff_stuffs.river_net_gen.river.layer.Layer;
import io.github.stuff_stuffs.river_net_gen.river.layer.PlateType;
import io.github.stuff_stuffs.river_net_gen.river.layer.RiverData;
import io.github.stuff_stuffs.river_net_gen.river.layer.RiverLayers;
import io.github.stuff_stuffs.river_net_gen.util.Hex;
import io.github.stuff_stuffs.river_net_gen.util.ImageOut;
import io.github.stuff_stuffs.river_net_gen.util.SHM;

public class Test {
    public static void main(final String[] args) {
        final int seed = 777431342;
        final int layerCount = 5;
        final Layer.Basic<PlateType> base = RiverLayers.enclaveDestroyer(layerCount + 1, RiverLayers.base(seed, layerCount + 1));
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
        final double scale = 1 / 6.0;
        draw(scale, "triver", layer, true, true, true, true);
    }

    private static void draw(final double scale, final String prefix, final Layer<RiverData> layer, final boolean terrain, final boolean heightMap, final boolean tiles, final boolean humidity) {
        final SHM.LevelCache baseLevel = SHM.createCache(0);
        final int count = (terrain ? 1 : 0) + (heightMap ? 1 : 0) + (tiles ? 1 : 0) + (humidity ? 1 : 0);
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
        final int humidityId;
        if (humidity) {
            files[id] = prefix + "Humidity.png";
            humidityId = id++;
        } else {
            humidityId = -1;
        }
        ImageOut.draw((x, y, painters) -> {
            final Hex.Coordinate coordinate = Hex.fromCartesian(x * scale, y * scale);
            final SHM.Coordinate shmCoord = SHM.fromHex(coordinate, SHMImpl.MAX_LEVEL);
            final RiverData data = layer.get(shmCoord);
            final int effectiveLevel = data.level();
            if (terrainId != -1 || tilesId != -1) {
                final SHMImpl.CoordinateImpl centerSHM = SHMImpl.outerTruncate(shmCoord, effectiveLevel);
                final Hex.Coordinate center = SHM.toHex(centerSHM);
                final double x0 = center.x();
                final double y0 = center.y();
                boolean terrainAccepted = terrainId == -1;
                boolean tileAccepted = tilesId == -1;
                if (data.outgoing() != null) {
                    final SHM.Coordinate neighbourCoordinate = SHM.add(shmCoord, SHM.shift(baseLevel.offset(data.outgoing()), effectiveLevel));
                    final RiverData outgoingData = layer.get(neighbourCoordinate);
                    final Hex.Coordinate neighbourCenterCoordinate = SHM.toHex(SHM.outerTruncate(neighbourCoordinate, outgoingData.level()));
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
                    SHM.Coordinate offset = SHM.add(centerSHM, SHM.shift(baseLevel.offset(offsetDir), effectiveLevel));
                    RiverData neighbourData = layer.get(offset);
                    final Hex.Coordinate incoming;
                    offsetDir = offsetDir.opposite().rotateC();
                    for (int i = effectiveLevel - 1; i >= neighbourData.level(); i--) {
                        offset = SHM.add(offset, SHM.shift(baseLevel.offset(offsetDir), i));
                        neighbourData = layer.get(offset);
                    }
                    incoming = SHM.toHex(SHMImpl.outerTruncate(offset, neighbourData.level()));
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
            if (humidityId != -1) {
                if (data.type() == PlateType.CONTINENT) {
                    final double h = data.rainfall();
                    final int i = Math.min(Math.max((int) (h * 24), 0), 255);
                    painters[humidityId].accept(i | (i << 8) | (i << 16));
                }
            }
        }, 8192, 8192, files);
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
