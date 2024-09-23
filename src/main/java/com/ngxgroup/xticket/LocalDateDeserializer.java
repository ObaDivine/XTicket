package com.ngxgroup.xticket;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 *
 * @author briano
 */
public class LocalDateDeserializer implements JsonDeserializer< LocalDate> {

    @Override
    public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        return LocalDate.parse(json.getAsString(), DateTimeFormatter.ofPattern("yyyy-mm-dd").withLocale(Locale.ENGLISH));
    }
}
