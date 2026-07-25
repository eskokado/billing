package com.eskcti.algashop.billing.domain.model;

import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator;

public class IdGenerator {
  private static final TimeBasedEpochRandomGenerator timeBasedEpochRandomGenerator = Generators
      .timeBasedEpochRandomGenerator();

  private IdGenerator() {
  }

  public static UUID generateTimeBasedUUID() {
    return timeBasedEpochRandomGenerator.generate();
  }

}