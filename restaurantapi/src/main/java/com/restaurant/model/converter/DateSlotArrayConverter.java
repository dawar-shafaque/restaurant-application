package com.restaurant.model.converter;

import com.restaurant.exception.InternalServerErrorException;
import com.restaurant.utils.DateSlot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Slf4j
public class DateSlotArrayConverter implements AttributeConverter<DateSlot[]> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(DateSlot[] dateSlots) {
        try {
            if (dateSlots == null) {
                return AttributeValue.builder().s("[]").build();
            }
            String json = MAPPER.writeValueAsString(dateSlots);
            return AttributeValue.builder().s(json).build();
        }
        catch (JsonProcessingException e) {
            log.error("Failed to serialize DateSlot array: " + e.getMessage());
            throw new InternalServerErrorException("Failed to serialize DateSlot array");
        }
    }

    @Override
    public DateSlot[] transformTo(AttributeValue attributeValue) {
        try {
            if (attributeValue == null || attributeValue.s() == null || attributeValue.s().isEmpty()) {
                return new DateSlot[0];
            }

            return MAPPER.readValue(attributeValue.s(), new TypeReference<DateSlot[]>() {
            });
        }
        catch (JsonProcessingException e) {
                log.error("Failed to deserialize JSON: " + attributeValue.s());
                log.error("Error: " + e.getMessage());
                // Return empty array instead of throwing exception
                return new DateSlot[0];
        }
        catch (Exception e) {
            log.error("Unexpected error deserializing DateSlot array: " + e.getMessage());
            // Return empty array instead of throwing exception
            return new DateSlot[0];
        }
    }

    @Override
    public EnhancedType<DateSlot[]> type() {
        return EnhancedType.of(DateSlot[].class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }

}