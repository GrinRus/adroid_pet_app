package ru.ryabov.pet.application.retrofit.services.interceptors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.http.HttpHeaders;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;

import static com.google.gson.JsonParser.parseString;
import static okhttp3.internal.platform.Platform.INFO;

public class PrettyLoggingInterceptor implements Interceptor {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private volatile Set<String> headersToRedact = Collections.emptySet();

    public enum Level {
        /**
         * No logs.
         */
        NONE,
        /**
         * Logs request and response lines.
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * }</pre>
         */
        BASIC,
        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        VERBOSE
    }

    public interface Logger {
        void log(String message);

        Logger DEFAULT = message -> Platform.get().log(INFO, message, null);
    }

    private final Logger logger;

    public PrettyLoggingInterceptor() {
        this(Logger.DEFAULT);
    }

    public PrettyLoggingInterceptor(Logger logger) {
        this.logger = logger;
    }

    private volatile Level level = Level.BASIC;

    public PrettyLoggingInterceptor setLevel(Level level) {
        if (level == null) throw new NullPointerException("level == null. Use Level.NONE instead.");
        this.level = level;
        return this;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Level level = this.level;
        Request request = chain.request();

        if (level == Level.NONE) {
            return chain.proceed(request);
        }


        boolean logEverything = level == Level.VERBOSE;

        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Buffer requestBuffer = new Buffer();

        if (logEverything) {
            Headers requestHeaders = request.headers();
            for (int i = 0, count = requestHeaders.size(); i < count; i++) {
                logHeader(requestBuffer, requestHeaders, i);
            }
        }

        String requestStartMessage = "--> "
                + request.method()
                + ' ' + request.url()
                + System.lineSeparator();
        if (hasRequestBody) {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)" + System.lineSeparator();
        }
        requestBuffer.writeString(requestStartMessage, UTF8);


        if (!logEverything || !hasRequestBody) {
            requestBuffer.writeUtf8("--> END " + request.method() + System.lineSeparator());
        } else if (requestBody.isDuplex()) {
            requestBuffer.writeUtf8("--> END " + request.method() + " (duplex request body omitted)" + System.lineSeparator());
        } else {
            try (Buffer buffer = new Buffer()) {

                try {
                    requestBody.writeTo(buffer);

                    JsonElement requestJson = parseString(buffer.readString(UTF8));
                    String prettyRequestJsonString = gson.toJson(requestJson);

                    requestBuffer.writeString(prettyRequestJsonString + System.lineSeparator(), UTF8);
                } catch (JsonSyntaxException ex) {
                    requestBody.writeTo(requestBuffer);
                }

                if (isPlaintext(requestBuffer)) {
                    requestBuffer.writeUtf8("--> END " + request.method()
                            + " (" + requestBody.contentLength() + "-byte body)" + System.lineSeparator());

                } else {
                    requestBuffer.writeUtf8("--> END " + request.method() + " (binary "
                            + requestBody.contentLength() + "-byte body omitted)" + System.lineSeparator());
                }
            }
        }

        logger.log(requestBuffer.readString(UTF8));


        Response response;
        Buffer responseBuffer = new Buffer();
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logger.log("<-- HTTP FAILED: " + e);
            throw e;
        }

        Headers responseHeaders = null;
        if (logEverything) {
            responseHeaders = response.headers();
            for (int i = 0, count = responseHeaders.size(); i < count; i++) {
                logHeader(responseBuffer, responseHeaders, i);
            }
        }

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        responseBuffer.writeString("<-- "
                        + response.code()
                        + (response.message().isEmpty() ? "" : ' ' + response.message())
                        + ' ' + response.request().url()
                        + System.lineSeparator(),
                UTF8);

        if (!logEverything || !HttpHeaders.hasBody(response)) {
            responseBuffer.writeUtf8("<-- END HTTP");
        } else {
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body.
            Buffer buffer = source.getBuffer();

            if ("gzip".equalsIgnoreCase(responseHeaders.get("Content-Encoding"))) {
                try (GzipSource gzippedResponseBody = new GzipSource(buffer.clone())) {
                    buffer = new Buffer();
                    buffer.writeAll(gzippedResponseBody);
                }
            }

            if (!isPlaintext(buffer)) {
                responseBuffer.writeUtf8("<-- END HTTP (binary " + buffer.size() + "-byte body omitted)" + System.lineSeparator());
                return response;
            }

            if (null != responseBody.contentType() && responseBody.contentType().type().equalsIgnoreCase("application/json")) {
                if (contentLength != 0) {
                    JsonElement responseJson = parseString(buffer.clone().readUtf8());
                    String prettyRequestJsonString = gson.toJson(responseJson);
                    responseBuffer.writeString(prettyRequestJsonString + System.lineSeparator(), UTF8);
                    responseBuffer.writeUtf8("<-- END HTTP (" + buffer.size() + "-byte body)" + System.lineSeparator());
                }
            } else {
                responseBuffer.writeUtf8(buffer.clone().readUtf8() + System.lineSeparator());
                responseBuffer.writeUtf8("<-- END HTTP (" + buffer.size() + "-byte body)" + System.lineSeparator());
            }
        }
        logger.log(responseBuffer.readUtf8());

        return response;
    }

    static boolean isPlaintext(Buffer buffer) {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    private void logHeader(Buffer buffer, Headers headers, int i) {
        String value = headersToRedact.contains(headers.name(i)) ? "██" : headers.value(i);
        buffer.writeString(headers.name(i) + ": " + value + System.lineSeparator(), UTF8);
    }
}