package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.ReturnToMainMenuException;

import java.util.Map;
import java.util.NoSuchElementException;

/** Main menu: groups the options by module and opens the menu of each module. */
public class MainMenuView {

    private static final int EXIT_OPTION = 8;

    private final InputReader input;
    private final Map<Integer, MenuView> modules;

    /** @param modules menu of each module, by its option number */
    public MainMenuView(InputReader input, Map<Integer, MenuView> modules) {
        this.input = input;
        this.modules = modules;
    }

    public void show() {
        while (true) {
            printMenu();
            int option;
            try {
                option = input.readInt("Seleccione una opción: ", 1, EXIT_OPTION);
            } catch (ReturnToMainMenuException e) {
                continue; // already in the main menu
            }
            if (option == EXIT_OPTION) {
                System.out.println("Hasta luego.");
                return;
            }
            openModule(option);
        }
    }

    /** An unexpected error is logged and the user comes back to this menu instead of closing the program. */
    private void openModule(int option) {
        MenuView module = modules.get(option);
        if (module == null) {
            System.out.println("Este módulo aún no está disponible.");
            return;
        }
        try {
            module.show();
        } catch (NoSuchElementException e) {
            throw e; // the input was closed: Main ends the program
        } catch (ReturnToMainMenuException e) {
            System.out.println("Operación cancelada. Volviendo al menú principal.");
        } catch (RuntimeException e) {
            ErrorLogger.log("MainMenuView.openModule", e);
            Screen.error("Ocurrió un error inesperado. El detalle quedó guardado en " + ErrorLogger.LOG_FILE + ".");
        }
    }

    private void printMenu() {
        Screen.box("   ERP Colegio Little Horizons", "   Sistema de Gestión Educativa");
        System.out.println();
        System.out.println("1. Gestión de Estudiantes");
        System.out.println("2. Gestión Académica");
        System.out.println("3. Gestión de Profesores");
        System.out.println("4. Actividades Extracurriculares");
        System.out.println("5. Gestión Financiera");
        System.out.println("6. Gestión de Recursos");
        System.out.println("7. Reportes y Estadísticas");
        System.out.println("8. Salir");
        System.out.println();
        System.out.println("En cualquier pregunta escriba '" + InputReader.MAIN_MENU_WORD
                + "' para cancelar y volver a este menú.");
    }
}
