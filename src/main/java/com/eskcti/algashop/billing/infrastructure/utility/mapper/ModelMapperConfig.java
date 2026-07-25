package com.eskcti.algashop.billing.infrastructure.utility.mapper;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.modelmapper.convention.NamingConventions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.eskcti.algashop.billing.application.utility.Mapper;

@Configuration
public class ModelMapperConfig {

  @Bean
  public Mapper mapper() {
    ModelMapper modelMapper = new ModelMapper();
    configuration(modelMapper);
    return modelMapper::map;
  }

  private void configuration(ModelMapper modelMapper) {
    modelMapper.getConfiguration()
        .setSourceNamingConvention(NamingConventions.NONE)
        .setDestinationNamingConvention(NamingConventions.NONE)
        .setMatchingStrategy(MatchingStrategies.STRICT);
  }

}