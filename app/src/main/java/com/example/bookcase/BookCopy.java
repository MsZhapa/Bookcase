package com.example.bookcase;

import com.google.firebase.Timestamp;

/** One physical copy. Position is retained while lent; no row/top category. */
public class BookCopy {
    private String id;
    private String editionId;
    private int cubeNumber;
    private long position;
    private String lendingStatus = "AVAILABLE";
    private String borrowerName;
    private String activeLoanId;
    private Timestamp lentDate;
    private Timestamp dueDate;
    private boolean read;
    private int progress;
    public BookCopy() {}
    public BookCopy(String id, String editionId, int cubeNumber, long position) {
        this.id = id; this.editionId = editionId; this.cubeNumber = cubeNumber; this.position = position;
    }
    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getEditionId() { return editionId; }
    public void setEditionId(String value) { editionId = value; }
    public int getCubeNumber() { return cubeNumber; }
    public void setCubeNumber(int value) { cubeNumber = value; }
    public long getPosition() { return position; }
    public void setPosition(long value) { position = value; }
    public String getLendingStatus() { return lendingStatus; }
    public void setLendingStatus(String value) { lendingStatus = value; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String value) { borrowerName = value; }
    public String getActiveLoanId() { return activeLoanId; }
    public void setActiveLoanId(String value) { activeLoanId = value; }
    public Timestamp getLentDate() { return lentDate; }
    public void setLentDate(Timestamp value) { lentDate = value; }
    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp value) { dueDate = value; }
    public boolean isRead() { return read; }
    public void setRead(boolean value) { read = value; }
    public int getProgress() { return progress; }
    public void setProgress(int value) { progress = value; }
}
