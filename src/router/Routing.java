/**
 * Redes Integradas de Telecomunicacoes I
 * MIEEC 2017/2018
 *
 * Routing.java
 *
 * Encapsulates the routing functions, hosting multiple instances of 
 * Routing_process objects, and handles DATA packets
 *
 * Created on 7 de Setembro de 2017, 16:00
 * @author  Luis Bernardo
 */

package router;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;


/**
 * Encapsulates the Routing functions, hosting multiple instances of 
 RoutingProcess objects, and handles DATA packets
 */
public class Routing implements Log {
    /** Maximum length of the Entry vector length */
    public final int MAX_ENTRY_VEC_LEN= 30;
    /** Time added to the period to define the TTL field of the ROUTE packets */ 
    public final int TTL_ADD= 6;
    
        
    /**
     * Create a new instance of a routing object, that encapsulates routing processes
     * @param local_name    local address
     * @param neig          Neighbour list
     * @param period        ROUTE timer period
     * @param min_interval  minimum interval between ROUTE packets sent
     * @param areas         list of areas of the node
     * @param multi_addr    multicast IP address
     * @param multi_port    multicast port number
     * @param win           reference to main window object
     * @param ds            unicast datagram socket
     * @param tableObj      reference to routing table graphical object
     */
    public Routing(char local_name, NeighbourList neig, int period, 
            int min_interval, String areas, String multi_addr, int multi_port,
            Router win,  DatagramSocket ds, JTable tableObj) {
        this.local_name= local_name;
        this.areas= "0"; // this.areas= areas;
        if ((areas == null) || (areas.length()<1)) {
            Log2("Invalid areas in routing constructor");
            this.rprocesses= null;
        } else {
            this.rprocesses= new HashMap<>();
            
            // Incomplete - This version ignores the areas introduced in the GUI   
            // Plase modify to support multiple areas, if you have time !
            
            Log("routing class only supports one area for now!\n\tUsing only area 0\n");
            this.rprocesses.put('0', 
                    new RoutingProcess(this, win, neig, '0', period, min_interval));            
        }
        
        this.neig= neig;
        this.local_TTL= period+TTL_ADD;
        this.win= win;
        this.ds= ds;
        this.tableObj= tableObj;
        // Initialize everything
        this.mdaemon= new MulticastDaemon(ds, multi_addr, multi_port, win, this);
        this.main_rtab= null;
        Log2("new Routing(local='"+local_name+"', period="+period+
            ", min_interval="+min_interval+")");
    }

    
    /**
     * Start the Routing processes and timers
     * @return true is running, false if starting failed
     */
    public boolean start() {
        // Start mdaemon thread
        if (!mdaemon.valid()) {
            return false;
        }
        if (!mdaemon.isAlive()) {
            mdaemon.start();
        }
        update_global_routing_table();
        start_announce_timer();
        return true;
    }


    /**
     * Return the local name
     * @return local address string
     */
    public char local_name() {
        return local_name;
    } 
    
    /** 
     * Return true if it belongs to area 'area' 
     * @param area  area number
     * @return true if inside area
     */
    public boolean in_area(char area) {
        return (areas != null) && (areas.indexOf(area) != -1);
    }

    /** 
     * Test if is running area '0' 
     * @return true if it belongs to 'area 0'
     */
    public boolean area0_running() {
        return (rprocesses != null) && (rprocesses.containsKey('0'));
    }

    /**
     * Return the RoutingProcess object associated to an area
     * @param area - the area selected
     * @return object reference, or null, if it does not exist
     */
    public RoutingProcess get_Routing_process(char area) {
        if (rprocesses == null) {
            return null;
        }
        return rprocesses.get(area);
    }

    /**
     * Handle the notification from the Routing process about route calculation
     * @param c_area area number
     * @param changed true if due to network changing
     */
    public void area_tab_calculated(char c_area, boolean changed) {
        
        Log("Routing.area_tab_calculated() only supports one area for now!\n");
        
        // Always updates the global routing table with the table from c_area
        // With multiple areas, the table from area 0 is used
        
        RoutingProcess rp= get_Routing_process(c_area);
        if (rp == null) {
            Log("Internal error: area_changed("+c_area+") - null area process\n");
            return;
        }
        // Update global Routing table
        main_rtab = rp.get_routing_table();

        // main_rtab.Log_routing_table(this);

        // Display Routing table 
        update_routing_window();
    }

