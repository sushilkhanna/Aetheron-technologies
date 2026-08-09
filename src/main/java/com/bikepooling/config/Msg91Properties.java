package com.bikepooling.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "msg91")
public class Msg91Properties {

    private Auth auth = new Auth();
    private Sender sender = new Sender();

    public static class Auth {
        private String key;
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }

    public static class Sender {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }
    public Sender getSender() { return sender; }
    public void setSender(Sender sender) { this.sender = sender; }
}