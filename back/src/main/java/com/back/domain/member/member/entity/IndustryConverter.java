package com.back.domain.member.member.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class IndustryConverter implements AttributeConverter<Industry, String> {
    @Override
    public String convertToDatabaseColumn(Industry attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public Industry convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Industry.fromLabel(dbData);
    }
}