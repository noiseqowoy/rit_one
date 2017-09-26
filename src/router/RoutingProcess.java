/**
 * Redes Integradas de Telecomunicacoes I
 * MIEEC 2017/2018
 *
 * RoutingProcess.java
 *
 * Holds the routing algorithm and the routing calculation for a single area
 *
 * Created on 7 de Setembro de 2017, 16:00
 * @author  Luis Bernardo
 */

package router;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Stores Routing information for each area
 */
public class RoutingProcess {    
    /** Routing Table */
    private RoutingTable rtab;

    /** area of the Routing process */
    public char area;
    /** List of routers with the ROUTE packets' information received (RouterInfo) */
    public HashMap<Character, RouterInfo> map;
    /** time of the last ROUTE packet sent */
    public Date lastSending;
    /** Sequence number of the next ROUTE packet to be sent */
    private int route_seq;
    /** Timer object that sends ROUTE packets */
    private javax.swing.Timer timer_announce;

    /** Routing object that coordinates multiple areas */
    private final Routing route;
    /** Main window object with the GUI */
    private final Router win;
    /** Neighbour list */
    private final NeighbourList neig;
    /** ROUTE packet's transmission period (s) */
    private final int period;
    /** Minimum interval between consecutive ROUTE packets (ms) */
    private final int min_interval;

    /**
     * Constructor; receives the configuration parameters from the main Routing process
     * @param route         Routing process
     * @param win           main window
     * @param neig          neighbor list
     * @param area          local area
     * @param period        ROUTE period (s)
     * @param min_interval  Minimum interval between ROUTE packets (ms)
     */
    public RoutingProcess(Routing route, Router win, NeighbourList neig, char area, int period /*s*/, int min_interval /*ms*/) {
        this.area = area;
        this.map = new HashMap<>();
        this.route = route;
        this.win = win;
        this.neig = neig;
        this.lastSending = null;
        this.rtab = new RoutingTable();
        this.timer_announce = null;
        this.route_seq = 1;
        this.period = period;
        this.min_interval = min_interval;
    }

    /**
     * Handles the reception of a ROUTE packet
     * @param sender packet sender
     * @param seq    sequence number
     * @param TTL    Time To Live
     * @param data   Entry vector received
     * @return true if the vector changed, false otherwise
     */
    public boolean process_ROUTE(char sender, int seq, int TTL, Entry[] data) {
        boolean changed = false;
        if (map == null) {
            return false;
        }

        win.Log("RoutingProcess.process_ROUTE not yet implemented\n");
        RouterInfo ri = new RouterInfo(win, sender, area, seq, TTL, data);
        // Validate stuff, check TTL
        
        /*if (data_validated){
            map.put(sender, ri);
        }*/
        if (ri.test_diff_vec(data) && win.sendIfChanges()){
            network_changed(false);
    }
        
        // The code must create a new RouterInfo object ...
        //    RouterInfo pt = new RouterInfo(win, sender, area, seq, TTL, data);
        // ... and place it in the corresponding list ...
        //    map.put(sender, pt);    // Stores the information in the list
        
        // Note that some tests must be performed before replacing existing 
        // information with the new one (is it valid? is it different? ...)
        //   ...
        
        // The function returns true if the vector contents changed ...
        // You can use test_diff_vec from class RouterInfo to compare vectors
        
        // It should also call network_chnged if sendifchanges is active:
        // if (changed && win.sendIfChanges()) {
        //    network_changed(false);
        // } 
        
        return changed;
    }

    /**
     * Get the Routing table contents
     * @return the Routing table
     */
    public RoutingTable get_routing_table() {
        return rtab;
    }


    /**
     * Run the Dijkstra algorithm, setting the Routing table in main_rtab variable
     * @return true if Routing tables changed, false otherwise
     */
    public boolean run_dijkstra() {
        char nextN;

        RoutingTable old= rtab;
        rtab= new RoutingTable();

        // Load local node and neighbors
        rtab = route.local_route_table(area);

        // Run Dijkstra algorithm        
        win.Log("RoutingProcess.run_dijkstra not implemented yet\n");
        
        // Place here the code to implement the Dijkstra algorithm directly over
        //   the rtab object
        
        // Read carefully the pages 366-369 of Computer Networks 5th edition
        // to understand how it is done
        // Remember that there is a maximum distance allowed!
        
        // Return true if Routing table changed
        return (old==null)|| ((rtab!=null) && !rtab.equal_RoutingTable(old));
    }

    /**
     * Send a ROUTE packet with neighbours' information
     * @param send_if_equal Send if is equal to previous if true, drop it otherwise
     * @return true if successful, false otherwise
     */
    public boolean send_local_ROUTE(boolean send_if_equal) {
        win.Log("send_local_ROUTE(" + area + ")\n");

        //
        Entry[] vec = route.local_vec(area);
        if (vec == null) { // No vector
            return false;
        }

        DatagramPacket dp = route.make_ROUTE_packet(route.local_name(), area, route_seq++, vec);
        try {
            route.mdaemon.send_packet(dp);
            lastSending = new Date();
            win.ROUTE_snt++;
            win.ROUTE_loc++;
            return true;
        } catch (IOException e) {
            win.Log("Error sending ROUTE: " + e + "\n");
            return false;
        }
    }


    /**
     * Handle timer event - update Routing table and send ROUTE
     */
    public void update_routing_table() {
        run_dijkstra();
        win.Dijkstra_cnt++;

        send_local_ROUTE(true);
        
        // Signal Routing object that this area was calculated
        route.area_tab_calculated(area, false);
    }

 
    /**
     * Launches timer responsible for sending periodic distance packets to
     * neighbours
     */
    public void start_announce_timer() {
        // Start here the implementation of the router project
        // ...
        // The timer should run the update_routing_table() function!
        java.awt.event.ActionListener act;
        act = new java.awt.event.ActionListener() { // define função corrida
        
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                update_routing_table();
            }
        // Código executado quando o temporizador disparar
        };
        timer_announce = new javax.swing.Timer(period, act);// Cria objeto timer
        timer_announce.setDelay(period);
        timer_announce.start();
        System.out.println("Timer announce");
        
    };
           

    /**
     * Stops the timer responsible for sending periodic distance packets to
     * neighbours
     */
    public void stop_announce_timer() {
        win.Log("RoutingProcess.stop_announce_timer not implemented yet\n");
        timer_announce.stop();
    }

    /**
     * Tests if the minimum interval time has elapsed since last sending
     * @return true if the time elapsed, false otherwise
     */
    public boolean test_time_since_last_update() {
        return (lastSending == null)
                || ((System.currentTimeMillis() - lastSending.getTime()) >= min_interval);
    }


    /**
     * Handle area changes - update Routing table and send ROUTE
     * @param send_ROUTE if true always send ROUTE packet
     */
    public void network_changed(boolean send_ROUTE) {
        win.Log("RoutingProcess.network_changed not implemented yet\n");
        
        // I used this function to implement the actions when the network changes,
        // including the recalculation and sending of ROUTE packets after a 
        //    modification satisfying the min_interval time
        // ...


        // Do not forget to call 
        // route.area_tab_calculated(area, changed);
    }

}