    /**
     * Handle a network change notification from the Neighbour management
     * @param _areas  array of areas modified
     */
    public void network_changed(String _areas) {
        if ((_areas == null) || (areas == null)){
            return;
        }
        Log("Routing.network_changed("+_areas+") not implemented yet\n");
        
        // If you have time, you must complete this function to address
        // handling SendIfChanges modifications! 

        // It must call the method network_changed for all RoutingProcess objects
        // Starting from non-'0' areas, and running the '0' area at the end
    }

    
    /** 
     * Stop all the Routing processes and resets the Routing state
     */
    public void stop() {
        // Stop multicast daemon
        mdaemon.stopRunning();
        mdaemon= null;
        
        stop_announce_timer();
        // Clean Routing information
        if (rprocesses != null)
            rprocesses.clear();
        // Clean Routing table
        if (main_rtab != null)
            main_rtab.clear();
        // Clear Routing table window
        update_routing_window();

        local_name= ' ';
        neig= null;
        win= null;
        ds= null;
        tableObj= null;
    }
        
    /**
     * Prepare a ROUTE packet with the Neighbour information 
     * @param name  local name (address)
     * @param area  local area
     * @param seq   sequence number
     * @param vec   Neighbour Entry vector
     * @return the ROUTE packet, or null if error
     */
    public DatagramPacket make_ROUTE_packet(char name, char area, int seq, 
            Entry[] vec) {        
        if (vec == null) {
            Log("ERROR: null vec in send_ROUTE_packet\n");
            return null;
        }
        Log2("make_ROUTE_packet("+name+seq+","+local_TTL+","+"[");
        for (int i=0;i<vec.length;i++) {
            Log2(""+(i>0?",":"")+vec[i].toString());
        }
        Log2("])\n");
        
        ByteArrayOutputStream os= new ByteArrayOutputStream();
        DataOutputStream dos= new DataOutputStream(os);
        try {
            dos.writeByte(Router.PKT_ROUTE);
            dos.writeChar(name);
            dos.writeChar(area);
            dos.writeInt(seq);
            dos.writeInt(local_TTL);
            dos.writeInt(vec.length);
            for (Entry vec1 : vec) {
                vec1.writeEntry(dos);
            }
            byte [] buffer = os.toByteArray();
            DatagramPacket dp= new DatagramPacket(buffer, buffer.length);
            
            return dp;
        }
        catch (IOException e) {
            Log("Error making ROUTE: "+e+"\n");                    
            return null;
        }
    }


    /**
     * Return the initial Routing table for area 'area' with only the direct neighbours
     * @param area the area requested
     * @return a Routing table with only the direct neighbors (except for area '0')
     */
    public RoutingTable local_route_table(char area) {
        // Get local vec
        Entry[] lvec = neig.local_vec(true, area);
        if (lvec == null) { // No local information ??
            win.Log("Internal error in Routing.local_vec\n");
            return null;
        }
        RoutingTable auxtab= new RoutingTable();
       
        // Prepare an initial routing table with the elements of the vector
        Log("Routing.local_route_table(area) not implemented yet\n");
        
        // Place here the code to create and fill the routing table auxtab.
        // Look at the constructors of class RouteEntry and pick the best one to instantiate new objects from Entry objects
        // ...
        
        // This function is incomplete for a multi-area routing LS protocol
        // OSPF has a core area ('0') that may connect other peripheric areas
        //     ('1'-'9') using a star topology. Therefore, the area '0' border
        //     routers disseminate the routes received from other areas on 
        //     area '0' and build a complete routing table.
        //     Then, they send the complete table to the other areas.
        // ...
        
        return null;
    }
 

