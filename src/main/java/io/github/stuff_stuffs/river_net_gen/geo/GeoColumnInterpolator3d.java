package io.github.stuff_stuffs.river_net_gen.geo;

import it.unimi.dsi.fastutil.HashCommon;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class GeoColumnInterpolator3d {
    private final Result fResult;
    private final Result sResult;
    private final Result tResult;

    public GeoColumnInterpolator3d(final double x0, final double z0, final GeoColumn first, final double x1, final double z1, final GeoColumn second, final double x2, final double z2, final GeoColumn third) {
        fResult = build(x0, z0, first);
        sResult = build(x1, z1, second);
        tResult = build(x2, z2, third);
    }

    public int interpolate(final double x, final double y, final double z) {
        final Section[] fSections = fResult.sections;
        double bestScore = Double.NEGATIVE_INFINITY;
        int best = -1;
        for (int i = 0; i < fSections.length; i++) {
            final Section section = fSections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y * fResult.yScale - section.y);
            final double dz = z - section.z;
            final double score = 1 / (Math.sqrt(dx * dx + dy * dy + dz * dz) + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = fResult.data[i];
            }
        }
        final Section[] sSections = sResult.sections;
        for (int i = 0; i < sSections.length; i++) {
            final Section section = sSections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y * sResult.yScale - section.y);
            final double dz = z - section.z;
            final double score = 1 / (Math.sqrt(dx * dx + dy * dy + dz * dz) + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = sResult.data[i];
            }
        }

        final Section[] tSections = tResult.sections;
        for (int i = 0; i < tSections.length; i++) {
            final Section section = tSections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y * tResult.yScale - section.y);
            final double dz = z - section.z;
            final double score = 1 / (Math.sqrt(dx * dx + dy * dy + dz * dz) + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = tResult.data[i];
            }
        }
        return best;
    }

    private static Result build(final double x, final double z, final GeoColumn column) {
        long seed = 0;
        final int length = column.length();
        for (int i = 0; i < length; i++) {
            seed = HashCommon.murmurHash3(seed + 1) ^ column.data(i);
        }
        final RandomGeneratorFactory<RandomGenerator> factory = RandomGeneratorFactory.of("Xoroshiro128PlusPlus");
        final RandomGenerator generator = factory.create(seed);
        final Section[] sections = new Section[length];
        final int[] data = new int[length];
        final double yScale = 1 / (double) (column.height(length - 1) + column.thickness(length - 1));
        for (int i = 0; i < length; i++) {
            final double nx = (generator.nextDouble() - 0.5) * 0.1;
            final double ny = 1 - Math.abs(generator.nextGaussian());
            final double nz = (generator.nextDouble() - 0.5) * 0.1;
            final double lenInv = 1 / Math.sqrt(nx * nx + ny * ny + nz * nz);
            sections[i] = new Section(x, (column.height(i)) * yScale, z, nx * lenInv, ny * lenInv, nz * lenInv);
            data[i] = column.data(i);
        }
        return new Result(yScale, sections, data);
    }

    private record Result(double yScale, Section[] sections, int[] data) {
    }

    private record Section(double x, double y, double z, double nx, double ny, double nz) {
    }
}
