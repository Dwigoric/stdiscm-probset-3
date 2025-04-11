package ph.dlsu.edu.ccs.stdiscm.jgang;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class VideoFile {
    private final String header;
    private final SocketChannel clientChannel;
    private final ByteBuffer leftoverBuffer;

    public VideoFile(String header, SocketChannel clientChannel, ByteBuffer leftoverBuffer) {
        this.header = header;
        this.clientChannel = clientChannel;
        this.leftoverBuffer = leftoverBuffer;
    }

    public String getHeader() {
        return header;
    }

    public SocketChannel getClientChannel() {
        return clientChannel;
    }

    public ByteBuffer getLeftoverBuffer() {
        return leftoverBuffer;
    }

    public void close() throws IOException {
        clientChannel.close();
    }

    @Override
    public String toString() {
        return "VideoFile{" +
                "header='" + getHeader() + '\'' +
                ", clientChannel=" + clientChannel +
                ", leftoverBuffer=" + (leftoverBuffer != null ? leftoverBuffer.remaining() + " bytes" : "null") +
                '}';
    }
}
