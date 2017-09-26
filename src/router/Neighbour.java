/**
 * Redes Integradas de Telecomunicacoes I
 * MIEEC 2017/2018
 *
 * Neighbour.java
 *
 * Holds neighbor router internal data
 *
 * Created on 7 de Setembro de 2017, 16:00
 * @author  Luis Bernardo
 */

package router;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Holds neighbor Router internal data
 */
public class Neighbour {
    /** neigbour's name (address) [A,Z] */
    public char name;
    /** IP address of the Neighbour */
    public String ip;
    /** port number of the Neighbour */
    public int port;
    /** distance to the Neighbour */
    public int dist;
    /** address of the Neighbour, includes IP+port */
    public InetAddress netip;

    // Multi-region specific field 
    /** Array of areas of the Neighbour */
    public String area;
    
    // Vector-distance protocols specific data
    public Entry[] vec;     // Neighbor vector
    public Date vec_date;   // Neighbor vector reception date
    public long vec_TTL;    // TTL in miliseconds
    
    /**
     * Return the name of the Neighbour
     * @return the character with the name
     */
    public char Name() { return name; }
    /**
     * Return the IP address of the Neighbour
     * @return IP address
     */
    public String Ip() { return ip; }
    /**
     * Return the port number of the Neighbour
     * @return port number
     */
    public int Port()  { return port; }
    /**
     * Return the distance to the Neighbour
     * @return distance
     */
    public int Dist()  { return dist; }
    /**
     * Return the InetAddress object to send messages to the Neighbour
     * @return InetAddress object
     */    
    public InetAddress Netip() { return netip; }
    
    /** Vector-distance protocol specific function:
     *          Returns a vector, if it exists
     * @return  Entry vector */
    public Entry[] Vec() { return vec_valid()? vec : null; }
    
    /** Returns the area string, if it exists
     * @return  Area string */
    public String Area() { return area; }

    
    /**
     * Parse a string with a compact name, defining the local name
     * @param name  the string
     * @return  true if name is valid, false otherwise
     */
    private boolean parseName(String name) {
        // Clear name
        if (name.length() != 1) {
            return false;
        }
        char c= name.charAt(0);
        if (!Character.isUpperCase (c)) {
            return false;
        }
        this.name= c;
        return true;
    }
    
    /**
     * Constructor - create an empty instance of neighbour
     */
    public Neighbour() {
        clear();
    }
    
    /**
     * Constructor - create a new instance of neighbour from parameters
     * @param name      neighbour's name
     * @param ip        ip address
     * @param port      port number
     * @param distance  distance
     * @param area      area
     */
    public Neighbour(char name, String ip, int port, int distance, String area) {
        clear();
        this.ip= ip;
        if (test_IP()) {
            this.name= name;
            this.port= port;
            this.dist= distance;
        } else {
            this.ip= null;
        }
        this.area= (area != null ? new String(area) : null);
    }
    
    /**
     * Constructor - create a clone of an existing object
     * @param src  object to be cloned
     */
    public Neighbour(Neighbour src) {
        this.name= src.name;
        this.ip= src.ip;
        this.netip= src.netip;
        this.port= src.port;
        this.dist= src.dist;
        this.area= (src.area==null ? null : new String(src.area));
    }
        
    /**
     * Update the fields of the Neighbour object
     * @param name      Neighbour's name
     * @param ip        ip address
     * @param port      port number
     * @param distance  distance
     * @param area      area string; if null it does not change the area field 
     */
    public void update_neigh(char name, String ip, int port, int distance, String area) {
        this.ip= ip;
        if (test_IP()) {
            this.name= name;
            this.port= port;
            this.dist= distance;
            if (area != null) {
                this.area= new String (area);
            }
        } else {
            clear();
        }
    }
    
    /**
     * Vector-distance specific function:
     *  updates last vector received from neighbor TTL in miliseconds
     * @param vec  vector
     * @param TTL  Time to Live
     * @throws Exception 
     */
    public void update_vec(Entry[] vec, long TTL) throws Exception {
        if (!is_valid()) {
            throw new Exception ("Update vector of invalid neighbor");
        }
        this.vec= vec;
        this.vec_date= new Date();  // Now
        this.vec_TTL= TTL;
    }
    
    /**
     * Clear the contents of the neigbour object
     */
    public final void clear() {
        this.name= ' ';
        this.ip= null;
        this.netip= null;
        this.port= 0;
        this.dist= Router.MAX_DISTANCE;
        this.vec= null;
        this.vec_date= null;
        this.vec_TTL= 0;
        this.area= null;
    }

    /**
     * Test the IP address
     * @return true if is valid, false otherwise
     */
    private boolean test_IP() {
        try {
            netip= InetAddress.getByName(ip);
            return true;
        }
        catch (UnknownHostException e) {
            netip= null;
            return false;
        }
    }

