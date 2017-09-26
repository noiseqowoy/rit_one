/**
 * Redes Integradas de Telecomunicacoes I
 * MIEEC 2017/2018
 *
 * MulticastDaemon.java
 *
 * Thread that receives multicast packets
 *
 * Created on 7 de Setembro de 2017, 16:00
 * @author  Luis Bernardo
 */
package router;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Thread that handles socket events
 */
public class MulticastDaemon extends Thread {

    volatile boolean keepRunning = true;
    private DatagramSocket ds;
    private MulticastSocket ms;
    private String multicast_addr;
    private InetAddress group;
    private int mport;
    private Router win;
    private Routing route;

    /**
     * Constructor - receives external parameters and creates multicast socket
     * @param ds                unicast datagram socket - used to send packets to the group
     * @param multicast_addr    IP multicast address
     * @param mport             multicast port number
     * @param win               main window reference
     * @param route             Routing object reference
     */
    MulticastDaemon(DatagramSocket ds, String multicast_addr, int mport,
            Router win, Routing route) {
        this.ds = ds;
        this.multicast_addr = multicast_addr;
        this.mport = mport;
        this.win = win;
        this.route = route;
        try {
            // Starts the multicast socket
            ms = new MulticastSocket(mport);
            group = InetAddress.getByName(multicast_addr);
            ms.joinGroup(group);
        } catch (Exception e) {
            win.Log("Multicast daemon failure: " + e + "\n");
            if (ms != null) {
                ms.close();
                ms = null;
            }
            group = null;
        }
    }

    /**
     * Test if object is valid
     * @return true if valid, false otherwise
     */
    public boolean valid() {
        return ms != null;
    }

    /**
     * Send a packet to the multicast group
     * @param dp  packet to send
     * @throws IOException 
     */
    public void send_packet(DatagramPacket dp) throws IOException {
        if (!valid()) {
            win.Log("Invalid call to send_packet multicast\n");
            return;
        }
        try {
            dp.setAddress(group);
            dp.setPort(mport);
            ds.send(dp);
        } catch (IOException e) {
            throw e;
        }
        route.Log2("mpacket sent to " + mport + "\n");
    }


    /**
     * Thread main function
     */
    @Override
    public void run() {
        byte[] buf = new byte[8096];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        try {
            while (keepRunning) {
                try {
                    ms.receive(dp);
                    ByteArrayInputStream BAis =
                            new ByteArrayInputStream(buf, 0, dp.getLength());
                    DataInputStream dis = new DataInputStream(BAis);
                    System.out.println("Received mpacket (" + dp.getLength()
                            + ") from " + dp.getAddress().getHostAddress()
                            + ":" + dp.getPort());
                    byte code;
                    char sender;
                    try {
                        code = dis.readByte();     // read code
                        sender = dis.readChar();   // read sender id
                        String ip = dp.getAddress().getHostAddress();  // Get sender address            
                        switch (code) {
                            case Router.PKT_ROUTE:
                                route.process_multicast_ROUTE(sender,
                                        dp, ip, dis);
                                break;
                            default:
                                win.Log("Invalid mpacket type: " + code + "\n");
                        }
                    } catch (IOException e) {
                        win.Log("Multicast Packet too short\n");
                    }
                } catch (SocketException se) {
                    if (keepRunning) {
                        win.Log("recv UDP SocketException : " + se + "\n");
                    }
                }
            }
        } catch (IOException e) {
            if (keepRunning) {
                win.Log("IO exception receiving data from socket : " + e);
            }
        }
    }

    /**
     * Stop the thread
     */
    public void stopRunning() {
        keepRunning = false;
        try {
            InetAddress lgroup = InetAddress.getByName(multicast_addr);
            ms.leaveGroup(lgroup);
        } catch (UnknownHostException e) {
            win.Log("Invalid address in stop running '" + multicast_addr + "': " + e + "\n");
        } catch (IOException e) {
            win.Log("Failed leave group: " + e + "\n");
        }
        if (this.isAlive()) {
            this.interrupt();
        }
    }
}
