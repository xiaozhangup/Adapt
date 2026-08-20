package com.volmit.adapt.util.spatial.mantle;

final class MantleHeight {
    private MantleHeight() {
    }

    static void validate(int minHeight, int maxHeight) {
        if (minHeight >= maxHeight || (minHeight & 15) != 0 || (maxHeight & 15) != 0) {
            throw new IllegalArgumentException("Mantle height bounds must be section-aligned and non-empty");
        }
    }

    static int toInternalY(int y, int minHeight, int maxHeight) {
        return y < minHeight || y >= maxHeight ? -1 : y - minHeight;
    }

    static int legacySectionOffset(int minHeight) {
        return -minHeight >> 4;
    }
}
