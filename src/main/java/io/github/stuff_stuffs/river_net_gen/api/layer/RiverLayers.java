package io.github.stuff_stuffs.river_net_gen.api.layer;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.NeighbourhoodFactory;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourChooser;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourhoodPredicate;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourhoodWalker;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class RiverLayers {
    private static final Hex.Direction[] DIRECTIONS = Hex.Direction.values();
    public static final NeighbourhoodPredicate<PlateType> ENCLAVE_DESTROYER = new NeighbourhoodPredicate<>(NeighbourhoodPredicate.Mode.OR, false) {
        @Override
        protected boolean testElement(final PlateType val, final Neighbourhood<PlateType> neighbourhood, final int s) {
            return val == PlateType.OCEAN;
        }
    };
    public static final NeighbourChooser<PlateType> OUTGOING_BASE = new NeighbourChooser<>() {
        @Override
        protected double weight(final PlateType val, final PlateType center, final Hex.Direction direction, final Neighbourhood<PlateType> neighbourhood, final long seed) {
            if (center == PlateType.CONTINENT && val == PlateType.OCEAN) {
                return 1;
            }
            return 0;
        }
    };
    public static final NeighbourChooser<RiverData> OUTGOING_GROW = new NeighbourChooser<>() {
        @Override
        protected double weight(final RiverData val, final RiverData center, final Hex.Direction direction, final Neighbourhood<RiverData> neighbourhood, final long seed) {
            if (val.outgoing() != null) {
                return 1;
            }
            return 0;
        }
    };
    public static final NeighbourhoodWalker<WalkerNode, WalkerNode, RiverLayers.Node, RiverData> FILL_DATA = new NeighbourhoodWalker<>(WalkerNode.class, WalkerNode.class) {
        @Override
        protected @Nullable WalkerNode start(final int s, final Neighbourhood<Node> neighbourhood, final RiverData context) {
            final Node node = neighbourhood.get(s);
            if (node.incoming.isEmpty()) {
                final WalkerNode walkerNode = new WalkerNode(context.height() + 1, node.depth, context.height() + 1, node.outgoing);
                walkerNode.tiles = 1;
                walkerNode.requiredFlowRate = 0;
                return walkerNode;
            }
            boolean internal = false;
            for (final Hex.Direction direction : node.incoming.keySet()) {
                if (neighbourhood.inCenterCluster(neighbourhood.offset(s, direction))) {
                    internal = true;
                    break;
                }
            }
            if (!internal) {
                double height = Double.POSITIVE_INFINITY;
                Hex.Direction incoming = null;
                long tiles = 0;
                double requiredFlowRate = 0;
                for (final Hex.Direction direction : node.incoming.keySet()) {
                    if (neighbourhood.offset(neighbourhood.center(), direction.rotateC()) == s) {
                        incoming = direction;
                        final RiverData.Incoming neighbourIncoming = context.incoming().get(direction);
                        height = neighbourIncoming.height();
                        tiles = 7 * neighbourIncoming.tiles();
                        requiredFlowRate = neighbourIncoming.flowRate();
                        break;
                    }
                }
                final double interpolated = interpolate(node.depth, node.depth + 1, context.height(), height);
                final WalkerNode walkerNode = new WalkerNode(interpolated, node.depth, interpolated, node.outgoing);
                walkerNode.tiles = tiles + 1;
                walkerNode.incomingHeights.put(incoming, new RiverData.Incoming(height, tiles, requiredFlowRate));
                walkerNode.requiredFlowRate = requiredFlowRate;
                return walkerNode;
            }
            return null;
        }

        @Override
        protected @Nullable WalkerNode process(final int s, final WalkerNode[] states, final Neighbourhood<Node> neighbourhood, final RiverData context) {
            double minHeight = Double.POSITIVE_INFINITY;
            int minDepth = Integer.MAX_VALUE;
            boolean edgeAdjacent = false;
            final Node node = neighbourhood.get(s);
            for (final Hex.Direction direction : node.incoming.keySet()) {
                final int n = neighbourhood.offset(s, direction);
                if (neighbourhood.inCenterCluster(n)) {
                    final WalkerNode state = states[n];
                    if (state != null) {
                        minHeight = Math.min(minHeight, state.minHeightAlongPath);
                        minDepth = Math.min(minDepth, state.minDepthAlongPath);
                    } else {
                        return null;
                    }
                } else {
                    final RiverData.Incoming incoming = context.incoming().get(direction);
                    final double interpolated = interpolate(node.depth, node.depth + 1, context.height(), incoming.height());
                    minHeight = Math.min(minHeight, interpolated);
                    minDepth = Math.min(minDepth, node.depth);
                    edgeAdjacent = true;
                }
            }
            final WalkerNode walkerNode;
            if (edgeAdjacent) {
                walkerNode = new WalkerNode(minHeight, minDepth, minHeight, node.outgoing);
            } else {
                final double interpolated = interpolate(node.depth, minDepth, context.height(), minHeight);
                walkerNode = new WalkerNode(minHeight, minDepth, interpolated, node.outgoing);
            }
            double requiredFlowRate = 0;
            long tiles = 1;
            for (final Hex.Direction direction : node.incoming.keySet()) {
                final int n = neighbourhood.offset(s, direction);
                if (neighbourhood.inCenterCluster(n)) {
                    final double flowRate = states[n].requiredFlowRate;
                    requiredFlowRate = requiredFlowRate + flowRate;
                    tiles = tiles + states[n].tiles;
                    walkerNode.incomingHeights.put(direction, new RiverData.Incoming(states[n].height, states[n].tiles, flowRate));
                } else {
                    final RiverData.Incoming incoming = context.incoming().get(direction);
                    final long expandedTiles = incoming.tiles() * 7;
                    walkerNode.incomingHeights.put(direction, new RiverData.Incoming(incoming.height(), expandedTiles, incoming.flowRate()));
                    requiredFlowRate = requiredFlowRate + incoming.flowRate();
                    tiles = tiles + expandedTiles;
                }
            }
            walkerNode.requiredFlowRate = requiredFlowRate;
            walkerNode.tiles = tiles;
            return walkerNode;
        }

        @Override
        protected WalkerNode finish(final int s, final WalkerNode val, final Neighbourhood<Node> neighbourhood, final RiverData context) {
            return val;
        }
    };
    public static final NeighbourhoodWalker<RiverData, WalkerNode, WalkerNode, RiverData> FLOW_FILL = new NeighbourhoodWalker<>(RiverData.class, WalkerNode.class) {
        @Override
        protected @Nullable WalkerNode start(final int s, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
            final WalkerNode node = neighbourhood.get(s);
            final Hex.Direction outgoing = node.outgoing;
            if (outgoing == null || !neighbourhood.inCenterCluster(neighbourhood.offset(s, outgoing))) {
                node.flowRate = context.flowRate();
                return node;
            }
            return null;
        }

        @Override
        protected @Nullable WalkerNode process(final int s, final WalkerNode[] states, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
            final WalkerNode node = neighbourhood.get(s);
            final int offset = neighbourhood.offset(s, node.outgoing);
            final WalkerNode parent = states[offset];
            if (parent != null) {
                final double overFlow = parent.flowRate - parent.requiredFlowRate;
                node.flowRate = node.requiredFlowRate + overFlow * (node.tiles / (double) (parent.tiles));
                return node;
            }
            return null;
        }

        @Override
        protected RiverData finish(final int s, final WalkerNode val, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
            return new RiverData(context.type(), val.incomingHeights, val.outgoing, val.height, val.flowRate, val.tiles);
        }
    };

    public static Layer.Basic<PlateType> base(final int seed, final int level) {
        final SHM.LevelCache cache = SHM.createCache(level);
        final Hash.Strategy<SHM.Coordinate> strategy = cache.outer();
        return new Layer.Basic<>(coordinate -> plateOfCoordinate(coordinate, seed, strategy));
    }

    public static Layer.Basic<PlateType> enclaveDestructor(final int level, final Layer<PlateType> prev) {
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = prev.get(coordinate);
            if (type == PlateType.CONTINENT) {
                if (ENCLAVE_DESTROYER.test(factory.build(coordinate, prev))) {
                    return PlateType.CONTINENT;
                } else {
                    return PlateType.OCEAN;
                }
            }
            return PlateType.OCEAN;
        });
    }

    private static PlateType plateOfCoordinate(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        return (HashCommon.mix(strategy.hashCode(coordinate) ^ seed) & 31) < 14 ? PlateType.CONTINENT : PlateType.OCEAN;
    }

    public static Layer.Basic<RiverData> riverBase(final int seed, final int level, final Layer<PlateType> prev) {
        final SHM shm = SHM.create(level);
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = prev.get(coordinate);
            if (type == PlateType.OCEAN) {
                final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                double flowSum = 0;
                for (final Hex.Direction direction : DIRECTIONS) {
                    final SHM.Coordinate offset = shm.add(coordinate, cache.offset(direction));
                    final Hex.Direction adjacentOutgoing = outgoingBase(offset, factory.build(offset, prev), seed, cache);
                    if (adjacentOutgoing != null && adjacentOutgoing.opposite() == direction) {
                        final double flowBase = flowBase(offset, seed, cache.outer());
                        incoming.put(direction, new RiverData.Incoming(1, 1, flowBase));
                        flowSum = flowSum + flowBase;
                    }
                }
                return new RiverData(PlateType.OCEAN, incoming, null, 0, flowSum, 1);
            }
            final Hex.@Nullable Direction chosen = outgoingBase(coordinate, factory.build(coordinate, prev), seed, cache);
            return new RiverData(PlateType.CONTINENT, Collections.emptyMap(), chosen, 1, flowBase(coordinate, seed, cache.outer()), 1);
        });
    }

    private static double flowBase(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        final int hashCode = strategy.hashCode(coordinate);
        final int start = HashCommon.mix(seed ^ hashCode) ^ HashCommon.murmurHash3(hashCode);
        final long data = HashCommon.murmurHash3(HashCommon.mix((long) start | (((long) start) << 32L)) + 123456);
        return 1 + (data >>> 11) * 0x1.0p-53;
    }

    public static Layer.Basic<RiverData> grow(final int seed, final int level, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        return new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            if (data.type() == PlateType.OCEAN) {
                if (data.incoming().isEmpty()) {
                    return data;
                } else {
                    final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                    for (final Hex.Direction direction : data.incoming().keySet()) {
                        final SHM.Coordinate offset = shm.add(coordinate, cache.offset(direction));
                        final RiverData neighbourData = prev.get(offset);
                        incoming.put(direction, new RiverData.Incoming(1, neighbourData.tiles(), neighbourData.flowRate()));
                    }
                    return new RiverData(PlateType.OCEAN, incoming, null, 0, data.flowRate(), data.tiles());
                }
            }
            if (data.outgoing() != null) {
                final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                for (final Hex.Direction direction : DIRECTIONS) {
                    if (direction == data.outgoing()) {
                        continue;
                    }
                    final SHM.Coordinate neighbour = shm.add(coordinate, cache.offset(direction));
                    final Neighbourhood<RiverData> neighbourhood = factory.build(neighbour, prev);
                    final RiverData neighbourData = neighbourhood.get(neighbourhood.center());
                    if (neighbourData.type() == PlateType.OCEAN || neighbourData.outgoing() != null) {
                        continue;
                    }
                    final Hex.@Nullable Direction chosen = outgoingGrow(neighbour, neighbourhood, cache.outer(), seed);
                    if (chosen == direction.opposite()) {
                        incoming.put(direction, new RiverData.Incoming(data.height() + 1, 1, flowBase(neighbour, seed, cache.outer())));
                    }
                }
                if (incoming.isEmpty()) {
                    return data;
                } else {
                    return new RiverData(PlateType.CONTINENT, incoming, data.outgoing(), data.height(), data.flowRate(), data.tiles());
                }
            }
            final Neighbourhood<RiverData> neighbourhood = factory.build(coordinate, prev);
            final Hex.@Nullable Direction chosen = outgoingGrow(coordinate, neighbourhood, cache.outer(), seed);
            if (chosen != null) {
                final RiverData neighbourData = neighbourhood.get(neighbourhood.center(), chosen);
                return new RiverData(PlateType.CONTINENT, Collections.emptyMap(), chosen, neighbourData.height() + 1, flowBase(coordinate, seed, cache.outer()), 1);
            }
            return data;
        });
    }

    public static Layer.Basic<RiverData> propagate(final int seed, final int level, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        return new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            double base = 0;
            long tiles = 1;
            if (data.incoming().isEmpty()) {
                return data;
            }
            if (data.type() == PlateType.CONTINENT) {
                base = flowBase(coordinate, seed, cache.outer());
            }
            final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
            for (final Map.Entry<Hex.Direction, RiverData.Incoming> entry : data.incoming().entrySet()) {
                final SHM.Coordinate neighbourCoordinate = shm.add(coordinate, cache.offset(entry.getKey()));
                final RiverData riverData = prev.get(neighbourCoordinate);
                incoming.put(entry.getKey(), new RiverData.Incoming(riverData.height(), riverData.tiles(), riverData.flowRate()));
                base = base + riverData.flowRate();
                tiles = tiles + riverData.tiles();
            }
            if (tiles != data.tiles()) {
                return new RiverData(data.type(), incoming, data.outgoing(), data.height(), base, tiles);
            }
            return data;
        });
    }

    public static Layer.Basic<RiverData> zoom(final int level, final int seed, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        final SHM.LevelCache outerCache = SHM.createCache(level + 1);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final Layer<SubRiverData> dataLayer = new Layer.CachingOuter<>(new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            return expandInternal(coordinate, data, level, shm, cache, outerCache, seed, factory, prev);
        }), 8, level + 1);
        return new Layer.Basic<>(coordinate -> dataLayer.get(coordinate).data[coordinate.get(level)]);
    }

    private static SubRiverData expandInternal(final SHM.Coordinate coordinate, final RiverData parentData, final int level, final SHM shm, final SHM.LevelCache cache, final SHM.LevelCache outerCache, final int seed, final NeighbourhoodFactory factory, final Layer<RiverData> prev) {
        if (parentData.type() == PlateType.OCEAN) {
            if (parentData.incoming().isEmpty()) {
                return new SubRiverData(new RiverData[]{parentData, parentData, parentData, parentData, parentData, parentData, parentData});
            }
            final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
            final RiverData[] dataArr = new RiverData[7];
            final RiverData empty = new RiverData(PlateType.OCEAN, Collections.emptyMap(), null, 0, 0, 0);
            dataArr[truncated.get(level)] = empty;
            for (final Hex.Direction direction : DIRECTIONS) {
                final SHM.Coordinate offset = shm.add(truncated, cache.offset(direction.rotateC()));
                if (parentData.incoming().containsKey(direction)) {
                    final RiverData.Incoming incoming = parentData.incoming().get(direction);
                    final RiverData riverData = new RiverData(PlateType.OCEAN, Map.of(direction, incoming), null, 0, incoming.flowRate(), 1 + incoming.tiles() * 7);
                    dataArr[offset.get(level)] = riverData;
                } else {
                    dataArr[offset.get(level)] = empty;
                }
            }
            return new SubRiverData(dataArr);
        }
        if (parentData.outgoing() == null) {
            return new SubRiverData(new RiverData[]{parentData, parentData, parentData, parentData, parentData, parentData, parentData});
        }
        final double[] weights = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        final SHM.Coordinate[] incoming = new SHM.Coordinate[7];
        final Hex.Direction[] incomingDirections = new Hex.Direction[7];
        final Node[] nodes = new Node[7];
        for (int i = 0; i < 7; i++) {
            nodes[i] = new Node();
        }
        final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
        final SHM.Coordinate start = shm.add(truncated, cache.offset(parentData.outgoing().rotateC()));
        final Map<SHM.Coordinate, Pair<Hex.Direction, RiverData.Incoming>> incomingPoints = new Object2ReferenceOpenCustomHashMap<>(cache.outer());
        for (final Map.Entry<Hex.Direction, RiverData.Incoming> entry : parentData.incoming().entrySet()) {
            final SHM.Coordinate offset = shm.add(truncated, cache.offset(entry.getKey().rotateC()));
            incomingPoints.put(offset, new ObjectObjectImmutablePair<>(entry.getKey(), entry.getValue()));
        }
        final byte b = start.get(level);
        weights[b] = Double.NEGATIVE_INFINITY;
        incoming[b] = shm.add(start, cache.offset(parentData.outgoing()));
        nodes[b].outgoing = parentData.outgoing();
        nodes[b].depth = 0;
        nodes[b].coordinate = start;
        incomingDirections[b] = parentData.outgoing();
        long state = seed ^ cache.outer().hashCode(start);
        final int[] shuffle = new int[7];
        for (int i = 0; i < 7; i++) {
            shuffle[i] = i;
        }
        shuffle(shuffle, state);
        final Hex.Direction[] shuffledDirections = Arrays.copyOf(DIRECTIONS, DIRECTIONS.length);
        shuffle(shuffledDirections, HashCommon.mix(state ^ 0xFFFF_0007));
        for (final Hex.Direction direction : shuffledDirections) {
            final SHM.Coordinate offset = shm.add(start, cache.offset(direction));
            if (!outerCache.outer().equals(start, offset)) {
                continue;
            }
            final byte index = offset.get(level);
            final double v = weight(parentData.outgoing(), direction);
            if (v < weights[index] || Double.isNaN(weights[index])) {
                weights[index] = v;
                incoming[index] = start;
                incomingDirections[index] = direction.opposite();
            }
        }

        for (int i = 1; i < 7; i++) {
            double min = Double.POSITIVE_INFINITY;
            int minIndex = -1;
            for (int j = 0; j < 7; j++) {
                final int index = shuffle[j];
                if (nodes[index].outgoing == null) {
                    final double weight = weights[index];
                    if (weight < min) {
                        min = weights[index];
                        minIndex = index;
                    }
                }
            }
            final SHM.Coordinate incomingCell = incoming[minIndex];
            nodes[incomingCell.get(level)].incoming.put(incomingDirections[minIndex].opposite(), -1);
            nodes[minIndex].outgoing = incomingDirections[minIndex];
            nodes[minIndex].depth = nodes[incomingCell.get(level)].depth + 1;
            final SHM.Coordinate cursor = shm.add(incomingCell, cache.offset(incomingDirections[minIndex].opposite()));
            nodes[minIndex].coordinate = cursor;
            if (incomingPoints.containsKey(cursor)) {
                final Pair<Hex.Direction, RiverData.Incoming> pair = incomingPoints.get(cursor);
                nodes[minIndex].incoming.put(pair.first(), pair.second().tiles());
            }
            if (nodes[minIndex].depth == 3) {
                continue;
            }
            for (int j = 0; j < DIRECTIONS.length; j++) {
                final Hex.Direction direction = shuffledDirections[j];
                final SHM.Coordinate offset = shm.add(cursor, cache.offset(direction));
                if (!outerCache.outer().equals(start, offset)) {
                    continue;
                }
                final byte index = offset.get(level);
                final double v = weight(incomingDirections[minIndex], direction);
                final double weight = weights[index];
                if (v < weight || Double.isNaN(weights[index])) {
                    weights[index] = v;
                    incoming[index] = cursor;
                    incomingDirections[index] = direction.opposite();
                } else if (v == weight && (state & 1) == 0) {
                    state = HashCommon.mix(state + 123456) ^ HashCommon.mix(state - 123456);
                    incoming[index] = cursor;
                    incomingDirections[index] = direction.opposite();
                }
            }
        }
        final Map<SHM.Coordinate, Node> nodeMap = new Object2ReferenceOpenCustomHashMap<>(outerCache.inner());
        for (int i = 0; i < 7; i++) {
            nodeMap.put(nodes[i].coordinate, nodes[i]);
        }
        final NeighbourhoodWalker.Result<WalkerNode> walk = FILL_DATA.walk(factory.build(truncated, nodeMap::get), parentData);
        final NeighbourhoodWalker.Result<RiverData> flowWalk = FLOW_FILL.walk(factory.build(truncated, co -> walk.raw()[co.get(level)]), parentData);
        return new SubRiverData(flowWalk.raw());
    }

    private static double interpolate(final int depth, final int maxDepth, final double height, final double endHeight) {
        return height + (endHeight - height) * (depth / (maxDepth + 1.0D));
    }

    private static <T> void shuffle(final T[] arr, long seed) {
        for (int i = 0; i < arr.length; i++) {
            final int idx = (((int) seed) & 0x7FFF_FFFF) % arr.length;
            seed = HashCommon.mix(seed + 12345678) ^ HashCommon.mix(seed - 12345678);
            final T cur = arr[i];
            arr[i] = arr[idx];
            arr[idx] = cur;
        }
    }

    private static void shuffle(final int[] arr, long seed) {
        for (int i = 0; i < arr.length; i++) {
            final int idx = (((int) seed) & 0x7FFF_FFFF) % arr.length;
            seed = HashCommon.mix(seed + 12345678) ^ HashCommon.mix(seed - 12345678);
            final int cur = arr[i];
            arr[i] = arr[idx];
            arr[idx] = cur;
        }
    }

    private static double weight(final Hex.Direction outgoing, final Hex.Direction incoming) {
        if (outgoing.opposite() == incoming) {
            return -5;
        }
        if (outgoing.rotateC() == incoming || outgoing.rotateCC() == incoming) {
            return 5;
        }
        return -1;
    }

    private static Hex.@Nullable Direction outgoingBase(final SHM.Coordinate coordinate, final Neighbourhood<PlateType> neighbourhood, final int seed, final SHM.LevelCache cache) {
        return OUTGOING_BASE.choose(neighbourhood, seed + cache.outer().hashCode(coordinate));
    }

    private static Hex.@Nullable Direction outgoingGrow(final SHM.Coordinate coordinate, final Neighbourhood<RiverData> neighbourhood, final Hash.Strategy<SHM.Coordinate> strategy, final int seed) {
        return OUTGOING_GROW.choose(neighbourhood, strategy.hashCode(coordinate) + seed);
    }

    public static final class Node {
        private Hex.@Nullable Direction outgoing;
        private final Object2LongMap<Hex.Direction> incoming = new Object2LongOpenHashMap<>();
        private int depth = -1;
        private SHM.Coordinate coordinate = null;
    }

    public static final class WalkerNode {
        private final Map<Hex.Direction, RiverData.Incoming> incomingHeights = new EnumMap<>(Hex.Direction.class);
        private final double minHeightAlongPath;
        private final int minDepthAlongPath;
        private final double height;
        private final Hex.Direction outgoing;
        private long tiles;
        private double requiredFlowRate = Double.NaN;
        private double flowRate = Double.NaN;

        private WalkerNode(final double minHeightAlongPath, final int minDepthAlongPath, final double height, final Hex.Direction outgoing) {
            this.minHeightAlongPath = minHeightAlongPath;
            this.minDepthAlongPath = minDepthAlongPath;
            this.height = height;
            this.outgoing = outgoing;
        }
    }

    private record SubRiverData(RiverData[] data) {
    }

    private RiverLayers() {
    }
}
