package com.englishlearning.common.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.cfg.DateTimeFeature;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

@Configuration(proxyBeanMethods = false)
public class ApiJsonConfiguration {
    @Bean
    JsonMapperBuilderCustomizer strictRequestJson() {
        SimpleModule timestamps = new SimpleModule();
        timestamps.addDeserializer(Instant.class, new StdDeserializer<Instant>(Instant.class) {
            @Override
            public Instant deserialize(JsonParser parser, DeserializationContext context) {
                if (parser.currentToken() != JsonToken.VALUE_STRING) {
                    return context.reportInputMismatch(Instant.class,
                            "Expected an ISO-8601 timestamp with an offset.");
                }
                try {
                    return OffsetDateTime.parse(parser.getString()).toInstant();
                } catch (DateTimeParseException exception) {
                    return context.reportInputMismatch(Instant.class,
                            "Expected an ISO-8601 timestamp with an offset.");
                }
            }
        });
        return builder -> builder.addModule(timestamps)
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                        DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS,
                        MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
