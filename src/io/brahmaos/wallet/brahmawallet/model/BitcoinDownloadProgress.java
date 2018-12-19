package io.brahmaos.wallet.brahmawallet.model;

import org.bitcoinj.core.Peer;

import java.io.Serializable;
import java.util.Date;

public class BitcoinDownloadProgress implements Serializable {
    private Peer peer;
    private int blocksLeft;
    private double progressPercentage;
    private Date currentBlockDate;
    private String currentBlockDateString;
    private boolean downloaded;

    public Peer getPeer() {
        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    public int getBlocksLeft() {
        return blocksLeft;
    }

    public void setBlocksLeft(int blocksLeft) {
        this.blocksLeft = blocksLeft;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Date getCurrentBlockDate() {
        return currentBlockDate;
    }

    public void setCurrentBlockDate(Date currentBlockDate) {
        this.currentBlockDate = currentBlockDate;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public String getCurrentBlockDateString() {
        return currentBlockDateString;
    }

    public void setCurrentBlockDateString(String currentBlockDateString) {
        this.currentBlockDateString = currentBlockDateString;
    }

    @Override
    public String toString() {
        return "BitcoinDownloadProgress{" +
                "peer=" + peer +
                ", blocksLeft=" + blocksLeft +
                ", progressPercentage=" + progressPercentage +
                ", currentBlockDate=" + currentBlockDate +
                ", currentBlockDateString='" + currentBlockDateString + '\'' +
                ", downloaded=" + downloaded +
                '}';
    }
}
