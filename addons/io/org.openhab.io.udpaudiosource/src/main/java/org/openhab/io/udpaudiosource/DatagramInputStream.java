package org.openhab.io.udpaudiosource;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DatagramInputStream extends InputStream {
    private static final int DEFAULT_BUFFER_SIZE = 1024;

    private DatagramSocket socket;
    private DatagramPacket packet;

    byte[] buffer;
    int packetSize = 0;
    int packetIndex = 0;

    public DatagramInputStream(DatagramSocket socket, int bufferSize) {
        this.socket = socket;
        this.buffer = new byte[bufferSize];
    }

    public DatagramInputStream(DatagramSocket socket) {
        this(socket, DEFAULT_BUFFER_SIZE);
    }

    @Override
    public void close() throws IOException {
        socket.close();
        socket = null;
        buffer = null;
        packetSize = 0;
        packetIndex = 0;
    }

    @Override
    public int available() throws IOException {
        return packetSize - packetIndex;
    }

    @Override
    public int read() throws IOException {
        if (packetIndex == packetSize) {
            receive();
        }

        int value = buffer[packetIndex] & 0xff;
        packetIndex++;
        return value;
    }

    @Override
    public int read(byte[] buff) throws IOException {
        return read(buff, 0, buff.length);
    }

    @Override
    public int read(byte[] buff, int off, int len) throws IOException {
        if (packetIndex == packetSize) {
            receive();
        }

        int lenRemaining = len;

        while (available() < lenRemaining) {
            System.arraycopy(buffer, packetIndex, buff, off + (len - lenRemaining), available());
            lenRemaining -= available();
            receive();
        }

        System.arraycopy(buffer, packetIndex, buff, off + (len - lenRemaining), lenRemaining);
        packetIndex += lenRemaining;
        return len;
    }

    @Override
    public long skip(long len) throws IOException {
        if (packetIndex == packetSize) {
            receive();
        }

        long lenRemaining = len;

        while (available() < lenRemaining) {
            lenRemaining -= available();
            receive();
        }

        packetIndex += (int) lenRemaining;
        return len;
    }

    private void receive() throws IOException {
        packet = new DatagramPacket(buffer, DEFAULT_BUFFER_SIZE);
        socket.receive(packet);
        packetIndex = 0;
        packetSize = packet.getLength();
    }

    @Override
    public void mark(int readlimit) {
        throw new RuntimeException("Marking not supported!");
    }

    @Override
    public void reset() throws IOException {
        throw new IOException("Marking not supported!");
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
