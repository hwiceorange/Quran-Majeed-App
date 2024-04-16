/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.utils.exceptions;

public class NoInternetException extends RuntimeException {
    public NoInternetException() {
        this("No internet");
    }

    public NoInternetException(String msg) {
        super(msg);
    }
}
