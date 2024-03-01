

package com.quran.quranaudio.online.quran_module.utils.exceptions;

public class HttpNotFoundException extends RuntimeException {
    public HttpNotFoundException() {
        this("Not found");
    }

    public HttpNotFoundException(String msg) {
        super(msg);
    }
}
