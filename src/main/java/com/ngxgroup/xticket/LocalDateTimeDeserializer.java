package com.ngxgroup.xticket;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author briano
 */
public class LocalDateTimeDeserializer implements JsonDeserializer< LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        if (json.getAsJsonPrimitive().getAsString() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDateTime.parse(json.getAsJsonPrimitive().getAsString().trim(), formatter);
        } else {
            return null;
        }
    }
}
