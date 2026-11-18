package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.InputReader;

/** Module 4 of the main menu: clubs and events. */
public class ExtracurricularView implements MenuView {

    private final InputReader input;
    private final ClubView clubView;
    private final EventView eventView;

    public ExtracurricularView(InputReader input, ClubView clubView, EventView eventView) {
        this.input = input;
        this.clubView = clubView;
        this.eventView = eventView;
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Actividades Extracurriculares");
            System.out.println("1. Clubes");
            System.out.println("2. Eventos");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 2);
            MenuView submenu = switch (option) {
                case 1 -> clubView;
                case 2 -> eventView;
                default -> null;
            };
            if (submenu != null && submenu.show() == Navigation.MAIN_MENU) {
                return Navigation.MAIN_MENU;
            }
        } while (option != 0);
        return Navigation.BACK;
    }
}