    /**
     * Test if the Neighbour is valid
     * @return true if is valid, false otherwise
     */
    public boolean is_valid() { return (netip!=null); }
    
    
    /**
     * Vector-distance protocol specific: test if the vector is valid
     * @return true if is valid, false otherwise
     */
    public boolean vec_valid() { return (vec!=null) && ((new Date().getTime() - vec_date.getTime())<=vec_TTL); }

    
    /**
     * Multi-area specific: test if the area is valid
     * @return true if is valid, false otherwise
     */
    public boolean area_valid() { return (area!=null) && (area.length()>0); }

    /**
     * Test if neighbor belongs to the area
     * @param a  parameter with the area
     * @return true if it belongs, false otherwise
     */
    public boolean in_area(char a) {
        return (area!=null) && (area.indexOf(a) != -1);
    }

    /**
     * Send a packet to the Neighbour
     * @param ds  datagram socket
     * @param dp  datagram packet with the packet contents
     * @throws IOException 
     */
    public void send_packet(DatagramSocket ds, 
                                DatagramPacket dp) throws IOException {
        try {
            dp.setAddress(this.netip);
            dp.setPort(this.port);
            ds.send(dp);
        }
        catch (IOException e) {
            throw e;
        }        
    }
    
    /**
     * Send a packet to the Neighbour
     * @param ds  datagram socket
     * @param os  output stream with the packet contents
     * @throws IOException 
     */
    public void send_packet(DatagramSocket ds, 
                                ByteArrayOutputStream os) throws IOException {
        try {
            byte [] buffer = os.toByteArray();
            DatagramPacket dp= new DatagramPacket(buffer, buffer.length, 
                this.netip, this.port);
            ds.send(dp);
        }
        catch (IOException e) {
            throw e;
        }        
    }
    
    /**
     * Create a send a HELLO packet to the Neighbour
     * @param ds    datagram socket
     * @param win   main window object 
     * @return true if sent successfully, false otherwise
     */
    public boolean send_Hello(DatagramSocket ds, Router win) {
        ByteArrayOutputStream os= new ByteArrayOutputStream();
        DataOutputStream dos= new DataOutputStream(os);
        String larea= win.local_areas();
        if (larea == null) {
            System.out.println("Area not defined - Hello not sent");
            return false;
        }
        try {
            dos.writeByte(Router.PKT_HELLO);
            // name ('letter')
            dos.writeChar(win.local_name());
            // Distance
            dos.writeInt(dist);
            // Areas
            dos.writeInt(larea.length());
            for (int i=0; i<larea.length(); i++) {
                dos.writeChar(larea.charAt(i));
            }
            //
            send_packet(ds, os);
            win.HELLO_snt++;
            return true;
        }
        catch (IOException e) {
            System.out.println("Internal error sending packet HELLO: "+e+"\n");
            return false;
        }        
    }
    
    /**
     * Create a send a BYE packet to the Neighbour
     * @param ds    datagram socket
     * @param win   main window object 
     * @return true if sent successfully, false otherwise
     */
    public boolean send_Bye(DatagramSocket ds, Router win) {
        ByteArrayOutputStream os= new ByteArrayOutputStream();
        DataOutputStream dos= new DataOutputStream(os);
        try {
            dos.writeByte(Router.PKT_BYE);
            // name ('letter')
            dos.writeChar(win.local_name());
            send_packet(ds, os);
            win.BYE_snt++;
            return true;
        }
        catch (IOException e) {
            System.out.println("Internal error sending packet BYE: "+e+"\n");
            return false;
        }        
    }
    
    /**
     * return a string with the Neighbour contents; replaces default function
     * @return string with the Neighbour contents
     */
    @Override
    public String toString() {
        String str= ""+name;
        if (name == ' ') {
            str= "INVALID";
        }
        return "("+str+" ; "+ip+" ; "+port+" ; "+dist+"A/"+(area==null?"null":area)+"/)";
    }
    
    /**
     * parses a string for the Neighbour field values
     * @param str  string with the values
     * @return true if parsing successful, false otherwise
     */
    public boolean parseString(String str) {
        StringTokenizer st = new StringTokenizer(str, " ();");
        if (st.countTokens( ) != 5) {
            return false;
        }
        try {
            // Parse name
            if (!parseName(st.nextToken())) {
                return false;
            }
            String r_ip= st.nextToken();
            int r_port= Integer.parseInt(st.nextToken());
            int r_dist= Integer.parseInt(st.nextToken());
            String r_area= st.nextToken();
            update_neigh(name, r_ip, r_port, r_dist, r_area);
            return is_valid();
        }
        catch (NumberFormatException e) {
            return false;
        }
    }
}
