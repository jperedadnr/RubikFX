package com.jpl.games.model;

/**
 *
 * @author jpereda, April 2014 - @JPeredaDnr
 */
public class Move{
    private String face;
    private long timestamp;
    
    public Move(String face, long timestamp){
        this.face=face;
        this.timestamp=timestamp;
    }
    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Move{" + "face=" + face + ", timestamp=" + timestamp + '}';
    }

}