    /**
     * Return the local Entry vector for area 'area', used to prepare the ROUTE packet
     * @param area  area number
     * @return the Entry vector, or null if error
     */
    public Entry[] local_vec(char area) {
        // Get local vec
        Entry[] lvec = neig.local_vec(true, area);
        if (lvec == null) { // No local information ??
            win.Log("Internal error in routing.local_vec\n");
            return null;
        }

        // This code is incomplete for the case where multiple areas coexist
        // When sending to non-zero areas, the core('0') routes must be sent
        // excluding routes that may produce loops!
        // ...

        return lvec;       
    }

    
    /** Unmarshalls unicast ROUTE packet e process it */
    /**
     * Unmarshall a ROUTE packet and process it
     * @param sender    the sender address
     * @param dp        datagram packet
     * @param ip        IP address of the sender
     * @param dis       input stream object
     * @return true if packet was handled successfully, false if error
     */
    public boolean process_ROUTE(char sender, DatagramPacket dp, 
            String ip, DataInputStream dis) {
        
        if (sender == local_name) {
            Log2("Packet loopback in process_ROUTE - ignored\n");
            return true;
        }
        try {
            char area= dis.readChar();
            if (!in_area(area)) {
                Log2("process_ROUTE ignored PKT_ROUTE("+sender+'('+area+"))");
                return true;   // Ignored packet
            }

            Log("PKT_ROUTE("+sender+'('+area+"),");
            String aux;
            int seq= dis.readInt();
            aux= "seq="+seq+",";
            int TTL= dis.readInt();
            aux+= "TTL="+TTL+",";
            int n= dis.readInt();
            aux+= "List:"+n+": ";
            if ((n<=0) || (n>MAX_ENTRY_VEC_LEN)) {
                Log("\nInvalid list length '"+n+"'\n");
                return false;
            }
            Entry [] data= new Entry [n];
            for (int i= 0; i<n; i++) {
                try {
                    data[i]= new Entry(dis);
                } catch(IOException e) {
                    Log("\nERROR - Invalid vector Entry: "+e.getMessage()+"\n");
                    return false;                    
                }
                aux+= (i==0 ? "" : " ; ") + data[i].toString();
            }
            Log(aux+")\n");

            // Update Router vector
            RoutingProcess rp = get_Routing_process(area);
            return (rp != null) && rp.process_ROUTE(sender, seq, TTL, data);
        } catch (IOException e) {
            Log("\nERROR - Packet too short\n");
            return false;
        }
    }

    /**
     * Handle multicast ROUTE packets
     *
     * @param sender sender address
     * @param dp datagram packet received
     * @param ip IP address
     * @param dis input stream
     * @return true if handled successfully, false otherwise
     */
    public boolean process_multicast_ROUTE(char sender, DatagramPacket dp,
            String ip, DataInputStream dis) {
        if (sender == local_name) {
            // Packet loopback - ignore
            return true;
        }
        Log2("multicast ");
        return process_ROUTE(sender, dp, ip, dis);
    }
    

    /**
     * Recalculate Routing table
     * @return true if the Routing table was modified, false otherwise
     */
    public synchronized boolean update_global_routing_table() {
        Log("routing.update_global_routing_table does not support multiple areas yet\n");
        if (rprocesses == null) {
            Log("Internal error in update_routing_table: null route vector\n");
            return false;
        }
        
        // For comparizon purposes
        RoutingTable old= main_rtab;
                        
        RoutingProcess rp= rprocesses.get(areas.charAt(0));
        if (rp != null) {
            // Run dijkstra for all areas except '0'
            rp.run_dijkstra();
            win.Dijkstra_cnt++;     
            main_rtab= rp.get_routing_table();
        } else {
            Log("Internal error in update_global_routing_table: null rp0\n");
            return false;
        }
        
        // This function is incomplete for a multi-area routing LS protocol
        // You have to guarantee that every destination address can be reached
        // and is present in the routing table
        // ...

        // To log a table contents
        // main_rtab.Log_routing_table(this);
        
        // Echo Routing table 
        update_routing_window();
        return true;

    }
        
