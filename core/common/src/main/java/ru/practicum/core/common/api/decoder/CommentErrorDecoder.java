package ru.practicum.core.common.api.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;

import ru.practicum.core.common.dto.ApiError;

import java.io.IOException;
import java.io.InputStream;

public class CommentErrorDecoder implements ErrorDecoder {
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

        if (response.status() == 500) {
            return new RuntimeException("Server error occurred: " + apiError.getErrors());
        } else {
            return defaultDecoder.decode(methodKey, response);
        }
    }
}