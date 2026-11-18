package com.littlehorizons.erp.util;

/**
 * Thrown by InputReader when the user types the main menu word in any question. The main menu catches
 * it, so the user can leave a form at any point without saving anything.
 */
public class ReturnToMainMenuException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReturnToMainMenuException() {
        super("Volver al menú principal");
    }
}
