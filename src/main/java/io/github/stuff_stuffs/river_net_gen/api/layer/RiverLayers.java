package io.github.stuff_stuffs.river_net_gen.api.layer;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.NeighbourhoodFactory;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourChooser;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourWeightedWalker;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourhoodPredicate;
import io.github.stuff_stuffs.river_net_gen.api.neighbour.base.NeighbourhoodWalker;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class RiverLayers {
    private static final double SQRT3_3 = Math.sqrt(3.0) / 3.0;
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
    public static NeighbourWeightedWalker<PathResult, PathTree, Void, CachedExpandData> SPLIT_RIVERS = new NeighbourWeightedWalker<>() {
        @Override
        protected PathResult finish(final PathTree tree, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
            final Node[] nodes = data.nodes;
            for (int i = 0; i < 7; i++) {
                nodes[i].depth = tree.depth[i];
                if (tree.start == i) {
                    nodes[i].outgoing = data.data.outgoing();
                } else {
                    final Hex.Direction direction = tree.outgoing[i];
                    nodes[i].outgoing = tree.outgoing[i];
                    nodes[neighbourhood.offset(i, direction)].incoming.put(direction.opposite(), tree.children[i]);
                }
                final Hex.Direction edge = neighbourhood.from(i);
                if (edge != null) {
                    final RiverData.Incoming incoming = data.data.incoming().get(edge.rotateCC());
                    if (incoming != null) {
                        nodes[i].incoming.put(edge.rotateCC(), incoming.tiles() * 7);
                    }
                }
            }
            return new PathResult(nodes);
        }

        @Override
        protected PathTree connect(int from, final int to, final Hex.Direction outgoing, final PathTree partialResult, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
            partialResult.depth[to] = partialResult.depth[from] + 1;
            partialResult.outgoing[to] = outgoing;
            while (partialResult.outgoing[from] != null) {
                partialResult.children[from] += 1;
                from = neighbourhood.offset(from, partialResult.outgoing[from]);
            }
            partialResult.children[from] += 1;
            return partialResult;
        }

        @Override
        protected PathTree init(final int start, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
            return new PathTree(start);
        }

        @Override
        protected int start(final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
            return neighbourhood.offset(neighbourhood.center(), data.data.outgoing().rotateC());
        }

        @Override
        protected double weight(final int from, final int to, final Hex.Direction fromDirection, final PathTree tree, final Neighbourhood<Void> neighbourhood, final CachedExpandData data, final int seed) {
            final Hex.Direction edge = neighbourhood.from(to);
            double weight = 1;
            if (edge != null) {
                final RiverData.Incoming incoming = data.data.incoming().get(edge.opposite());
                if (incoming != null) {
                    weight = weight + weightAngle(fromDirection, edge.opposite());
                }
            }
            if (from != tree.start) {
                weight = weight + weightAngle(tree.outgoing[from], fromDirection.opposite());
            } else {
                weight = weight + weightAngle(data.data.outgoing(), fromDirection.opposite());
            }
            return weight + 70  * randomDoubleFromLong(HashCommon.mix(data.splitSeed ^ HashCommon.mix(seed -123456789L) ^ HashCommon.mix(from * 63 + 1) ^ HashCommon.mix(to * 127 + 3)));
        }

        private double weightAngle(final Hex.Direction outgoing, final Hex.Direction incoming) {
            if (outgoing == incoming.rotateC() || outgoing == incoming.rotateCC()) {
                return 1;
            }
            if (outgoing == incoming.opposite()) {
                return 100;
            }
            return 50;
        }
    };
    public static final NeighbourhoodWalker<WalkerNode, WalkerNode, RiverLayers.Node, RiverData> FILL_DATA = new NeighbourhoodWalker<>(WalkerNode.class, WalkerNode.class) {
        @Override
        protected @Nullable WalkerNode start(final int s, final Neighbourhood<Node> neighbourhood, final RiverData context) {
            final Node node = neighbourhood.get(s);
            if (node.incoming.isEmpty()) {
                final WalkerNode walkerNode = new WalkerNode(context.height() + 1, node.depth, context.height() + 1, node.outgoing);
                final int hash = SHM.outerHash(neighbourhood.toGlobal(s), 0);
                walkerNode.tiles = randomDoubleFromLong(HashCommon.murmurHash3(hash | (long) hash << 32)) * 2 * context.rainfall()  + 1;
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
                double tiles = 0;
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
            double tiles = 1;
            for (final Hex.Direction direction : node.incoming.keySet()) {
                final int n = neighbourhood.offset(s, direction);
                if (neighbourhood.inCenterCluster(n)) {
                    final double flowRate = states[n].requiredFlowRate;
                    requiredFlowRate = requiredFlowRate + flowRate;
                    tiles = tiles + states[n].tiles;
                    walkerNode.incomingHeights.put(direction, new RiverData.Incoming(states[n].height, states[n].tiles, flowRate));
                } else {
                    final RiverData.Incoming incoming = context.incoming().get(direction);
                    final double expandedTiles = incoming.tiles() * 7;
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
                node.flowRate = node.requiredFlowRate + overFlow * (node.tiles / (double) (parent.tiles + 1));
                return node;
            }
            return null;
        }

        @Override
        protected RiverData finish(final int s, final WalkerNode val, final Neighbourhood<WalkerNode> neighbourhood, final RiverData context) {
            double humidity = val.flowRate;
            for (RiverData.Incoming incoming : val.incomingHeights.values()) {
                humidity = humidity - incoming.flowRate();
            }
            return new RiverData(context.type(), val.incomingHeights, val.outgoing, val.height, val.flowRate, val.tiles, context.level() - 1, (humidity + context.rainfall() * 4) * 0.2);
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
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final SHM.MutableCoordinate mutable = SHM.createMutable();
        final Hash.Strategy<SHM.Coordinate> outer = cache.outer();
        return new Layer.Basic<>(coordinate -> {
            final PlateType type = prev.get(coordinate);
            if (type == PlateType.OCEAN) {
                final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                double flowSum = 0;
                for (final Hex.Direction direction : DIRECTIONS) {
                    SHM.MAX_LEVEL.addMutable(coordinate, cache.offset(direction), mutable);
                    final Hex.Direction adjacentOutgoing = outgoingBase(mutable, factory.build(mutable, prev), seed, cache);
                    if (adjacentOutgoing != null && adjacentOutgoing.opposite() == direction) {
                        final double flowBase = flowBase(mutable, seed, outer);
                        incoming.put(direction, new RiverData.Incoming(1, 1, flowBase));
                        flowSum = flowSum + flowBase;
                    }
                }
                return new RiverData(PlateType.OCEAN, incoming, null, 0, flowSum, 1, level, -1);
            }
            final Hex.@Nullable Direction chosen = outgoingBase(coordinate, factory.build(coordinate, prev), seed, cache);
            return new RiverData(PlateType.CONTINENT, Collections.emptyMap(), chosen, 1, flowBase(coordinate, seed, outer), 1, level, 1);
        });
    }

    private static double randomDoubleFromLong(final long state) {
        return (state >>> 11) * 0x1.0p-53;
    }

    private static double flowBase(final SHM.Coordinate coordinate, final int seed, final Hash.Strategy<SHM.Coordinate> strategy) {
        final int hashCode = strategy.hashCode(coordinate);
        final int start = HashCommon.mix(seed ^ hashCode) ^ HashCommon.murmurHash3(hashCode);
        final long data = HashCommon.murmurHash3(HashCommon.mix((long) start | (((long) start) << 32L)) + 123456);
        return 7 + 7 * (data >>> 11) * 0x1.0p-53;
    }

    public static Layer.Basic<RiverData> grow(final int seed, final int level, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final SHM.MutableCoordinate mutable = SHM.createMutable();
        return new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            if (data.type() == PlateType.OCEAN) {
                if (data.incoming().isEmpty()) {
                    return data;
                } else {
                    final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                    for (final Hex.Direction direction : data.incoming().keySet()) {
                        shm.addMutable(coordinate, cache.offset(direction), mutable);
                        final RiverData neighbourData = prev.get(mutable);
                        incoming.put(direction, new RiverData.Incoming(1, neighbourData.tiles(), neighbourData.flowRate()));
                    }
                    return new RiverData(PlateType.OCEAN, incoming, null, 0, data.flowRate(), data.tiles(), level, -1);
                }
            }
            if (data.outgoing() != null) {
                final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
                for (final Hex.Direction direction : DIRECTIONS) {
                    if (direction == data.outgoing()) {
                        continue;
                    }
                    shm.addMutable(coordinate, cache.offset(direction), mutable);
                    final Neighbourhood<RiverData> neighbourhood = factory.build(mutable, prev);
                    final RiverData neighbourData = neighbourhood.get(neighbourhood.center());
                    if (neighbourData.type() == PlateType.OCEAN || neighbourData.outgoing() != null) {
                        continue;
                    }
                    final Hex.@Nullable Direction chosen = outgoingGrow(mutable, neighbourhood, cache.outer(), seed);
                    if (chosen == direction.opposite()) {
                        incoming.put(direction, new RiverData.Incoming(data.height() + 1, 1, flowBase(mutable, seed, cache.outer())));
                    }
                }
                if (incoming.isEmpty()) {
                    return data;
                } else {
                    return new RiverData(PlateType.CONTINENT, incoming, data.outgoing(), data.height(), data.flowRate(), data.tiles(), level, 1);
                }
            }
            final Neighbourhood<RiverData> neighbourhood = factory.build(coordinate, prev);
            final Hex.@Nullable Direction chosen = outgoingGrow(coordinate, neighbourhood, cache.outer(), seed);
            if (chosen != null) {
                final RiverData neighbourData = neighbourhood.get(neighbourhood.center(), chosen);
                return new RiverData(PlateType.CONTINENT, Collections.emptyMap(), chosen, neighbourData.height() + 1, flowBase(coordinate, seed, cache.outer()), 1, level, 1);
            }
            return data;
        });
    }

    public static Layer.Basic<RiverData> propagate(final int seed, final int level, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        final SHM.MutableCoordinate mutable = SHM.createMutable();
        return new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            double base = 0;
            double tiles = 1;
            if (data.incoming().isEmpty()) {
                return data;
            }
            if (data.type() == PlateType.CONTINENT) {
                base = flowBase(coordinate, seed, cache.outer());
            }
            final Map<Hex.Direction, RiverData.Incoming> incoming = new EnumMap<>(Hex.Direction.class);
            for (final Map.Entry<Hex.Direction, RiverData.Incoming> entry : data.incoming().entrySet()) {
                shm.addMutable(coordinate, cache.offset(entry.getKey()), mutable);
                final RiverData riverData = prev.get(mutable);
                incoming.put(entry.getKey(), new RiverData.Incoming(riverData.height(), riverData.tiles(), riverData.flowRate()));
                base = base + riverData.flowRate();
                tiles = tiles + riverData.tiles();
            }
            if (tiles != data.tiles()) {
                return new RiverData(data.type(), incoming, data.outgoing(), data.height(), base, tiles, level, data.rainfall());
            }
            return data;
        });
    }

    public static Layer.Basic<RiverData> zoom(final int level, final int seed, final Layer<RiverData> prev) {
        final SHM shm = SHM.create();
        final SHM.LevelCache cache = SHM.createCache(level);
        final SHM.LevelCache outerCache = SHM.createCache(level + 1);
        final NeighbourhoodFactory factory = NeighbourhoodFactory.create(level);
        final CachedExpandData cachedExpandData = new CachedExpandData(level);
        final Layer<SubRiverData> dataLayer = new Layer.CachingOuter<>(new Layer.Basic<>(coordinate -> {
            final RiverData data = prev.get(coordinate);
            return zoomInternal(coordinate, data, level, shm, cache, outerCache, seed, factory, cachedExpandData);
        }), 8, level + 1);
        return new Layer.Basic<>(coordinate -> dataLayer.get(coordinate).data[coordinate.get(level)]);
    }

    private static SubRiverData zoomInternal(final SHM.Coordinate coordinate, final RiverData parentData, final int level, final SHM shm, final SHM.LevelCache cache, final SHM.LevelCache outerCache, final int seed, final NeighbourhoodFactory factory, final CachedExpandData cachedExpandData) {
        final SHM.MutableCoordinate scratch0 = cachedExpandData.scratch0;
        if (parentData.type() == PlateType.OCEAN) {
            if (parentData.incoming().isEmpty() || (parentData.flowRate() - cachedExpandData.maxFlowRate) > 0) {
                return new SubRiverData(new RiverData[]{parentData, parentData, parentData, parentData, parentData, parentData, parentData});
            }
            final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
            final RiverData[] dataArr = new RiverData[7];
            final RiverData empty = new RiverData(PlateType.OCEAN, Collections.emptyMap(), null, 0, 0, 0, level, -1);
            dataArr[truncated.get(level)] = empty;
            for (final Hex.Direction direction : DIRECTIONS) {
                shm.addMutable(truncated, cache.offset(direction.rotateC()), scratch0);
                if (parentData.incoming().containsKey(direction)) {
                    final RiverData.Incoming incoming = parentData.incoming().get(direction);
                    final RiverData riverData = new RiverData(PlateType.OCEAN, Map.of(direction, incoming), null, 0, incoming.flowRate(), 1 + incoming.tiles() * 7, level,-1);
                    dataArr[scratch0.get(level)] = riverData;
                } else {
                    dataArr[scratch0.get(level)] = empty;
                }
            }
            return new SubRiverData(dataArr);
        }
        if (parentData.outgoing() == null || (parentData.flowRate() - cachedExpandData.maxFlowRate) > -0.0001) {
            return new SubRiverData(new RiverData[]{parentData, parentData, parentData, parentData, parentData, parentData, parentData});
        }
        cachedExpandData.reset();
        final SHM.Coordinate truncated = SHM.outerTruncate(coordinate, level + 1);
        final int outerHash = SHM.outerHash(truncated, level + 1);
        cachedExpandData.splitSeed = HashCommon.mix(outerHash + 4321);
        cachedExpandData.data = parentData;
        cachedExpandData.nodeGetter.nodes = SPLIT_RIVERS.walk(factory.build(truncated, i -> null), cachedExpandData, seed ^ outerHash).nodes;
        final NeighbourhoodWalker.Result<WalkerNode> walk = FILL_DATA.walk(factory.build(truncated, cachedExpandData.nodeGetter), parentData);
        cachedExpandData.walkerNodeGetter.nodes = walk.raw();
        final NeighbourhoodWalker.Result<RiverData> flowWalk = FLOW_FILL.walk(factory.build(truncated, cachedExpandData.walkerNodeGetter), parentData);
        return new SubRiverData(flowWalk.raw());
    }

    private static final class NodeGetter implements Function<SHM.Coordinate, Node> {
        private final int level;
        private Node[] nodes;

        private NodeGetter(final int level) {
            this.level = level;
        }

        @Override
        public Node apply(final SHM.Coordinate coordinate) {
            return nodes[coordinate.get(level)];
        }
    }

    private static final class WalkerNodeGetter implements Function<SHM.Coordinate, WalkerNode> {
        private final int level;
        private WalkerNode[] nodes;

        private WalkerNodeGetter(final int level) {
            this.level = level;
        }

        @Override
        public WalkerNode apply(final SHM.Coordinate coordinate) {
            return nodes[coordinate.get(level)];
        }
    }

    private static double interpolate(final int depth, final int maxDepth, final double height, final double endHeight) {
        return height + (endHeight - height) * (depth / (maxDepth + 1.0D));
    }

    private static Hex.@Nullable Direction outgoingBase(final SHM.Coordinate coordinate, final Neighbourhood<PlateType> neighbourhood, final int seed, final SHM.LevelCache cache) {
        return OUTGOING_BASE.choose(neighbourhood, seed + cache.outer().hashCode(coordinate));
    }

    private static Hex.@Nullable Direction outgoingGrow(final SHM.Coordinate coordinate, final Neighbourhood<RiverData> neighbourhood, final Hash.Strategy<SHM.Coordinate> strategy, final int seed) {
        return OUTGOING_GROW.choose(neighbourhood, strategy.hashCode(coordinate) + seed);
    }

    public static final class Node {
        private Hex.@Nullable Direction outgoing;
        private final Object2DoubleMap<Hex.Direction> incoming = new Object2DoubleOpenHashMap<>();
        private int depth = -1;
    }

    public static final class WalkerNode {
        private final Map<Hex.Direction, RiverData.Incoming> incomingHeights = new EnumMap<>(Hex.Direction.class);
        private final double minHeightAlongPath;
        private final int minDepthAlongPath;
        private final double height;
        private final Hex.Direction outgoing;
        private double tiles;
        private double requiredFlowRate = Double.NaN;
        private double flowRate = Double.NaN;

        private WalkerNode(final double minHeightAlongPath, final int minDepthAlongPath, final double height, final Hex.Direction outgoing) {
            this.minHeightAlongPath = minHeightAlongPath;
            this.minDepthAlongPath = minDepthAlongPath;
            this.height = height;
            this.outgoing = outgoing;
        }
    }

    private static final class PathTree {
        private final int start;
        private final Hex.Direction[] outgoing;
        private final int[] depth;
        private final long[] children;

        private PathTree(final int start) {
            this.start = start;
            outgoing = new Hex.Direction[7];
            depth = new int[7];
            children = new long[7];
        }
    }

    private static final class PathResult {
        private final Node[] nodes;

        private PathResult(final Node[] nodes) {
            this.nodes = nodes;
        }
    }

    private static final class CachedExpandData {
        private final Node[] nodes = new Node[7];
        private final NodeGetter nodeGetter;
        private final WalkerNodeGetter walkerNodeGetter;
        private final SHM.MutableCoordinate scratch0;
        private final double maxFlowRate;
        private int splitSeed;
        private RiverData data;

        public CachedExpandData(final int level) {
            for (int i = 0; i < 7; i++) {
                nodes[i] = new Node();
            }
            nodeGetter = new NodeGetter(level);
            walkerNodeGetter = new WalkerNodeGetter(level);
            scratch0 = SHM.createMutable();
            maxFlowRate = Math.pow(SQRT3_3 * Math.pow(2.6457513, level), 3);
        }

        public void reset() {
            for (final Node node : nodes) {
                node.outgoing = null;
                node.incoming.clear();
                node.depth = -1;
            }
        }
    }

    private record SubRiverData(RiverData[] data) {
    }

    private RiverLayers() {
    }
}
