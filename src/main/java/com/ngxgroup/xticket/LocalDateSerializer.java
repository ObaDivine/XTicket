package com.ngxgroup.xticket;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 *
 * @author briano
 */
public class LocalDateSerializer implements JsonSerializer <LocalDate> { 
    @Override
    public JsonElement serialize(LocalDate localDate, Type type, JsonSerializationContext context) {
        if (localDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return new JsonPrimitive(formatter.format(localDate));
        } else {
            return null;
        }
    }
   
}