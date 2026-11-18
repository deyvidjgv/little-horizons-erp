package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Module 6 of the main menu: inventory of resources, loans, returns and overdue loans. */
public class ResourceView implements MenuView {

    private static final int NAME_COLUMN_WIDTH = 36;
    private static final int CATEGORY_COLUMN_WIDTH = 22;
    private static final int LOCATION_COLUMN_WIDTH = 22;

    private final InputReader input;
    private final ResourceController controller;
    private final LoanController loanController;
    private final StudentSelector studentSelector;
    private final TeacherSelector teacherSelector;

    public ResourceView(InputReader input, ResourceController controller, LoanController loanController,
                        StudentController studentController, TeacherController teacherController) {
        this.input = input;
        this.controller = controller;
        this.loanController = loanController;
        this.studentSelector = new StudentSelector(input, studentController);
        this.teacherSelector = new TeacherSelector(input, teacherController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Gestión de Recursos");
            System.out.println("1. Registrar recurso");
            System.out.println("2. Listar inventario");
            System.out.println("3. Consultar recurso por código");
            System.out.println("4. Actualizar recurso");
            System.out.println("5. Eliminar recurso");
            System.out.println("6. Prestar recurso");
            System.out.println("7. Registrar devolución");
            System.out.println("8. Reporte de préstamos vencidos");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 8);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> list();
                    case 3 -> showResource();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> lend();
                    case 7 -> registerReturn();
                    case 8 -> overdueReport();
                    default -> {
                    }
                }
            } catch (DataAccessException e) {
                Screen.error(e.getMessage());
            }
        } while (option != 0);
        return Navigation.BACK;
    }

    /** Also used by Reportes y Estadísticas. */
    void overdueReport() throws DataAccessException {
        List<Loan> overdue = loanController.overdueLoans();
        LocalDate today = LocalDate.now();
        Screen.subtitle("Préstamos vencidos al " + Formats.date(today));
        if (overdue.isEmpty()) {
            System.out.println("No hay préstamos vencidos.");
            return;
        }
        String format = "%-34s %-11s %-30s %-12s %s%n";
        System.out.printf(format, "Recurso", "Tipo", "Lo tiene", "Debía volver", "Días de retraso");
        overdue.forEach(loan -> System.out.printf(format, Screen.fit(loan.getResource().getDisplayName(), 34),
                loan.getBorrowerType().getLabel(), Screen.fit(loan.getBorrower().getFullName(), 30),
                Formats.date(loan.getDueDate()), loan.getDaysOverdue(today)));
        System.out.println("Total: " + overdue.size() + " préstamo(s) vencido(s).");
    }

    private void register() throws DataAccessException {
        Screen.subtitle("Registrar recurso");
        Resource resource = new Resource();
        resource.setCode(readCode(null));
        if (controller.findByCode(resource.getCode()).isPresent()) {
            Screen.error("Ya existe un recurso con el código " + resource.getCode() + ".");
            return;
        }
        readResourceData(resource, null);
        Screen.result(controller.register(resource));
    }

    private void list() throws DataAccessException {
        List<ResourceCategory> filters = new ArrayList<>();
        filters.add(null);
        filters.addAll(controller.listCategories());
        ResourceCategory category = input.readChoice("Categoría", filters,
                filter -> filter == null ? "Todas" : filter.getName(), null);
        List<Resource> resources = controller.list(category == null ? 0 : category.getId());
        if (resources.isEmpty()) {
            System.out.println("No hay recursos registrados.");
            return;
        }
        String format = "%-" + NAME_COLUMN_WIDTH + "s %-" + CATEGORY_COLUMN_WIDTH + "s %-" + LOCATION_COLUMN_WIDTH
                + "s %s%n";
        System.out.printf(format, "Recurso", "Categoría", "Ubicación", "Estado");
        resources.forEach(resource -> System.out.printf(format, Screen.fit(resource.getDisplayName(), NAME_COLUMN_WIDTH),
                Screen.fit(resource.getCategory().getName(), CATEGORY_COLUMN_WIDTH),
                Screen.fit(Formats.text(resource.getLocation()), LOCATION_COLUMN_WIDTH),
                resource.getStatus().getDatabaseValue()));
        System.out.println("Total: " + resources.size() + " recurso(s).");
    }

    private void showResource() throws DataAccessException {
        Optional<Resource> found = askResource();
        if (found.isEmpty()) {
            return;
        }
        Resource resource = found.get();
        Screen.subtitle(resource.getDisplayName());
        System.out.println("Categoría: " + resource.getCategory().getName());
        System.out.println("Descripción: " + Formats.text(resource.getDescription()));
        System.out.println("Fecha de adquisición: " + Formats.date(resource.getAcquisitionDate()));
        System.out.println("Ubicación física: " + Formats.text(resource.getLocation()));
        System.out.println("Estado: " + resource.getStatus().getDatabaseValue());
        Optional<Loan> loan = loanController.findOpenLoan(resource.getId());
        loan.ifPresent(open -> System.out.println("Préstamo: " + open.getBorrowerType().getLabel() + " "
                + open.getBorrower().getFullName() + ", desde el " + Formats.date(open.getLoanDate())
                + " hasta el " + Formats.date(open.getDueDate()) + " (" + open.getStatus().getDatabaseValue() + ")"));
    }

    private void update() throws DataAccessException {
        Optional<Resource> found = askResource();
        if (found.isEmpty()) {
            return;
        }
        Resource current = found.get();
        Screen.subtitle("Actualizar " + current.getDisplayName());
        PersonForm.printEditHint();
        Resource edited = new Resource();
        edited.setId(current.getId());
        edited.setCode(readCode(current.getCode()));
        readResourceData(edited, current);
        edited.setStatus(input.readChoice("Estado", List.of(ResourceStatus.values()),
                ResourceStatus::getDatabaseValue, current.getStatus()));
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<Resource> found = askResource();
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar " + found.get().getDisplayName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    private void lend() throws DataAccessException {
        Optional<Resource> resource = askResource();
        if (resource.isEmpty()) {
            return;
        }
        if (resource.get().getStatus() != ResourceStatus.AVAILABLE) {
            Screen.error(resource.get().getDisplayName() + " no está disponible (estado: "
                    + resource.get().getStatus().getDatabaseValue() + ").");
            return;
        }
        BorrowerType type = input.readChoice("¿Quién lo solicita?", List.of(BorrowerType.values()),
                BorrowerType::getLabel, null);
        Optional<? extends Person> borrower = type == BorrowerType.STUDENT ? studentSelector.ask() : teacherSelector.ask();
        if (borrower.isEmpty()) {
            return;
        }
        Loan loan = new Loan();
        loan.setResource(resource.get());
        loan.setBorrowerType(type);
        loan.setBorrower(borrower.get());
        loan.setLoanDate(input.readDate("Fecha del préstamo", true, LocalDate.now()));
        loan.setDueDate(input.readDate("Fecha de devolución programada", true, null));
        loan.setNotes(input.readOptionalText("Observaciones (opcional)", Loan.NOTES_MAX_LENGTH));
        Screen.result(loanController.lend(loan));
    }

    private void registerReturn() throws DataAccessException {
        Optional<Resource> resource = askResource();
        if (resource.isEmpty()) {
            return;
        }
        Optional<Loan> loan = loanController.findOpenLoan(resource.get().getId());
        if (loan.isEmpty()) {
            Screen.error(resource.get().getDisplayName() + " no tiene un préstamo pendiente de devolución.");
            return;
        }
        System.out.println("Lo tiene: " + loan.get().getBorrowerType().getLabel() + " "
                + loan.get().getBorrower().getFullName() + ", debía volver el " + Formats.date(loan.get().getDueDate()));
        LocalDate returnDate = input.readDate("Fecha real de devolución", true, LocalDate.now());
        Screen.result(loanController.registerReturn(resource.get().getId(), returnDate));
    }

    private Optional<Resource> askResource() throws DataAccessException {
        String code = input.readText("Código del recurso (ej: LIB-001)", Resource.CODE_MAX_LENGTH);
        Optional<Resource> resource = controller.findByCode(code);
        if (resource.isEmpty()) {
            Screen.error("No existe un recurso con el código " + ResourceController.normalizeCode(code) + ".");
        }
        return resource;
    }

    private String readCode(String current) {
        String code = input.readText("Código (ej: LIB-001)", Resource.CODE_MAX_LENGTH, true,
                value -> ResourceController.isValidCode(ResourceController.normalizeCode(value)),
                ResourceController.CODE_RULE, current);
        return ResourceController.normalizeCode(code);
    }

    /** @param current resource being edited, or null when it is new */
    private void readResourceData(Resource resource, Resource current) throws DataAccessException {
        resource.setName(input.readText("Nombre", Resource.NAME_MAX_LENGTH, true, null, null,
                current == null ? null : current.getName()));
        List<ResourceCategory> categories = controller.listCategories();
        ResourceCategory currentCategory = current == null ? null : categories.stream()
                .filter(category -> category.getId() == current.getCategory().getId())
                .findFirst().orElse(null);
        resource.setCategory(input.readChoice("Categoría", categories, ResourceCategory::getName, currentCategory));
        resource.setDescription(input.readText("Descripción (opcional)", Resource.DESCRIPTION_MAX_LENGTH, false, null,
                null, current == null ? null : current.getDescription()));
        resource.setAcquisitionDate(input.readDate("Fecha de adquisición (opcional)", false,
                current == null ? null : current.getAcquisitionDate()));
        resource.setLocation(input.readText("Ubicación física (ej: Biblioteca)", Resource.LOCATION_MAX_LENGTH, false,
                null, null, current == null ? null : current.getLocation()));
    }
}
