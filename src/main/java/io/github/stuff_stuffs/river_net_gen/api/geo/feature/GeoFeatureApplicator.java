package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

import io.github.stuff_stuffs.river_net_gen.impl.geo.GeoFeatureApplicatorImpl;

import java.util.Collection;
import java.util.OptionalInt;
import java.util.Set;

public interface GeoFeatureApplicator {
    int apply(double x, double y, double z);

    void setFeatures(Collection<? extends GeoFeature> features);

    Set<String> geoIdentifiers();

    OptionalInt getGeoId(String identifier);

    interface BaseGeoFeature {
        GeoFeature.Instance setup(GeoFeature.Registry registry);
    }

    static GeoFeatureApplicator create(final BaseGeoFeature base, final int maxDepth) {
        return new GeoFeatureApplicatorImpl(base, maxDepth);
    }
}