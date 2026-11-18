# ERP Colegio Little Horizons

## 1. Título del Proyecto

**ERP Colegio Little Horizons — Sistema de Gestión Educativa**

## 2. Descripción

### Propósito

Aplicación de consola en Java que centraliza los procesos académicos, administrativos y financieros
del Colegio Internacional "Little Horizons" (de Preescolar a grado 11°) en una base de datos MySQL.

### Problemas que resuelve

| Problema del colegio | Cómo lo resuelve el sistema |
|---|---|
| Notas en cuadernos que se pierden | Registro de notas por materia y período, con historial desde el ingreso |
| Clubes y eventos sin registro central | Catálogo de clubes con cupo, membresías, eventos y participantes |
| Facturación manual con errores | Registro de pagos y factura electrónica generada automáticamente |
| Libros y equipos que se "pierden" | Inventario de recursos y préstamos a estudiantes y profesores |
| Profesores sin visibilidad del rendimiento | Promedios automáticos, reporte de inasistencias y de notas bajas |

### Funcionalidades principales

- Estudiantes, acudientes y matrículas por grado y año.
- Profesores, asignación de materias y horarios sin cruces.
- Inscripción en materias, notas (0.0 a 5.0), promedios y asistencia.
- Clubes con cupo máximo y eventos con posiciones en competencias.
- Conceptos de pago, pagos, historial, morosos y factura electrónica.
- Inventario de recursos, préstamos, devoluciones y préstamos vencidos.
- Reportes y estadísticas.

## 3. Autores