    /**
     * Display the Routing table in the GUI
     */
    public void update_routing_window() {
        Log2("update_routing_window\n");
        Iterator<RouteEntry> iter= null;
        if (main_rtab!=null) {
            iter= main_rtab.iterator();
        }

        // update window
        for (int i= 0; i<tableObj.getRowCount(); i++) {
            if ((main_rtab != null) && iter.hasNext()) {
                RouteEntry next= iter.next();
                tableObj.setValueAt(""+next.dest,i,0);
                tableObj.setValueAt(""+next.next_hop,i,1);
                tableObj.setValueAt(""+next.next_hop_area,i,2);
                tableObj.setValueAt(""+next.dist,i,3);
            } else {
                tableObj.setValueAt("",i,0);
                tableObj.setValueAt("",i,1);
                tableObj.setValueAt("",i,2);
                tableObj.setValueAt("",i,3);
            }
        }
    }
        
    
    /**
     * Launches timers responsible for sending periodic ROUTE packets to 
     *  neighbours in all areas
     */
    private void start_announce_timer() {
        if (rprocesses == null) {
            return;
        }
        // Start timers for all areas
        for (RoutingProcess rp : rprocesses.values()) {
            rp.start_announce_timer();
        }
    }
    
    /** 
     * Stops the timer responsible for sending periodic ROUTE packets to 
     *  neighbours in all areas 
     */
    private void stop_announce_timer() {
        if (rprocesses == null) {
            return;
        }
        for (RoutingProcess rp : rprocesses.values()) {
            rp.stop_announce_timer();
        }
    }

    
    
    /***************************************************************************
     *              DATA HANDLING
     */
    
    /**
     * returns next hop to reach destination
     * @param dest destination address
     * @return the address of the next hop, or ' ' if not found.
     */
    public char next_Hop(char dest) {
        if (main_rtab == null) {
            return ' ';
        }
        return main_rtab.nextHop(dest);
    }

    /**
     * send a DATA packet using the Routing table and the neighbor information
     * @param dest destination address
     * @param dp   datagram packet object
     */
    public void send_data_packet(char dest, DatagramPacket dp) {
        if (win.is_local_name(dest)) {
            // Send to local node
            try {
                dp.setAddress(InetAddress.getLocalHost());
                dp.setPort(ds.getLocalPort());
                ds.send(dp);
                win.DATA_snt++;
            }
            catch (UnknownHostException e) {
                Log("Error sending packet to himself: "+e+"\n");
            }
            catch (IOException e) {
                Log("Error sending packet to himself: "+e+"\n");
            }
            
        } else { // Send to Neighbour Router
            char prox= next_Hop(dest);
            if (prox == ' ') {
                Log("No route to destination: packet discarded\n");
            } else {
                // Lookup Neighbour
                Neighbour pt= neig.locate_neig(prox);
                if (pt == null) {
                    Log("Invalid neighbour ("+prox+
                        ") in routing table: packet discarder\n");
                    return;
                }
                try {
                    pt.send_packet(ds, dp);
                    win.DATA_snt++;
                }
                catch(IOException e) {
                    Log("Error sending DATA packet: "+e+"\n");
                }
            }            
        }
    }

    /** Prepare a data packet; adds local_name to path
     * @param sender    sender address
     * @param seq       sequence number
     * @param dest      destination
     * @param msg       message string
     * @param path      path
     * @return  Datagram packet object 
     */
    public DatagramPacket make_data_packet(char sender, int seq, char dest, 
            String msg, String path) {
        ByteArrayOutputStream os= new ByteArrayOutputStream();
        DataOutputStream dos= new DataOutputStream(os);
        try {
            dos.writeByte(Router.PKT_DATA);
            dos.writeChar(sender);
            dos.writeInt(seq);
            dos.writeChar(dest);
            dos.writeShort(msg.length());
            dos.writeBytes(msg);
            dos.writeByte(path.length()+1);
            dos.writeBytes(path+win.local_name());
        }
        catch (IOException e) {
            Log("Error encoding data packet: "+e+"\n");
            return null;
        }
        byte [] buffer = os.toByteArray();
        return new DatagramPacket(buffer, buffer.length);
    }
    
