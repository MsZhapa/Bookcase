package com.example.bookcase;

import com.google.firebase.Timestamp;

public class LendingRecord {

    private String id;
    private String bookId;
    private String bookCopyId;
    public String getBookCopyId() { return bookCopyId; }
    public void setBookCopyId(String value) { bookCopyId = value; }
    private String borrowerName;
    private Timestamp lentDate;
    private Timestamp dueDate;
    private Timestamp returnedDate;
    private String status;

    public LendingRecord() {
        // Required by Firestore
    }

    public LendingRecord(String id, String bookId,
                         String borrowerName,
                         Timestamp lentDate,
                         Timestamp dueDate) {

        this.id = id;
        this.bookId = bookId;
        this.bookCopyId = bookId;
        this.borrowerName = borrowerName;
        this.lentDate = lentDate;
        this.dueDate = dueDate;
        this.returnedDate = null;
        this.status = "ACTIVE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBookId() { return bookId; }
    public void setBookId(String bookId) { this.bookId = bookId; }

    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String borrowerName) {
        this.borrowerName = borrowerName;
    }

    public Timestamp getLentDate() { return lentDate; }
    public void setLentDate(Timestamp lentDate) {
        this.lentDate = lentDate;
    }

    public Timestamp getDueDate() { return dueDate; }
    public void setDueDate(Timestamp dueDate) {
        this.dueDate = dueDate;
    }

    public Timestamp getReturnedDate() { return returnedDate; }
    public void setReturnedDate(Timestamp returnedDate) {
        this.returnedDate = returnedDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = status;
    }
}
