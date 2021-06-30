package ru.ryabov.pet.application.services;

import com.google.gson.GsonBuilder;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.ryabov.pet.application.services.interceptors.PrettyLoggingInterceptor;

import static ru.ryabov.pet.application.services.interceptors.PrettyLoggingInterceptor.Level.VERBOSE;

@Slf4j
public class ServiceManager {

    private List<Interceptor> interceptors = new ArrayList<>();

    public ServiceManager addInterceptor(Interceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public ServiceManager resetInterceptors() {
        this.interceptors = new ArrayList<>();
        return this;
    }

    private Retrofit retrofit(String baseUrl, OkHttpClient.Builder httpClientBuilder) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClientBuilder
                        .followRedirects(false)
                        .addInterceptor(new PrettyLoggingInterceptor(log::info).setLevel(VERBOSE))
                        .build())
                .addConverterFactory(GsonConverterFactory.create(
                        new GsonBuilder()

                                .serializeNulls()
                                .setLenient()
                                .create()))
                .build();
    }


    @SneakyThrows
    public <S> S createService(Class<S> serviceClass, String baseUrl) {

        OkHttpClient.Builder httpClientBuilder = getUnsafeOkHttpClientBuilder();

        for (Interceptor interceptor : this.interceptors)
            httpClientBuilder.addInterceptor(interceptor);

        return retrofit(baseUrl, httpClientBuilder).create(serviceClass);
    }

    private OkHttpClient.Builder getUnsafeOkHttpClientBuilder() throws NoSuchAlgorithmException, KeyManagementException {
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                   String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[0];
                    }
                }
        };

        HostnameVerifier hostnameVerifier = (hostname, session) -> true;

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                .hostnameVerifier(hostnameVerifier)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS);
    }
}
