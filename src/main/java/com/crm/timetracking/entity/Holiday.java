package com.crm.timetracking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "holidays",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_holiday_date_name",
        columnNames = {"date", "name", "country"}
    ),
    indexes = {
        @Index(name = "idx_holidays_year_country", columnList = "year, country"),
        @Index(name = "idx_holidays_date",         columnList = "date")
    }
)
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    private String type = "PUBLIC";

    @Column(name = "country", nullable = false, length = 2)
    private String country = "IL";

    @Column(name = "year", nullable = false)
    private Short year;

    @Column(name = "credit_hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal creditHours = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    protected Holiday() {}

    public Holiday(LocalDate date, String name, String type, String country, BigDecimal creditHours) {
        this.date        = date;
        this.name        = name;
        this.type        = type;
        this.country     = country;
        this.creditHours = creditHours;
        this.year        = (short) date.getYear();
    }

    public Long getId()                  { return id; }
    public LocalDate getDate()           { return date; }
    public String getName()              { return name; }
    public String getType()              { return type; }
    public String getCountry()           { return country; }
    public Short getYear()               { return year; }
    public BigDecimal getCreditHours()   { return creditHours; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
