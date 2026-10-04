package com.pos.checkout.client;

import org.springframework.http.HttpStatusCode;

/** A downstream service answered with a client error (4xx) that checkout should surface to the POS. */
public class UpstreamException extends RuntimeException {

    private final HttpStatusCode status;

    public UpstreamException(String service, HttpStatusCode status, String body) {
        super(service + " rejected the request (" + status.value() + "): " + body);
        this.status = status;
    }

    public HttpStatusCode status() {
        return status;
    }
}
