package com.processing.merchantacquirer.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;

@Entity
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Merchants")
@Schema(description = "Мерчант (торговая точка)")
public class Merchant {
  @Schema(description = "Идентификатор мерчанта", example = "MERCH00000000001")
  @Id private String id;

  @Schema(description = "Наименование мерчанта", example = "Пятёрочка #1234")
  private String name;

  @Schema(description = "MCC-код (Merchant Category Code)", example = "5411")
  @Column(name = "mcc", length = 4)
  @JdbcTypeCode(Types.CHAR)
  private String mcc;

  @Schema(description = "Категория мерчанта", example = "grocery")
  private String category;

  @Schema(description = "Идентификатор эквайрера", example = "ACQ001")
  private String acquirerId;

  @Schema(description = "Комиссия эквайрера (доля), например 0.015 = 1.5%", example = "0.0150")
  @Column(name = "acquiring_fee", precision = 19, scale = 4)
  private BigDecimal acquiringFee;

  @Schema(description = "Средний чек в копейках", example = "85000")
  private BigDecimal averageCheck;
}
