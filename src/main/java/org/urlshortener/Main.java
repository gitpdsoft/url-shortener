package org.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main implements RequestHandler<Map<String, Object>, Map<String, String>> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private S3Client s3Client = S3Client.builder().build();

    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        String body = input.get("body").toString();
        Map<String, String> bodyMap;
        try {
            bodyMap = objectMapper.readValue(body, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Error parsing JSON body: " + e.getMessage(), e);
        }

        String fromUrl = bodyMap.get("fromUrl");
        String expirationTime = bodyMap.get("expirationTime");

        String shortUrlId = UUID.randomUUID().toString().substring(0, 10);
        UrlData urlData = new UrlData(fromUrl, Long.parseLong(expirationTime));

        try {
            uploadJsonFile(objectMapper.writeValueAsString(urlData), shortUrlId);
        } catch (Exception e) {
            throw new RuntimeException("Error saving JSON to S3: " + e.getMessage(), e);
        }

        Map<String, String> response = new HashMap<>();
        response.put("code", shortUrlId);
        return response;
    }

    private void uploadJsonFile(String json, String fileName) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket("url-shortener-npd")
                .key(fileName + ".json")
                .build();
        s3Client.putObject(request, RequestBody.fromString(json));
    }
}