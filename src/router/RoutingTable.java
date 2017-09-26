/**
 * Redes Integradas de Telecomunicacoes I MIEEC 2014/2015
 *
 * routing.java
 *
 * Encapsulates the routing functions, hosting multiple instances of
 * Routing_process objects, and handles DATA packets
 *
 * Created on 7 de Setembro de 2014, 18:00
 *
 * @author Luis Bernardo
 */
package router;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public final class RoutingTable {
    /** Routing table object */
    private final HashMap<Character,RouteEntry> rtab;
    
    /**
     * Constructor
     */
    public RoutingTable() {
        rtab= new HashMap<>();
    }

    /**
     * Constructor that clones table received
     * @param src  Initial table 
     */
    public RoutingTable(RoutingTable src) {
        rtab= new HashMap<>();
        merge_table(src, ' ');
    }
    
    /**
     * Check if the routing table is defined 
     * @return true if it is defined
     */
    public boolean is_valid() {
        return (rtab!=null);
    }
    
    public void clear() {
        if (rtab!=null)
            rtab.clear();
    }
    
    /**
     * Add route entry to routing table
     * @param re RouteEntry object
     */
    public void add_route(RouteEntry re) {
        rtab.put(re.dest, re);
    }
    
    /**
     * Merge routing table - select the shortest distance and exclude routes through exclude_area
     * @param rt  Routing table to merge
     * @param exclude_area  Exclude area, ' ' does not exclude any area
     */
    public void merge_table(RoutingTable rt, char exclude_area) {
        if ((rt == null) || !rt.is_valid())
            return;
        for (RouteEntry re: rt.rtab.values()) {
            RouteEntry aux= rtab.get(re.dest);
            if ((re.next_hop_area != exclude_area) && 
                    ((aux==null) || (re.dist < aux.dist)))
                rtab.put(re.dest, new RouteEntry(re));
        }
    }
    
    /**
     * Returns the RouteEntry associated to a destination
     * @param dest destination
     * @return RouteEntry object
     */
    public RouteEntry get_RouteEntry(char dest) {
        if (!is_valid())
            return null;
        return rtab.get(dest);
    }
    
    /**
     * Return the route's set
     * @return set of all RouteEntry 
     */
    public Collection<RouteEntry> get_routeset() {
        if (!is_valid())
            return null;
        return rtab.values();
    }
    
    /**
     * Return the routing table as an array of Entry
     * @return Entry vector with table contents 
     */
    public Entry[] get_Entry_vector() {
        if (!is_valid())
            return null;
        Entry[] vec= new Entry[rtab.size()];
        Iterator<RouteEntry> it= rtab.values().iterator();
        for (int i= 0; (i<vec.length) && it.hasNext(); i++) {
            RouteEntry re= it.next();
            vec[i]= new Entry(re);
        }
        return vec;
    }   
    
    /**
     * Returns the next hop address in the path to dest
     * @param dest destination
     * @return the next hop address
     */
    public char nextHop(char dest) {
        RouteEntry re= get_RouteEntry(dest);
        if (!is_valid())
            return ' ';
        return re.next_hop;
    }
    
    /**
     * Builds an iterator to the RouteEntry values
     * @return 
     */
    public Iterator<RouteEntry> iterator() {
        if (!is_valid())
            return null;
        return rtab.values().iterator();
    }
    
    /**
     * Compare the local routing tables with rt
     * @param rt - routing table
     * @return true if rt is equal to rtab and not null, false otherwise
     */
    public boolean equal_RoutingTable(RoutingTable rt) {
        if ((rt == null) || !rt.is_valid() || is_valid() )
            return false;
        HashMap<Character, RouteEntry> map= rt.rtab;
        if (rtab.size() != map.size()) {
            return false;
        }
        Iterator<RouteEntry> it= rt.iterator();
        while (it.hasNext()) {
            RouteEntry re= it.next();
            if (!re.equals_to(rtab.get(re.dest))) {
                return false;
            }                
        }      
        return true;
    } 
    
    /**
     * Log the content of a routing table object
     * @param log Logging object
     */
    public void Log_routing_table(Log log) {
        if (rtab==null) {
            return;
        }
        for (RouteEntry re: rtab.values()) {
            log.Log(re.toString()+"\n");
        }
    }
    
}
