package io.github.stuff_stuffs.river_net_gen.impl.geo;

import io.github.stuff_stuffs.river_net_gen.api.geo.feature.GeoFeature;
import io.github.stuff_stuffs.river_net_gen.api.geo.feature.GeoFeatureApplicator;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterators;

import java.util.*;

public class GeoFeatureApplicatorImpl implements GeoFeatureApplicator {
    private static final Comparator<GeoFeature> OLDEST_FIRST = Comparator.comparingDouble(GeoFeature::timeStamp);
    private final BaseGeoFeature base;
    private final int maxDepth;
    private final GeoFeatureContextImpl root;
    private final GeoFeatureContextImpl[] contexts;
    private final GeoFeature[] featureStack;
    private final GeoFeature.Instance[] instances;
    private int features = 0;
    private GeoFeature.Instance baseInstance;
    private RegistryImpl registry;

    public GeoFeatureApplicatorImpl(final BaseGeoFeature base, final int maxDepth) {
        this.base = base;
        this.maxDepth = maxDepth;
        root = new GeoFeatureContextImpl(this, maxDepth);
        contexts = new GeoFeatureContextImpl[maxDepth];
        for (int i = 0; i < maxDepth; i++) {
            contexts[i] = new GeoFeatureContextImpl(this, i);
        }
        featureStack = new GeoFeature[maxDepth];
        instances = new GeoFeature.Instance[maxDepth];
        registry = new RegistryImpl();
        baseInstance = base.setup(registry);
    }

    @Override
    public int apply(final double x, final double y, final double z) {
        return query(-1, x, y, z);
    }

    @Override
    public void setFeatures(final Collection<? extends GeoFeature> features) {
        final int unwrap = ObjectIterators.unwrap(features.iterator(), featureStack);
        this.features = unwrap;
        Arrays.sort(featureStack, 0, unwrap, OLDEST_FIRST);
        if (unwrap != features.size()) {
            throw new RuntimeException();
        }
        registry = new RegistryImpl();
        baseInstance = base.setup(registry);
        for (int i = 0; i < unwrap; i++) {
            instances[i] = featureStack[i].setup(registry);
        }
    }

    @Override
    public Set<String> geoIdentifiers() {
        return Collections.unmodifiableSet(registry.geoIds.keySet());
    }

    @Override
    public OptionalInt getGeoId(final String identifier) {
        final int i = registry.geoIds.getInt(identifier);
        if (i == -1) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(i);
    }

    private int query(final int index, final double x, final double y, final double z) {
        if (index < maxDepth) {
            final GeoFeatureContextImpl context;
            final GeoFeature.Instance feature;
            if (index + 1 < features) {
                context = contexts[index + 1];
                feature = instances[index + 1];
            } else {
                context = root;
                feature = baseInstance;
            }
            context.x(x);
            context.y(y);
            context.z(z);
            if (!(context.computed | context.fallThrough)) {
                feature.apply(context);
            }
            return context.result();
        } else {
            throw new IllegalStateException();
        }
    }

    private static final class GeoFeatureContextImpl implements GeoFeature.GeoFeatureContext {
        private final GeoFeatureApplicatorImpl parent;
        private final int index;
        private double x;
        private double y;
        private double z;
        private boolean fallThrough = false;
        private int result = 0;
        private boolean computed = false;

        private GeoFeatureContextImpl(final GeoFeatureApplicatorImpl parent, final int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public double x() {
            return x;
        }

        @Override
        public double y() {
            return y;
        }

        @Override
        public double z() {
            return z;
        }

        @Override
        public void x(final double x) {
            if (x != this.x) {
                computed = false;
                fallThrough = false;
                this.x = x;
            }
        }

        @Override
        public void y(final double y) {
            if (y != this.y) {
                computed = false;
                fallThrough = false;
                this.y = y;
            }
        }

        @Override
        public void z(final double z) {
            if (z != this.z) {
                computed = false;
                fallThrough = false;
                this.z = z;
            }
        }

        @Override
        public int query(final double x, final double y, final double z) {
            return parent.query(index, x, y, z);
        }

        @Override
        public void setQuery() {
            fallThrough = true;
        }

        @Override
        public void set(final int geoId) {
            computed = true;
            fallThrough = false;
            result = geoId;
        }

        private int result() {
            if (computed) {
                return result;
            } else if (fallThrough) {
                return query(x, y, z);
            }
            throw new IllegalStateException();
        }
    }

    private static final class RegistryImpl implements GeoFeature.Registry {
        private final Object2IntMap<String> geoIds;
        private int nextId = 0;

        public RegistryImpl() {
            geoIds = new Object2IntOpenHashMap<>();
            geoIds.defaultReturnValue(-1);
        }

        @Override
        public int getGeoId(final String featureName) {
            if (geoIds.containsKey(featureName)) {
                return geoIds.getInt(featureName);
            }
            final int id = nextId++;
            geoIds.put(featureName, id);
            return id;
        }
    }
}
