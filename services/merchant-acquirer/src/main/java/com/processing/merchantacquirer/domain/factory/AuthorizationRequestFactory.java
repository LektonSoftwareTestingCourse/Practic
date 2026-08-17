package com.processing.merchantacquirer.domain.factory;

import com.processing.merchantacquirer.domain.service.StanGenerator;
import com.processing.merchantacquirer.domain.entity.Merchant;
import com.processing.merchantacquirer.domain.entity.Terminal;
import com.processing.common.dto.authorization.AuthorizationRequest;

import java.time.Instant;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationRequestFactory {
  private final StanGenerator stanGenerator;

  public AuthorizationRequest build(
          String pan, String currencyCode, BigDecimal amount, Terminal terminal, Merchant merchant, Instant time) {

    return AuthorizationRequest.builder()
        .mti("0100")
        .stan(stanGenerator.next(terminal.getId()))
        .pan(pan)
        .processingCode("000000")
        .amount(amount)
        .currencyCode(currencyCode)
        .transmissionDateTime(time)
        .terminalId(terminal.getId())
        .terminalType(terminal.getType())
        .merchantId(merchant.getId())
        .mcc(merchant.getMcc())
        .acquirerId(merchant.getAcquirerId())
        .build();
  }
}