- **Nombre completo:** Deyvid Godoy
- **Correo electrónico:** deyvidjgv12@gmail.com
- **Contacto:** GitHub [deyvidjgv](https://github.com/deyvidjgv)

## 4. Tecnologías Utilizadas

| Tipo | Tecnología |
|---|---|
| Lenguaje | Java 21 (aplicación Java estándar, sin Maven ni Gradle) |
| IDE | IntelliJ IDEA |
| Base de datos | MySQL 8.0 |
| Librerías externas | MySQL Connector/J 9.3.0 (`lib/mysql-connector-j-9.3.0.jar`) |

El resto usa solo la biblioteca estándar de Java: JDBC (`java.sql`), `java.time`, Stream API y
`java.security.MessageDigest` para el CUFE simulado.

## 5. Requerimientos Previos

- **JDK 21 o superior.**
- **MySQL 8.0 o superior (obligatorio):** el script usa restricciones `CHECK`, que MySQL solo aplica desde la versión 8.0.
- IntelliJ IDEA (Community o Ultimate).
- Git, para clonar el repositorio.

## 6. Configuración de la Base de Datos

### 6.1. Creación de la base de datos

El archivo `docs/database.sql` tiene dos secciones:

- **Sección 1 — DDL:** crea la base `erp_little_horizons` y todas sus tablas, con llaves primarias,
  llaves foráneas, índices y checks. Está basada en el script del anexo; las dos tablas agregadas
  (`horarios_asignacion` y `valores_concepto_estudiante`) están justificadas dentro del script.
- **Sección 2 — DML:** datos de prueba coherentes (más de 30 registros en cada tabla principal).

Desde la carpeta del proyecto (PowerShell):

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p -e "source docs/database.sql"
```

> El script empieza con `DROP DATABASE IF EXISTS erp_little_horizons`: si la base ya existe, la borra y la crea de nuevo.

### 6.2. Configuración de credenciales

Las credenciales no se suben al repositorio. Copie el archivo de ejemplo y escriba su usuario y clave de MySQL:

```powershell
Copy-Item db.properties.example db.properties
```

```properties
db.url=jdbc:mysql://localhost:3306/erp_little_horizons
db.user=root
db.password=su_clave
```

### 6.3. Instalación del driver JDBC

El driver ya viene en `lib/mysql-connector-j-9.3.0.jar`. En IntelliJ IDEA:
**File > Project Structure > Libraries > + > Java** y elegir ese archivo.

Si lo prefiere, puede descargar la misma versión desde https://dev.mysql.com/downloads/connector/j/
(Platform Independent) y copiar el `.jar` en la carpeta `lib/`.

## 7. Instalación y Ejecución

### 7.1. Clonar el repositorio

```powershell
git clone https://github.com/deyvidjgv/little-horizons-erp.git
cd little-horizons-erp
```

### 7.2. Abrir el proyecto en el IDE

En IntelliJ IDEA: **File > Open** y elegir la carpeta `little-horizons-erp`. Verifique que `src` esté
marcada como carpeta de fuentes, que el SDK sea Java 21 o superior y que la librería del driver esté
agregada (paso 6.3).

### 7.3. Compilar

En IntelliJ: **Build > Build Project**. Sin IDE (PowerShell, desde la carpeta del proyecto):

```powershell
$fuentes = Get-ChildItem src -Recurse -Filter *.java | ForEach-Object FullName
javac --release 21 -encoding UTF-8 -cp "lib/*" -d out $fuentes
```

### 7.4. Ejecutar

En IntelliJ: ejecutar la clase `com.littlehorizons.erp.Main`, con la carpeta del proyecto como
directorio de trabajo (la predeterminada). Sin IDE:

```powershell
java -cp "out;lib/*" com.littlehorizons.erp.Main
```

El programa lee `db.properties` y escribe `error_log.txt` y la carpeta `facturas/` en el directorio
desde el que se ejecuta.

## 8. Estructura del Proyecto

### Arquitectura MVC

```
Vista (view)  →  Controlador (controller)  →  DAO (dao)  →  MySQL
  lee y muestra     reglas de negocio          SQL con PreparedStatement
```

- La **vista** solo pide datos al usuario y muestra resultados; no conoce SQL ni reglas.
- El **controlador** valida las reglas del negocio y devuelve un `OperationResult` (éxito o error con su mensaje).
- El **DAO** es el único que habla con la base de datos.
- El **modelo** son las entidades que viajan entre las capas.

### Paquetes

```
src/com/littlehorizons/erp/
├── model/        Entidades del dominio (Student, Teacher, Subject, Payment, Loan...) y enums de los valores ENUM de la base
├── view/         Menús de consola de cada módulo (StudentView, AcademicView, FinanceView...)
├── controller/   Lógica de negocio y validaciones (StudentController, PaymentController...)
├── dao/          Acceso a datos con JDBC (BaseDAO y un DAO por tabla o grupo de tablas)
├── service/      Generación de la factura electrónica (InvoiceService) y datos del colegio (SchoolInfo)
├── util/         Utilidades compartidas: conexión, registro de errores, lectura de consola, formatos y validaciones
└── Main.java     Punto de entrada: verifica la conexión, arma los módulos y abre el menú principal
```

Otras carpetas: `docs/` (script de la base de datos), `lib/` (driver JDBC), `facturas/` (se crea al
generar la primera factura).

## 9. Principios y Patrones de Diseño Aplicados

### 9.1. Principios SOLID

- **S — Responsabilidad única:** cada clase tiene un solo trabajo. `StudentDAO` solo ejecuta el SQL de
  estudiantes, `StudentController` solo aplica sus reglas, `StudentView` solo conversa con el usuario e
  `InvoiceService` solo arma y guarda la factura.
- **O — Abierto/cerrado:** `MainMenuView` recibe un `Map<Integer, MenuView>` con los módulos; agregar un
  módulo no cambia el menú principal. `BaseDAO` tampoco cambia cuando aparece una tabla nueva: cada DAO
  le pasa su SQL y su mapeo como lambdas.
- **L — Sustitución de Liskov:** `Student`, `Guardian` y `Teacher` heredan de `Person`. Los métodos que
  reciben un `Person` (por ejemplo, `BaseDAO.fillIdentity` o `PersonForm.readNames`) funcionan igual con
  cualquiera de los tres. Las vistas de los módulos se usan a través de la interfaz `MenuView`.
- **I — Segregación de interfaces:** las interfaces son pequeñas y específicas: `MenuView` (un método
  `show()`), `DatabaseValue` (un método `getDatabaseValue()`) y las interfaces funcionales de `BaseDAO`
  (`ParameterSetter`, `RowMapper`, `TransactionWork`, `ItemSetter`).
- **D — Inversión de dependencias:** las clases reciben sus dependencias por el constructor en lugar de
  crearlas (por ejemplo, `PaymentController` recibe sus DAO e `InvoiceService`). `Main` es el único lugar
  donde se crean y se conectan.

### 9.2. Patrones de Diseño

| Patrón | Dónde | Justificación |
|---|---|---|
| MVC | Paquetes `model`, `view` y `controller` | Separar lo que se muestra, las reglas y los datos, como pide el proyecto |
| DAO | Paquete `dao` | Aislar todo el SQL: el resto del programa no conoce `java.sql` |
| Singleton | `util.DatabaseConnection` | Leer `db.properties` una sola vez y tener un único punto de acceso a la base. Cada operación abre su propia conexión y la cierra con try-with-resources |
| Template Method | `dao.BaseDAO` | Los pasos fijos de JDBC (abrir, preparar, ejecutar, cerrar, transacción) están en un solo lugar; cada DAO aporta solo su SQL y su mapeo |
| Inyección de dependencias por constructor | Controladores y vistas | Clases desacopladas y fáciles de cambiar |

## 10. Funcionalidades Principales

### 1. Gestión de Estudiantes
- Registro completo del estudiante (datos personales, documento, fecha de nacimiento, dirección, salud) con su primer acudiente.
- Ficha con acudientes, contactos de emergencia e historial de matrículas; búsqueda por nombre y lista por estado.
- Acudientes: registro, consulta y vínculo con varios estudiantes (hermanos); cada estudiante conserva al menos uno.
- Matrículas por grado y año; retiro (estado Retirado) y grado (estado Graduado).

### 2. Gestión Académica
- Catálogo de materias por grado.
- Inscripción por período de un estudiante o de todo un grado en las materias de su grado.
- Notas de 0.0 a 5.0 por materia y período (I, II, III, IV o Final), registradas por el profesor del grupo.
- Cálculo automático del promedio por materia, del promedio general del estudiante en el período y del promedio del curso en cada materia.
- Historial académico desde el ingreso.
- Asistencia diaria por materia (Presente, Ausente, Tardanza, Excusa Médica) y reporte de inasistencias por semana.

### 3. Gestión de Profesores
- Directorio de profesores con documento, especialización y contacto.
- Asignación de materias por año y sección (una materia puede tener varios profesores, uno por grupo).
- Horarios de clase por día y hora, sin cruces para un mismo profesor.

### 4. Actividades Extracurriculares
- Clubes con descripción, cupo máximo y profesor encargado; la inscripción no supera el cupo.
- Membresías con fecha de ingreso y estado (Activa o Retirada).
- Calendario de eventos por tipo (Académico, Cultural, Deportivo, Social, Científico), participantes y posición (1er, 2do o 3er Lugar, o Participante) en los eventos con competencia.

### 5. Gestión Financiera
- Catálogo de conceptos de pago con valor base y valores especiales por estudiante (descuentos, becas).
- Registro de pagos con recibo consecutivo; historial por estudiante y por acudiente.
- Reporte de morosos: estudiantes con matrícula activa cuyo último pago de un concepto mensual tiene más de un mes.
- Factura electrónica generada al registrar cada pago (ver abajo).

**Factura electrónica.** Al confirmar un pago:
1. se guardan el pago y la factura con su número consecutivo;
2. se leen desde la base todos los datos de la factura;
3. se calcula el CUFE simulado (hash SHA-384 del número de factura + NIT + fecha de emisión);
4. se genera el archivo `facturas/factura_000123.txt` con los datos del colegio, el cliente (acudiente
   que paga), el detalle, el subtotal, el IVA, el total, el CUFE, un código QR simulado y la resolución
   de habilitación simulada;
5. se muestra la ruta donde quedó guardado.

### 6. Gestión de Recursos
- Inventario con código único, nombre, categoría, estado y ubicación física.
- Préstamos a estudiantes y a profesores (fecha de préstamo, fecha programada y estado).
- Devolución: registra la fecha real y deja el recurso Disponible.
- Reporte de préstamos vencidos.

### 7. Reportes y Estadísticas
- Inasistencias de una semana, promedio del curso por materia, morosos, préstamos vencidos y participación en eventos.

### En todos los módulos
- Cada dato se valida al escribirlo; si es inválido se muestra el motivo y se vuelve a pedir.
- Cada operación muestra `[OK]` o `[ERROR]` con un mensaje claro.
- Cada submenú tiene `0` para volver al menú anterior y los submenús internos tienen `9` para ir al menú principal.
- En cualquier pregunta, también en mitad de un formulario, se puede escribir `menu` para cancelar sin guardar y volver al menú principal.

## 11. Manejo de Errores y Logs

### Sistema de logging

La clase `util.ErrorLogger` escribe cada error en el archivo **`error_log.txt`**, con fecha y hora,
el lugar donde ocurrió, el mensaje y la traza completa. El archivo se abre en modo agregar, así que
conserva los errores de ejecuciones anteriores.

```
[2026-11-17 18:57:15] ERROR in StudentDAO - No se pudo registrar el estudiante.: Duplicate entry ...
java.sql.SQLIntegrityConstraintViolationException: Duplicate entry ...
	at ...
```

### Ubicación del archivo de logs

`error_log.txt` se crea en el directorio de trabajo del programa (la carpeta del proyecto al ejecutarlo
desde IntelliJ o con los comandos de la sección 7). No se sube al repositorio.

### Estrategia de manejo de excepciones

1. **Entradas del usuario:** `InputReader` valida el formato (números, fechas, textos, correos...) y vuelve a preguntar; estos errores no se registran en el log.
2. **Reglas de negocio:** los controladores devuelven un `OperationResult` con el motivo del rechazo (por ejemplo, "El club no tiene cupo").
3. **Base de datos:** cada `SQLException` se captura en el DAO, se registra en `error_log.txt` y se convierte en una `DataAccessException` con un mensaje claro, que la vista muestra sin cerrar el programa.
4. **Recursos:** todas las conexiones, `PreparedStatement` y `ResultSet` se cierran con try-with-resources. Las operaciones que tocan varias tablas (matrícula, pago con factura, préstamo, devolución...) usan transacciones con `rollback` si algo falla.
5. **Errores inesperados:** el menú principal captura cualquier otra excepción, la registra en el log y vuelve al menú en lugar de terminar el programa.
6. **Archivos:** si no se puede escribir la factura, el pago queda registrado, el error se registra y se informa al usuario.

## 12. Capturas de Pantalla

Menú principal:

```
╔══════════════════════════════════════════════╗
║   ERP Colegio Little Horizons                ║
║   Sistema de Gestión Educativa               ║
╚══════════════════════════════════════════════╝

1. Gestión de Estudiantes
2. Gestión Académica
3. Gestión de Profesores
4. Actividades Extracurriculares
5. Gestión Financiera
6. Gestión de Recursos
7. Reportes y Estadísticas
8. Salir

Seleccione una opción:
```

## 13. Problemas Conocidos y Limitaciones

- El CUFE, el código QR y la resolución de la DIAN son simulados: la factura no se envía a la DIAN y el QR no se puede escanear.
- La factura se genera como archivo de texto (`.txt`). La columna `facturas.nombre_archivo_pdf` del script guarda el nombre de ese archivo.
- Los conceptos de pago no tienen campo de IVA, por eso la factura muestra el IVA como "no aplica".
- El reporte de morosos se basa en la fecha del último pago de un concepto mensual en el año académico actual (o en la fecha de matrícula si nunca ha pagado): el sistema no guarda cuentas por cobrar.
- Los datos de prueba se armaron tomando como fecha actual el 17 de noviembre de 2026; los morosos y los préstamos vencidos cambian según la fecha en que se ejecute.
- En consolas de Windows con fuentes antiguas, los recuadros del menú pueden verse distintos; en la consola de IntelliJ se ven bien.

## 14. Futuras Mejoras

- Generar la factura también en PDF y enviarla por correo al acudiente.
- Usuarios y roles (rector, secretaría, profesor) con inicio de sesión.
- Exportar los reportes a archivos.

## 15. Licencia

Proyecto académico desarrollado para fines educativos. Todos los derechos reservados por su autor.

## 16. Contacto

- **Deyvid Godoy**
- Correo: deyvidjgv12@gmail.com
- GitHub: [deyvidjgv](https://github.com/deyvidjgv)
