package com.eskcti.algashop.billing.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter(AccessLevel.PRIVATE)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractAuditableEntity {

  @CreatedBy
  protected UUID createdByUserId;

  @CreatedDate
  protected OffsetDateTime createdAt;

  @LastModifiedBy
  protected UUID lastModifiedByUserId;

  @LastModifiedDate
  protected OffsetDateTime lastModifiedDate;

  @Version
  protected long version;

}