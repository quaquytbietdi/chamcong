package com.example.appchamcong.model;

public class Staff {

    public enum Status { ON_TIME, LATE, ABSENT }

    private String id;
    private String name;
    private Long checkInTimeMillis;
    private Long checkOutTimeMillis;
    private Status status;

    // Constructor rỗng (Bắt buộc phải có để Firebase ánh xạ dữ liệu)
    public Staff() {
    }

    // Constructor đầy đủ
    public Staff(String id, String name, Status status) {
        this.id = id;
        this.name = name;
        this.status = status;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Long getCheckInTimeMillis() { return checkInTimeMillis; }
    public Long getCheckOutTimeMillis() { return checkOutTimeMillis; }
    public Status getStatus() { return status; }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCheckInTimeMillis(Long v) { this.checkInTimeMillis = v; }
    public void setCheckOutTimeMillis(Long v) { this.checkOutTimeMillis = v; }
    public void setStatus(Status status) { this.status = status; }
}