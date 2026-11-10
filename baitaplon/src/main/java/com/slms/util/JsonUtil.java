package com.slms.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lớp tiện ích xử lý chuyển đổi JSON sử dụng thư viện Gson.
 * Đã cấu hình adapter cho LocalDate và LocalDateTime.
 */
public final class JsonUtil {

    /** Định dạng ngày: yyyy-MM-dd */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    /** Định dạng ngày giờ: yyyy-MM-dd'T'HH:mm:ss */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Instance Gson đã được cấu hình sẵn (thread-safe) */
    private static final Gson GSON;

    static {
        GSON = new GsonBuilder()
                .setPrettyPrinting()

                // Adapter cho LocalDate
                .registerTypeAdapter(LocalDate.class,
                        (JsonSerializer<LocalDate>) (src, typeOfSrc, context) ->
                                new JsonPrimitive(src.format(DATE_FORMATTER)))
                .registerTypeAdapter(LocalDate.class,
                        (JsonDeserializer<LocalDate>) (json, typeOfT, context) ->
                                LocalDate.parse(json.getAsString(), DATE_FORMATTER))

                // Adapter cho LocalDateTime
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                                new JsonPrimitive(src.format(DATE_TIME_FORMATTER)))
                .registerTypeAdapter(LocalDateTime.class,
                        (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                                LocalDateTime.parse(json.getAsString(), DATE_TIME_FORMATTER))

                .create();
    }

    /**
     * Constructor private - không cho phép tạo instance.
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("Lớp tiện ích - không được tạo instance");
    }

    /**
     * Chuyển đổi đối tượng Java thành chuỗi JSON.
     *
     * @param object đối tượng cần chuyển đổi
     * @return chuỗi JSON
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Chuyển đổi chuỗi JSON thành đối tượng Java.
     *
     * @param json  chuỗi JSON
     * @param clazz lớp đích
     * @param <T>   kiểu đối tượng đích
     * @return đối tượng Java tương ứng
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }
}
