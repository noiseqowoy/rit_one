/**
 * Redes Integradas de Telecomunicacoes I
 * MIEEC 2017/2018
 *
 * RouterInfo.java
 *
 * Holds the information regarding the routers sending ROUTE packets
 * Note that in link state the routers flood the ROUTE packets - they come from 
 * all nodes in an area
 *
 * Created on 7 de Setembro de 2018, 16:00
 * @author  Luis Bernardo
 */

package router;

import java.util.Date;
import java.util.HashMap;

/**
 * Auxiliary class to hold routing information received from each Router
 */
public class RouterInfo {

    /**
     * class with specific area data
     */
    /** address name */
    public char name;
    /** area where the ROUTE was received */
    public char area;
    /** Entry vector with neighbour list received */
    public Entry[] vec;
    /** Last sequence number */
    public int seq;
    /** Time To Live (s) */
    public int TTL;
    /** Time when the vector was received */
    public Date date;
    /** Reference to the main window of the GUI */
    private Router win;

    /**
     * Creates a new instance of RouterInfo
     */
    /**
     * Constructor - creates a new instance of RouterInfo
     * @param win   Reference to the main window of the GUI
     * @param name  address name
     * @param area  area where the ROUTE was received
     * @param seq   ROUTE sequence number
     * @param TTL   Time To Live (s)
     * @param vec   Entry vector with neighbour list
     */
    public RouterInfo(Router win, char name, char area, int seq, int TTL, Entry[] vec) {
        this.name = name;
        this.area = area;
        this.vec = vec;
        this.seq = seq;
        this.TTL = TTL;
        this.date = new Date();
        this.win = win;
    }

    /**
     * Constructor - clones the content of another object
     * @param src  object to be cloned
     */
    public RouterInfo(RouterInfo src) {
        this.name = src.name;
        this.area = src.area;
        this.vec = src.vec;
        this.seq = src.seq;
        this.TTL = src.TTL;
        this.date = src.date;
    }

    /**
     * Update the vector received
     * @param vec   Entry vector received
     * @param seq   sequence number
     * @param TTL   Time to live
     */
    public void update_vec(Entry[] vec, int seq, int TTL) {
        this.date = new Date(); // Get current time
        this.vec = vec;
        this.seq = seq;
        this.TTL = TTL;
    }

    /**
     * Test if the vector is still valid (is defined and TTL has not elapsed
     * @return true if is valid, false otherwise
     */
    public boolean vec_valid() {
        win.Log("RouterInfo.vec_valid not implemented yet\n");
        // Place here the code to test if the vector stored in vec is valid or not
        // ...
        return true;
    }

    /**
     * Test if the vector in _vec is valid
     * @param _vec  vector to be tested
     * @return true if valid, false otherwise
     */
    private boolean test_vec_contents(Entry[] _vec) {
        if (_vec == null) {
            return false;
        }
        HashMap<String, String> h = new HashMap<>();
        for (int i = 0; i < _vec.length; i++) {
            if (h.containsKey("" + _vec[i].dest)) {
                win.Log("Invalid vector - duplicated destination '"
                        + _vec[i].dest + "'\n");
                return false;
            }
            h.put("" + _vec[i].dest, "");
        }
        return true;
    }

    /**
     * Test if the objects' vector is equal to _vec
     * @param _vec  the vector to be compared
     * @return true if different, false otherwise
     */
    public boolean test_diff_vec(Entry[] _vec) {
        if (!test_vec_contents(_vec)) {
            return false;
        }
        if (vec == null) {
            return (_vec != null);
        }
        if (_vec == null) {
            return true;
        }
        // both are different of null and _vec is valid
        if (vec.length != _vec.length) {
            return true;
        }
        int cnt = 0;
        for (int i = 0; i < vec.length; i++) {
            for (int j= 0; j < _vec.length; j++) {
                if (vec[i].equals_to(_vec[j])) {
                    cnt++;
                }
            }
        }
        return (cnt != vec.length);
    }
}
