package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.OperationResult;

/** Console output shared by the views: boxed titles and the success and error messages. */
final class Screen {

    private static final int BOX_WIDTH = 46;

    private Screen() {
    }

    static void box(String... lines) {
        System.out.println();
        System.out.println("╔" + "═".repeat(BOX_WIDTH) + "╗");
        for (String line : lines) {
            System.out.printf("║%-" + BOX_WIDTH + "s║%n", line);
        }
        System.out.println("╚" + "═".repeat(BOX_WIDTH) + "╝");
    }

    static void title(String text) {
        box("   " + text);
    }

    static void subtitle(String text) {
        System.out.println();
        System.out.println("--- " + text + " ---");
    }

    static void result(OperationResult result) {
        if (result.success()) {
            success(result.message());
        } else {
            error(result.message());
        }
    }

    static void success(String message) {
        System.out.println("[OK] " + message);
    }

    static void error(String message) {
        System.out.println("[ERROR] " + message);
    }

    /** Cuts a text so it fits in a table column. */
    static String fit(String text, int width) {
        if (text == null) {
            return "-";
        }
        return text.length() <= width ? text : text.substring(0, width - 3) + "...";
    }
}
