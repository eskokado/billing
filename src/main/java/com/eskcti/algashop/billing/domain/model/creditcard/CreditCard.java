package com.eskcti.algashop.billing.domain.model.creditcard;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

import com.eskcti.algashop.billing.domain.model.IdGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter(AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditCard {
  @EqualsAndHashCode.Include
  private UUID id;
  private OffsetDateTime createdAt;
  private UUID customerId;
  private String lastNumbers;
  private String brand;
  private Integer expMonth;
  private Integer expYear;
  private String gatewayCode;

  public static CreditCard brandNew(UUID customerId,
      String lastNumbers,
      String brand,
      Integer expMonth,
      Integer expYear,
      String gatewayCreditCardCode) {
    Objects.requireNonNull(customerId);
    Objects.requireNonNull(expMonth);
    Objects.requireNonNull(expYear);

    if (StringUtils.isAnyBlank(lastNumbers, brand, gatewayCreditCardCode)) {
      throw new IllegalArgumentException();
    }

    return new CreditCard(
        IdGenerator.generateTimeBasedUUID(),
        OffsetDateTime.now(),
        customerId,
        lastNumbers,
        brand,
        expMonth,
        expYear,
        gatewayCreditCardCode);
  }

  public void setGatewayCode(String gatewayCode) {
    if (StringUtils.isBlank(gatewayCode)) {
      throw new IllegalArgumentException();
    }
    this.gatewayCode = gatewayCode;
  }
}