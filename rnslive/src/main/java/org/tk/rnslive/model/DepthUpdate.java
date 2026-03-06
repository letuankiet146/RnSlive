package org.tk.rnslive.model;

import java.util.List;

public class DepthUpdate {
    private String e; // Event type
    private long E; // Event time
    private long T; // Transaction time
    private String s; // Symbol
    private long U; // First update ID
    private long u; // Final update ID
    private long pu; // Previous final update ID
    private List<List<String>> b; // Bids
    private List<List<String>> a; // Asks

    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
    }

    public long getEventTime() {
        return E;
    }

    public void setEventTime(long E) {
        this.E = E;
    }

    public long getTransactionTime() {
        return T;
    }

    public void setTransactionTime(long T) {
        this.T = T;
    }

    public String getSymbol() {
        return s;
    }

    public void setSymbol(String s) {
        this.s = s;
    }

    public long getFirstUpdateId() {
        return U;
    }

    public void setFirstUpdateId(long U) {
        this.U = U;
    }

    public long getFinalUpdateId() {
        return u;
    }

    public void setFinalUpdateId(long u) {
        this.u = u;
    }

    public long getPreviousFinalUpdateId() {
        return pu;
    }

    public void setPreviousFinalUpdateId(long pu) {
        this.pu = pu;
    }

    public List<List<String>> getBids() {
        return b;
    }

    public void setBids(List<List<String>> b) {
        this.b = b;
    }

    public List<List<String>> getAsks() {
        return a;
    }

    public void setAsks(List<List<String>> a) {
        this.a = a;
    }
}
