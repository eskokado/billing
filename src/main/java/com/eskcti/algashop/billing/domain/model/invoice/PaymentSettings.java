package com.eskcti.algashop.billing.domain.model.invoice;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.eskcti.algashop.billing.domain.model.DomainException;
import com.eskcti.algashop.billing.domain.model.IdGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
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
@Entity
public class PaymentSettings {

  @Id
  @EqualsAndHashCode.Include
  private UUID id;
  private UUID creditCardId;
  private String gatewayCode;

  @Enumerated(EnumType.STRING)
  private PaymentMethod method;

  @OneToOne(mappedBy = "paymentSettings")
  @Getter(AccessLevel.PRIVATE)
  @Setter(AccessLevel.PACKAGE)
  private Invoice invoice;

  static PaymentSettings brandNew(PaymentMethod method, UUID creditCardId) {
    Objects.requireNonNull(method);
    if (method.equals(PaymentMethod.CREDIT_CARD)) {
      Objects.requireNonNull(creditCardId);
    }
    return new PaymentSettings(
        IdGenerator.generateTimeBasedUUID(),
        creditCardId,
        null,
        method,
        null);
  }

  void assignGatewayCode(String gatewayCode) {
    if (StringUtils.isBlank(gatewayCode)) {
      throw new IllegalArgumentException();
    }
    if (this.getGatewayCode() != null) {
      throw new DomainException("Gateway code already assigned");
    }
    setGatewayCode(gatewayCode);
  }
}