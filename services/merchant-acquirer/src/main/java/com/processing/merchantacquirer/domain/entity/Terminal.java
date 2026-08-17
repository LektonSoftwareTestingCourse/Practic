package com.processing.merchantacquirer.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Terminals")
@Schema(description = "Платёжный терминал, привязанный к мерчанту")
public class Terminal {
  @Schema(description = "Идентификатор терминала", example = "TERM042")
  @Id
  private String id;

  @Schema(description = "Тип терминала", example = "POS")
  private String type;

  @Schema(description = "Мерчант, которому принадлежит терминал")
  @ManyToOne
  private Merchant merchant;
}
