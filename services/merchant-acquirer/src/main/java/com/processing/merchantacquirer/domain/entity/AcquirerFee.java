package com.processing.merchantacquirer.domain.entity;

import com.processing.common.dto.authorization.AuthorizationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;

import java.math.BigDecimal;


@Entity
@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@Schema(description = "Справочная запись о комиссии эквайрера по сгенерированной транзакции")
public class AcquirerFee {
    @Schema(description = "Внутренний идентификатор записи", example = "1001")
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acquirer_fee_seq")
    @SequenceGenerator(name = "acquirer_fee_seq", sequenceName = "acquirer_fee_seq", allocationSize = 100)
    private Long id;

    @Schema(description = "Дата и время передачи транзакции (ISO-8601)", example = "2026-06-22T10:15:30Z")
    @Column(name = "transmission_date_time", nullable = false)
    private Instant transmissionDateTime;

    @Schema(description = "System trace audit number (STAN)", example = "000301")
    private String stan;

    @Schema(description = "Номер карты (PAN)", example = "4000000000000002")
    private String pan;

    @Schema(description = "Идентификатор терминала", example = "TERM042")
    @Column(name = "terminal_id")
    private String terminalId;

    @Schema(description = "Рассчитанная комиссия эквайрера в копейках", example = "1275")
    private BigDecimal acquirerFee;

    @Schema(description = "Сумма транзакции в копейках", example = "85000")
    private BigDecimal amount;

    public AcquirerFee(
            Instant transmissionDateTime, String stan, String pan, String terminalId, BigDecimal acquirerFee, BigDecimal amount) {
        this.transmissionDateTime = transmissionDateTime;
        this.stan = stan;
        this.pan = pan;
        this.terminalId = terminalId;
        this.acquirerFee = acquirerFee;
        this.amount = amount;
    }

    public static AcquirerFee of(BigDecimal fee, AuthorizationRequest request) {
        return new AcquirerFee(
                request.transmissionDateTime(),
                request.stan(),
                request.pan(),
                request.terminalId(),
                fee,
                request.amount()
        );
    }
}
