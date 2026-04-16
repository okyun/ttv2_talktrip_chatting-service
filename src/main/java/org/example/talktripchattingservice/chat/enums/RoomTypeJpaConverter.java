package org.example.talktripchattingservice.chat.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RoomTypeJpaConverter implements AttributeConverter<RoomType, String> {

    @Override
    public String convertToDatabaseColumn(RoomType attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RoomType convertToEntityAttribute(String db) {
        if (db == null || db.isBlank()) {
            return null;
        }
        return RoomType.fromStoredValue(db);
    }
}
