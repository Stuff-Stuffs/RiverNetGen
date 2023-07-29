package io.github.stuff_stuffs.river_net_gen.api.geo.feature;

public class FaultGeoFeature implements GeoFeature {
    private final double age;
    private final double x, y, z;
    private final double nx, ny, nz;
    private final double tx, ty, tz;
    private final double radius;
    private final double faultFactor;
    private final Instance instance;

    public FaultGeoFeature(final double age, final double x, final double y, final double z, final double nx, final double ny, final double nz, final double tx, final double ty, final double tz, final double radius, final double faultFactor) {
        this.age = age;
        this.x = x;
        this.y = y;
        this.z = z;

        final double scale = 1 / Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (!Double.isFinite(scale)) {
            throw new IllegalArgumentException();
        }
        this.nx = nx * scale;
        this.ny = ny * scale;
        this.nz = nz * scale;

        final double tScale = 1 / Math.sqrt(tx * tx + ty * ty + tz * tz);
        if (!Double.isFinite(tScale)) {
            throw new IllegalArgumentException();
        }
        this.tx = tx * tScale;
        this.ty = ty * tScale;
        this.tz = tz * tScale;

        this.radius = radius;
        this.faultFactor = faultFactor;

        final double invRadiusSq = 1 / (radius * radius);
        instance = context -> {
            final double dx = context.x() - this.x;
            final double dy = context.y() - this.y;
            final double dz = context.z() - this.z;
            final double distanceSq = dx * dx + dy * dy + dz * dz;
            final double factor = 1 - distanceSq * invRadiusSq;
            if (factor > 0) {
                final double side = Math.copySign(1.0, dx * this.nx + dy * this.ny + dz * this.nz);
                final double rx = this.x + dx + side * factor * this.faultFactor * 0.5 * this.tx;
                final double ry = this.y + dy + side * factor * this.faultFactor * 0.5 * this.ty;
                final double rz = this.z + dz + side * factor * this.faultFactor * 0.5 * this.tz;
                context.x(rx);
                context.y(ry);
                context.z(rz);
            }
            context.setQuery();
        };
    }

    @Override
    public double timeStamp() {
        return age;
    }

    @Override
    public Instance setup(final Registry registry) {
        return instance;
    }
}
