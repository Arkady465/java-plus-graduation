package ru.practicum.core.common.api.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import ru.practicum.core.common.dto.ApiError;
import ru.practicum.core.common.exception.NotFoundException;

import java.io.IOException;
import java.io.InputStream;

public class UserErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        ApiError apiError;

        try (InputStream bodyInputStream = response.body().asInputStream()) {
            apiError = objectMapper.readValue(bodyInputStream, ApiError.class);
        } catch (IOException ex) {
            return new RuntimeException(ex.getMessage());
        }

        return switch (response.status()) {
            case 404 -> new NotFoundException(apiError.getErrors());
            case 500 -> new RuntimeException("Server error occurred: " + apiError.getErrors());
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}