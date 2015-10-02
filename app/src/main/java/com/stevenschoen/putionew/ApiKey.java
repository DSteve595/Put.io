package com.stevenschoen.putionew;

public class ApiKey {
    // Remove the surrounding comments after adding your key.
    private static final String API_KEY /* = "API_KEY_HERE */;
    private static final String CLIENT_ID /* = "CLIENT_ID_HERE */;

    public static String getApiKey() {
        if (API_KEY == null) {
            throw new NullPointerException("Secret key not set. Get one at put.io and place it in ApiKey.java");
        }
        return API_KEY;
    }

    public static String getClientId() {
        if (CLIENT_ID == null) {
            throw new NullPointerException("Client ID not set. Get one at put.io and place it in ApiKey.java");
        }
        return CLIENT_ID;
    }

}
