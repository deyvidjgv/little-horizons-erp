package com.littlehorizons.erp.view;

/** Menu of a module. The main menu only knows this interface, so a new module does not change it. */
public interface MenuView {

    /** Shows the menu until the user leaves it. */
    Navigation show();
}