    /** Prepare and send a data packet; adds local_name to path 
     * @param sender    sender address
     * @param seq       sequence number
     * @param dest      destination
     * @param msg       message string
     * @param path      path 
     */
    public void send_data_packet(char sender, int seq, char dest, String msg,
            String path) {
        if (!Character.isUpperCase(sender)) {
            Log("Invalid sender '"+sender+"'\n");
            return;
        }
        if (!Character.isUpperCase(dest)) {
            Log("Invalid destination '"+dest+"'\n");
            return;
        }
        DatagramPacket dp= make_data_packet(sender, seq, dest, msg, path);
        if (dp != null) {
            send_data_packet(dest, dp);
        }
    }

    /** unmarshal DATA packet e process it
     * @param sender    sender's address
     * @param dp        DatagramPacket object
     * @param ip        IP address
     * @param dis       DataInputStream object
     * @return  true if read was successful 
     */
    public boolean process_DATA(char sender, DatagramPacket dp, 
            String ip, DataInputStream dis) {
        try {
            Log("PKT_DATA");
            if (!Character.isUpperCase(sender)) {
                Log("Invalid sender '"+sender+"'\n");
                return false;
            }
            // Read seq
            int seq= dis.readInt();
            // Read Dest
            char dest= dis.readChar();
            // Read message
            int len_msg= dis.readShort();
            if (len_msg>255) {
                Log(": message too long ("+len_msg+">255)\n");
                return false;
            }
            byte [] sbuf1= new byte [len_msg];
            int n= dis.read(sbuf1,0,len_msg);
            if (n != len_msg) {
                Log(": Invalid message length\n");
                return false;
            }
            String msg= new String(sbuf1,0,n);
            // Read path
            int len_path= dis.readByte();
            if (len_path>Router.MAX_PATH_LEN) {
                Log(": path length too long ("+len_msg+">"+Router.MAX_PATH_LEN+
                    ")\n");
                return false;
            }
            byte [] sbuf2= new byte [len_path];
            n= dis.read(sbuf2,0,len_path);
            if (n != len_path) {
                Log(": Invalid path length\n");
                return false;
            }
            String path= new String(sbuf2,0,n);
            Log(" ("+sender+"-"+dest+"-"+seq+"):'"+msg+"':Path='"+path+win.local_name()+"'\n");
            // Test Routing table
            if (win.is_local_name(dest)) {
                // Arrived at destination
                Log("DATA packet reached destination\n");
                return true;
            } else {
                char prox= next_Hop(dest);
                if (prox == ' ') {
                    Log("No route to destination: packet discarded\n");
                    return false;
                } else {
                    // Send packet to next hop
                    send_data_packet(sender, seq, dest, msg, path);
                    return true;
                }
            }
        }
        catch (IOException e) {
            Log(" Error decoding data packet: " + e + "\n");
        }
        return false;       
    }
    
    
    /***************************************************************************
     *              Log functions
     */
    
    
    /**
     * Output the string to the log text window and command line
     * @param s log string
     */
    @Override
    public void Log(String s) {
        if (win != null) {
            win.Log(s);
        }
    }
    
    /**
     * Auxiliary log function - when more detail is required remove the comments
     * @param s log string
     */
    public final void Log2(String s) {
        //System.err.println(s);
        //if (win != null)
        //    win.Log(s);  // For detailed debug purposes
    }


    
    /***************************************************************************
     *              Variables
     */

    /** Routing table object */
    public RoutingTable main_rtab;
    /** Array of Routing process objects */
    private final HashMap<Character, RoutingProcess> rprocesses;
    
    /** Local address name */
    private char local_name;
    /** String with all local areas */
    private final String areas;
    /** Neighbour list */
    private NeighbourList neig;
    /** TTL value used in sent ROUTE packets */
    private final int local_TTL;
    /** Reference to main window with GUI */
    private Router win;
    /** Unicast datagram socket used to send packets */
    private DatagramSocket ds;
    /** Reference to graphical Routing table object */
    private JTable tableObj;

    // A multicast socket is used initialy to broadcast the ROUTE packets!
    public MulticastDaemon mdaemon;
}
