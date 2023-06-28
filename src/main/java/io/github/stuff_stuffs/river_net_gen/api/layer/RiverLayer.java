package io.github.stuff_stuffs.river_net_gen.api.layer;

import io.github.stuff_stuffs.river_net_gen.api.util.GenUtil;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.RandomCollection;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.PriorityQueue;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RiverLayer {
    private static final Hex.Direction[] DIRECTIONS = Hex.Direction.values();

    public static Layer.Basic<PlateType> base(final int seed, final int level) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final Hash.Strategy<SHM.Coordinate> strategy = cache.outer();
        final SHM shm = SHM.create();
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = plateOfCoordinate(coordinate, seed, strategy);
            if (type == PlateType.CONTINENT) {
                for (final Hex.Direction direction : DIRECTIONS) {
                    final SHM.Coordinate offset = shm.add(coordinate, cache.offset(direction));
                    if (plateOfCoordinate(offset, seed, strategy) == PlateType.OCEAN) {
                        return PlateType.CONTINENT;
                    }
                }
                return PlateType.OCEAN;
            }
            return type;
        });
    }

    private static PlateType plateOfCoordinate(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        return (HashCommon.mix(strategy.hashCode(coordinate) ^ seed) & 31) < 14 ? PlateType.CONTINENT : PlateType.OCEAN;
    }

    public static Layer.Basic<RiverData> riverBase(final int seed, final int level, final Layer<PlateType> prev) {
        final SHM shm = SHM.create(level);
        final SHM.LevelCache cache = SHM.createCache(level);
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = prev.get(coordinate);
            if (type == PlateType.OCEAN) {
                final Object2DoubleMap<Hex.Direction> incoming = new Object2DoubleOpenHashMap<>();
                for (final Hex.Direction direction : DIRECTIONS) {
                    final Hex.Direction adjacentOutgoing = outgoingBase(shm.add(coordinate, cache.offset(direction)), seed, prev, shm, cache);
                    if (adjacentOutgoing != null && adjacentOutgoing.opposite() == direction) {
                        incoming.put(direction, 1);
                    }
                }
                return new RiverData(PlateType.OCEAN, incoming, null, 0);
            }
            final Hex.Direction outgoingDirection = outgoingBase(coordinate, seed, prev, shm, cache);
            return new RiverData(PlateType.CONTINENT, Object2DoubleMaps.emptyMap(), outgoingDirection, 1);
        });
    }

    private static @Nullable Hex.Direction outgoingBase(final SHM.Coordinate coordinate, final int seed, final Layer<PlateType> prev, final SHM shm, final SHM.LevelCache cache) {
        final PlateType type = prev.get(coordinate);
        if (type == PlateType.OCEAN) {
            return null;
        }
        final Hex.Direction[] oceanAdjacent = new Hex.Direction[DIRECTIONS.length];
        int idx = 0;
        for (final Hex.Direction direction : DIRECTIONS) {
            final SHM.Coordinate offset = shm.add(coordinate, cache.offset(direction));
            final PlateType plateType = prev.get(offset);
            if (plateType == PlateType.OCEAN) {
                oceanAdjacent[idx++] = direction;
            }
        }
        if (idx == 0) {
            return null;
        }
        if (idx == 1) {
            return oceanAdjacent[0];
        }
        final int hash = cache.outer().hashCode(coordinate);
        return oceanAdjacent[(HashCommon.murmurHash3(seed + hash) >>> 1) % idx];
    }

    public static Layer.Basic<RiverData> zoom(final int level, final int seed, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        final SHM.LevelCache outerCache = SHM.createCache(level + 1);
        final Layer<SubRiverData> dataLayer = new Layer.CachingOuter<>(new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            return expandInternal(coordinate, data, level, shm, cache, outerCache, seed);
        }), 8, level + 1);
        return new Layer.Basic<>(coordinate -> dataLayer.get(coordinate).data[coordinate.get(level)]);
    }

    public static Layer.Basic<RiverData> expand(final int level, final int seed, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        return new Layer.Basic<>(coordinate -> tryFill(coordinate, seed, prev, shm, cache, level));
    }

    private static RiverData tryFill(final SHM.Coordinate coordinate, final int seed, final Layer<RiverData> prev, final SHM shm, final SHM.LevelCache cache, final int level) {
        final SHM.Coordinate truncate = SHM.outerTruncate(coordinate, level);
        final RiverData data = prev.get(truncate);
        if (data.type() == PlateType.CONTINENT) {
            final Set<Hex.Direction> additionalIncoming = EnumSet.noneOf(Hex.Direction.class);
            for (final Hex.Direction direction : DIRECTIONS) {
                final SHM.Coordinate offset = shm.add(truncate, cache.offset(direction));
                if (fillCheck(offset, seed, prev, shm, cache)) {
                    final @Nullable ObjectDoublePair<Hex.Direction> outgoing = outgoing(offset, seed, prev, shm, cache);
                    if (outgoing != null && outgoing.left().opposite() == direction) {
                        additionalIncoming.add(direction);
                    }
                }
            }
            if (additionalIncoming.isEmpty()) {
                return data;
            }
            final Object2DoubleMap<Hex.Direction> incoming = new Object2DoubleOpenHashMap<>(data.incoming().size() + additionalIncoming.size());
            for (final Object2DoubleMap.Entry<Hex.Direction> entry : data.incoming().object2DoubleEntrySet()) {
                incoming.put(entry.getKey(), entry.getDoubleValue());
            }
            for (final Hex.Direction direction : additionalIncoming) {
                incoming.put(direction, data.height() + 1);
            }
            return new RiverData(PlateType.CONTINENT, incoming, data.outgoing(), data.height());
        } else {
            if (!fillCheck(truncate, seed, prev, shm, cache)) {
                return data;
            }
            final @Nullable ObjectDoublePair<Hex.Direction> outgoing = outgoing(truncate, seed, prev, shm, cache);
            if (outgoing == null) {
                return data;
            }
            return new RiverData(PlateType.CONTINENT, Object2DoubleMaps.emptyMap(), outgoing.left(), outgoing.rightDouble());
        }
    }

    private static boolean fillCheck(final SHM.Coordinate coordinate, final int seed, final Layer<RiverData> prev, final SHM shm, final SHM.LevelCache cache) {
        final RiverData centerData = prev.get(coordinate);
        if (centerData.type() == PlateType.CONTINENT || !centerData.incoming().isEmpty()) {
            return false;
        }
        int landAdjacent = 0;
        for (final Hex.Direction direction : DIRECTIONS) {
            final SHM.Coordinate add = shm.add(coordinate, cache.offset(direction));
            final RiverData data = prev.get(add);
            if (data.type() == PlateType.CONTINENT) {
                landAdjacent++;
            }
        }
        if (landAdjacent < 2) {
            return false;
        }
        return ((HashCommon.mix((seed ^ cache.full().hashCode(coordinate) * 7 + 43)) >>> 1) & 1) == 1;
    }

    private static @Nullable ObjectDoublePair<Hex.Direction> outgoing(final SHM.Coordinate coordinate, final int seed, final Layer<RiverData> prev, final SHM shm, final SHM.LevelCache cache) {
        final ObjectDoublePair<Hex.Direction>[] available = new ObjectDoublePair[DIRECTIONS.length];
        int idx = 0;
        for (final Hex.Direction direction : DIRECTIONS) {
            final SHM.Coordinate add = shm.add(coordinate, cache.offset(direction));
            final RiverData data = prev.get(add);
            if (data.type() == PlateType.CONTINENT) {
                available[idx++] = new ObjectDoubleImmutablePair<>(direction, data.height());
            }
        }
        if (idx == 0) {
            return null;
        }
        if (idx == 1) {
            return available[0];
        }
        return available[(HashCommon.mix(seed + cache.full().hashCode(coordinate)) >>> 1) % idx];
    }

    private static SubRiverData expandInternal(final SHM.Coordinate coordinate, final RiverData data, final int level, final SHM shm, final SHM.LevelCache cache, final SHM.LevelCache outerCache, final int seed) {
        final PlateType type = data.type();
        if (data.type() == PlateType.OCEAN) {
            if (data.incoming().isEmpty()) {
                return SubRiverData.EMPTY_OCEAN;
            }
            final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
            final RiverData[] dataArr = new RiverData[7];
            dataArr[truncated.get(level)] = SubRiverData.EMPTY_OCEAN.data[0];
            for (final Hex.Direction direction : DIRECTIONS) {
                final SHM.Coordinate offset = shm.add(truncated, cache.offset(direction.rotateC()));
                if (data.incoming().containsKey(direction)) {
                    final RiverData riverData = new RiverData(PlateType.OCEAN, Object2DoubleMaps.singleton(direction, data.incoming().getDouble(direction)), null, 0);
                    dataArr[offset.get(level)] = riverData;
                } else {
                    dataArr[offset.get(level)] = SubRiverData.EMPTY_OCEAN.data[0];
                }
            }
            return new SubRiverData(dataArr);
        }
        if (data.outgoing() == null) {
            return SubRiverData.EMPTY_CONTINENT;
        }
        final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
        final RandomCollection<SHM.Coordinate> randomCollection = new RandomCollection<>((long) seed << 32L | (SHM.outerHash(truncated, level) & 0xFFFF_FFFFL));
        final SHM.Coordinate start = shm.add(truncated, cache.offset(data.outgoing().rotateC()));
        randomCollection.add(start);
        long state = HashCommon.mix(seed + 191);
        final Map<SHM.Coordinate, Set<Hex.Direction>> incoming = new Object2ReferenceOpenCustomHashMap<>(outerCache.inner());
        for (final Object2DoubleMap.Entry<Hex.Direction> entry : data.incoming().object2DoubleEntrySet()) {
            final Hex.Direction direction = entry.getKey();
            final SHM.Coordinate c = shm.add(truncated, cache.offset(direction.rotateC()));
            incoming.computeIfAbsent(c, s -> EnumSet.noneOf(Hex.Direction.class)).add(direction);
        }
        final Node startNode = new Node(data.outgoing(), 0);
        final Map<SHM.Coordinate, Node> nodes = new Object2ReferenceOpenCustomHashMap<>(outerCache.inner());
        nodes.put(start, startNode);
        while (!randomCollection.isEmpty()) {
            final SHM.Coordinate popped = randomCollection.pop();
            final Hex.Direction[] available = new Hex.Direction[DIRECTIONS.length];
            int i = 0;
            for (final Hex.Direction direction : DIRECTIONS) {
                final SHM.Coordinate adjacent = shm.add(popped, cache.offset(direction));
                if (!outerCache.outer().equals(adjacent, truncated)) {
                    continue;
                }
                if (!nodes.containsKey(adjacent)) {
                    available[i++] = direction;
                }
            }
            if (i == 0) {
                continue;
            }
            if (i != 1) {
                randomCollection.add(popped);
            }
            final Hex.Direction chosen = GenUtil.choose(available, i, t -> 1, (int) state);
            state = HashCommon.mix(state) * 65535 + 1;
            final SHM.Coordinate adjacent = shm.add(popped, cache.offset(chosen));
            randomCollection.add(adjacent);
            final Node poppedNode = nodes.get(popped);
            poppedNode.incoming.add(chosen);
            final Set<Hex.Direction> directions = incoming.get(adjacent);
            final Node node = new Node(chosen.opposite(), poppedNode.depth + 1);
            if (directions != null) {
                node.incoming.addAll(directions);
            }
            nodes.put(adjacent, node);
        }
        final Map<SHM.Coordinate, ObjectDoublePair<Hex.Direction>> incomingHeights = new Object2ReferenceOpenCustomHashMap<>(outerCache.inner());
        for (final Object2DoubleMap.Entry<Hex.Direction> entry : data.incoming().object2DoubleEntrySet()) {
            final Hex.Direction direction = entry.getKey();
            final SHM.Coordinate c = shm.add(truncated, cache.offset(direction.rotateC()));
            final ObjectDoublePair<Hex.Direction> pair = incomingHeights.get(c);
            if (pair == null) {
                incomingHeights.put(c, new ObjectDoubleImmutablePair<>(direction, entry.getDoubleValue()));
            } else {
                incomingHeights.put(c, new ObjectDoubleImmutablePair<>(pair.first(), Math.min(pair.rightDouble(), entry.getDoubleValue())));
            }
        }
        fillHeights(nodes, incomingHeights, shm, cache, outerCache, data.height(), level);
        final Node center = nodes.get(truncated);
        final RiverData[] dataArr = new RiverData[7];
        dataArr[truncated.get(level)] = new RiverData(type, center.incomingHeights, center.outgoing, center.height);
        for (final Hex.Direction direction : DIRECTIONS) {
            final SHM.Coordinate offset = shm.add(truncated, cache.offset(direction));
            final Node node = nodes.get(offset);
            dataArr[offset.get(level)] = new RiverData(type, node.incomingHeights, node.outgoing, node.height);
        }
        return new SubRiverData(dataArr);
    }

    private static void fillHeights(final Map<SHM.Coordinate, Node> nodes, final Map<SHM.Coordinate, ObjectDoublePair<Hex.Direction>> incoming, final SHM shm, final SHM.LevelCache cache, final SHM.LevelCache outerCache, final double height, final int level) {
        final PriorityQueue<SHM.Coordinate> available = new ObjectArrayFIFOQueue<>();
        for (final Map.Entry<SHM.Coordinate, Node> entry : nodes.entrySet()) {
            final Node node = entry.getValue();
            if (node.incoming.isEmpty()) {
                available.enqueue(entry.getKey());
            } else {
                boolean internal = false;
                for (final Hex.Direction direction : node.incoming) {
                    if (outerCache.outer().equals(entry.getKey(), shm.add(entry.getKey(), cache.offset(direction)))) {
                        internal = true;
                        break;
                    }
                }
                if (!internal) {
                    available.enqueue(entry.getKey());
                }
            }
        }
        final Set<SHM.Coordinate> visited = new ObjectOpenCustomHashSet<>(outerCache.inner());
        while (!available.isEmpty()) {
            final SHM.Coordinate coordinate = available.dequeue();
            visited.add(coordinate);
            final Node node = nodes.get(coordinate);
            if (node.incoming.isEmpty()) {
                node.height = height + 1;
                node.minHeightAlongPath = node.height;
                node.minDepthAlongPath = node.depth;
            } else {
                double minHeight = Double.POSITIVE_INFINITY;
                int minDepth = Integer.MAX_VALUE;
                for (final Hex.Direction direction : node.incoming) {
                    final SHM.Coordinate offset = shm.add(coordinate, cache.offset(direction));
                    if (outerCache.outer().equals(offset, coordinate)) {
                        final Node prevNode = nodes.get(offset);
                        minHeight = Math.min(minHeight, prevNode.minHeightAlongPath);
                        minDepth = Math.min(minDepth, prevNode.minDepthAlongPath);
                    } else {
                        final ObjectDoublePair<Hex.Direction> pair = incoming.get(coordinate);
                        minHeight = Math.min(minHeight, pair.rightDouble());
                        minDepth = Math.min(minDepth, node.depth);
                        node.incomingHeights.put(direction, pair.rightDouble());
                    }
                }
                node.height = interpolate(node.depth, minDepth, height, minHeight);
                node.minHeightAlongPath = minHeight;
                node.minDepthAlongPath = minDepth;
            }
            if (node.outgoing != null) {
                final SHM.Coordinate next = shm.add(coordinate, cache.offset(node.outgoing));
                if (!outerCache.outer().equals(next, coordinate)) {
                    continue;
                }
                final Node nextNode = nodes.get(next);
                nextNode.incomingHeights.put(node.outgoing.opposite(), node.height);
                if (visited.contains(next)) {
                    continue;
                }
                final Set<Hex.Direction> nextIncoming = nextNode.incoming;
                if (nextIncoming.size() == 1) {
                    available.enqueue(next);
                } else {
                    boolean ok = true;
                    for (final Hex.Direction direction : nextIncoming) {
                        final SHM.Coordinate add = shm.add(next, cache.offset(direction));
                        if (!outerCache.outer().equals(add, coordinate)) {
                            continue;
                        }
                        if (!visited.contains(add)) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        available.enqueue(next);
                    }
                }
            }
        }
    }

    private static double interpolate(final int depth, final int maxDepth, final double height, final double endHeight) {
        return height + (endHeight - height) * (depth / (maxDepth + 1.0D));
    }

    private static final class Node {
        private final Set<Hex.Direction> incoming = EnumSet.noneOf(Hex.Direction.class);
        private final Object2DoubleMap<Hex.Direction> incomingHeights = new Object2DoubleOpenHashMap<>();
        private final @Nullable Hex.Direction outgoing;
        private final int depth;
        private double minHeightAlongPath = Double.POSITIVE_INFINITY;
        private int minDepthAlongPath = Integer.MAX_VALUE;
        private double height = Double.NaN;

        private Node(final Hex.@Nullable Direction outgoing, final int depth) {
            this.outgoing = outgoing;
            this.depth = depth;
        }
    }

    private record SubRiverData(RiverData[] data) {
        private static final SubRiverData EMPTY_CONTINENT;
        private static final SubRiverData EMPTY_OCEAN;

        static {
            final RiverData emptyC = new RiverData(PlateType.CONTINENT, Object2DoubleMaps.emptyMap(), null, 1);
            EMPTY_CONTINENT = new SubRiverData(new RiverData[]{emptyC, emptyC, emptyC, emptyC, emptyC, emptyC, emptyC});

            final RiverData emptyO = new RiverData(PlateType.OCEAN, Object2DoubleMaps.emptyMap(), null, 0);
            EMPTY_OCEAN = new SubRiverData(new RiverData[]{emptyO, emptyO, emptyO, emptyO, emptyO, emptyO, emptyO});
        }
    }

    private RiverLayer() {
    }
}
