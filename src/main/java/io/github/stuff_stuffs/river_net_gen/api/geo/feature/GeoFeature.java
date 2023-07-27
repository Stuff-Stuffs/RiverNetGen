package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public interface GeoFeature {
    double age();

    Instance setup(Registry registry);

    interface Instance {
        void apply(GeoFeatureContext context);
    }

    interface GeoFeatureContext {
        double x();

        double y();

        double z();

        void x(double x);

        void y(double y);

        void z(double z);

        int query();

        void setQuery();

        void set(int geoId);
    }

    interface Registry {
        int getGeoId(String featureName);
    }
}
