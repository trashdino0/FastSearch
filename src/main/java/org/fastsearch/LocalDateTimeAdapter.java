package org.fastsearch;

import com.google.gson.TypeAdapter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


// ============================================
// GSON ADAPTER FOR LocalDateTime
// ============================================
class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.format(formatter));
        }
    }

    @Override
    public LocalDateTime read(com.google.gson.stream.JsonReader in) throws IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return LocalDateTime.parse(in.nextString(), formatter);
    }
}