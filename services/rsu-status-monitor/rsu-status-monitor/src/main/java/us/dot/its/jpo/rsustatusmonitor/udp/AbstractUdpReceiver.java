package us.dot.its.jpo.rsustatusmonitor.udp;

import java.net.DatagramSocket;
import java.net.SocketException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractUdpReceiver implements Runnable {

    protected DatagramSocket socket;
    protected int port;
    protected int bufferSize;
    private boolean stopped = false;

    /**
     * Checks if the receiver is stopped.
     *
     * @return true if the receiver is stopped, false otherwise
     */
    public boolean isStopped() {
        return stopped;
    }

    /**
     * Sets the stopped state of the receiver. Intended for testing.
     *
     * @param stopped true to stop the receiver, false to continue running
     */
    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    protected AbstractUdpReceiver(int port, int bufferSize) {
        this.port = port;
        this.bufferSize = bufferSize;

        try {
            this.socket = new DatagramSocket(this.port);
            log.debug("Created UDP socket bound to port {}", this.port);
        } catch (SocketException e) {
            log.error("Error creating socket with port {}", this.port, e);
        }
    }

}