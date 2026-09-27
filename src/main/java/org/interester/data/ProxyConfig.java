package org.interester.data;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

record ProxyConfig(String host, int port, String user, String password) {

    static Optional<ProxyConfig> fromEnv(Map<String, String> env) {
        String value = firstNonBlank(env.get("HTTPS_PROXY"), env.get("https_proxy"),
                env.get("ALL_PROXY"), env.get("all_proxy"));
        if (value == null) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(value.contains("://") ? value : "http://" + value);
            if (uri.getHost() == null) {
                return Optional.empty();
            }
            int port = uri.getPort() > 0 ? uri.getPort() : 80;
            String user = null;
            String password = null;
            if (uri.getRawUserInfo() != null) {
                String[] parts = uri.getRawUserInfo().split(":", 2);
                user = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                password = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            }
            return Optional.of(new ProxyConfig(uri.getHost(), port, user, password));
        } catch (IllegalArgumentException e) {
            System.err.println("interester: ignoring invalid proxy setting");
            return Optional.empty();
        }
    }

    ProxySelector selector() {
        return ProxySelector.of(new InetSocketAddress(host, port));
    }

    Optional<Authenticator> authenticator() {
        if (user == null) {
            return Optional.empty();
        }
        allowBasicAuthForHttpsTunnels();
        return Optional.of(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() != RequestorType.PROXY) {
                    return null;
                }
                return new PasswordAuthentication(user, password.toCharArray());
            }
        });
    }

    @Override
    public String toString() {
        return "ProxyConfig[" + host + ":" + port + (user == null ? "" : ", with credentials") + "]";
    }

    private static void allowBasicAuthForHttpsTunnels() {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v.strip();
            }
        }
        return null;
    }
}
