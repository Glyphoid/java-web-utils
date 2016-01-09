package me.scai.network.webutils;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public final class JsonWebClient {
    /* Constants */
    private static final Gson GSON = new Gson();
    private static final JsonParser JSON_PARSER = new JsonParser();

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private final static int HTTP_STATUS_OK = 200;

    /* Member classes */
    public static class StatusException extends Exception {
        public StatusException(String msg) {
            super(msg);
        }
    }

    public static class BodyReadException extends Exception {
        public BodyReadException(String msg) {
            super(msg);
        }
    }

    public static class BodyParseException extends Exception {
        public BodyParseException(String msg) {
            super(msg);
        }
    }

    public static class AllAttemptsFailedException extends Exception {
        public AllAttemptsFailedException(String msg) {
            super(msg);
        }
    }

    /* Member variables */
    private static int maxNumHttpRequestRepeats = 3;

    /* Methods */
    public static JsonObject sendRequestAndGetResponseWithRepeats(GenericUrl url, String method, JsonObject bodyData)
            throws StatusException, BodyReadException, BodyParseException, AllAttemptsFailedException {
        int attemptCount = 0;

        while (attemptCount++ < maxNumHttpRequestRepeats) {
            try {
                return sendRequestAndGetResponse(url, method, bodyData);
            } catch (IOException ioExc) {}
        }

        throw new AllAttemptsFailedException("All " + maxNumHttpRequestRepeats + " attempt(s) to access endpoint " + url +
                " (Method=" + method + ")");
    }

    private static JsonObject sendRequestAndGetResponse(GenericUrl url, String method, JsonObject bodyData)
            throws IOException, StatusException, BodyReadException, BodyParseException {
        HttpContent content = ByteArrayContent.fromString("application/json", GSON.toJson(bodyData));

        /* Construct the HTTP request */
        HttpRequestFactory requestFactory = HTTP_TRANSPORT
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(JSON_FACTORY));
                    }
                });

        /* Send the request and retrieve the response */
        HttpResponse resp = null;
        try {
            HttpRequest req = requestFactory.buildRequest(method, url, content);
            resp = req.execute();
        } catch (IOException exc) {
            throw exc;
        }

        /* Examine the response status code */
        if (resp.getStatusCode() != HTTP_STATUS_OK) {
            throw new StatusException("Failed to get successful response due to status code: " + resp.getStatusCode() +
                    "(" + resp.getStatusMessage() + ")");
        }

        /* Read the body of the response */
        String respBody = null;
        try {
            InputStream is = resp.getContent();
            String isEncoding = resp.getContentEncoding();
            if (isEncoding == null) {
                isEncoding = "UTF-8";
            }

            respBody = IOUtils.toString(is, isEncoding);
            System.out.println(respBody);
        } catch (IOException exc) {
            throw new BodyReadException(
                    "IO exception occurred during body reading of the response, due to: " + exc.getMessage());
        }

        /* Parse the response JSON object */
        JsonObject respObj;
        try {
            respObj = JSON_PARSER.parse(respBody).getAsJsonObject();
        } catch (Exception exc) {
            throw new BodyParseException("Failed to parse the JSON response, due to: " + exc.getMessage());
        }

        return respObj;
    }
}
