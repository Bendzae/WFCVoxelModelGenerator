package org.example.voxparser;

import java.io.IOException;
import java.io.InputStream;
import org.example.shared.Vector3;

final class VoxSizeChunk extends VoxChunk {
    private final Vector3<Integer> size;

    VoxSizeChunk(InputStream stream) throws IOException {
        this.size = StreamUtils.readVector3i(stream);
    }

    public Vector3<Integer> getSize() {
        return size;
    }
}
