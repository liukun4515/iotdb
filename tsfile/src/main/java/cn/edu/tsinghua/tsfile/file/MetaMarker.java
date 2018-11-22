package cn.edu.tsinghua.tsfile.file;

import java.io.IOException;

/**
 * MetaMarker denotes the type of headers and footers. Enum is not used for space saving.
 */
public class MetaMarker {
    public static final byte ChunkGroupFooter = 0;
    public static final byte ChunkHeader = ChunkGroupFooter + 1;

    public static void handleUnexpectedMarker(byte marker) throws IOException {
        throw new IOException("Unexpected marker " + marker);
    }
}
