package com.processing.cardmanagement.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bin_issuers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BinIssuerEntity {
    @Id
    @Column(length = 6, nullable = false)
    private String bin;

    @Column(length = 10, nullable = false)
    private String issuerId;
}
