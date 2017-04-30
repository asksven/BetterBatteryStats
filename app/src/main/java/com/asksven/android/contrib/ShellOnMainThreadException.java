/**
 * Contrib by Chainfire (see https://raw.github.com/Chainfire/libsuperuser/master/libsuperuser/src/eu/chainfire/libsuperuser/Shell.java)
 */
package com.asksven.android.contrib;
/**
 * Exception class used to crash application when shell commands are executed
 * from the main thread, and we are in debug mode. 
 */
@SuppressWarnings("serial")
public class ShellOnMainThreadException extends RuntimeException {
    public static final String EXCEPTION_COMMAND = "Application attempted to run a shell command from the main thread";
    public static final String EXCEPTION_NOT_IDLE = "Application attempted to wait for a non-idle shell to close on the main thread";
    public static final String EXCEPTION_WAIT_IDLE = "Application attempted to wait for a shell to become idle on the main thread";

    public ShellOnMainThreadException(String message) {
        super(message);
    }
}