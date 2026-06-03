package org.example.ca_agent.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "report")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
