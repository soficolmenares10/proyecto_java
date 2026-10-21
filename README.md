# ERP Colegio Internacional Little Horizons

Sistema de Planificación de Recursos Empresariales (ERP) educativo, de consola, desarrollado en Java con arquitectura MVC y base de datos MySQL.

---

## 1. Descripción

### Propósito
Centraliza y automatiza los procesos académicos, administrativos y financieros del Colegio Internacional "Little Horizons" (preescolar a 11°, más de 500 estudiantes y 40 profesores), reemplazando los registros en papel y las hojas de cálculo dispersas.

### Problemas que resuelve
| Problema del colegio | Cómo lo resuelve el sistema |
|---|---|
| Pérdida de información académica (notas en cuadernos) | Las notas se guardan en la base de datos, con historial completo por estudiante |
| Actividades extracurriculares desorganizadas | Catálogo de clubes con cupos, membresías, eventos, participantes y podios |
| Facturación manual, lenta y con errores | Pagos con recibo consecutivo y factura electrónica en PDF (CUFE y QR simulados) |
| Recursos que se "pierden" | Inventario y préstamos con fecha de devolución, devoluciones y reporte de vencidos |
| Profesores sin visibilidad del rendimiento | Reportes de promedios y de estudiantes que necesitan apoyo adicional |

### Funcionalidades principales
Gestión de estudiantes, acudientes y matrículas · profesores, carga académica y horarios · materias, inscripciones, notas con promedios y asistencia · clubes y eventos · conceptos de pago, pagos, cobros, morosos y facturas PDF · recursos y préstamos · reportes y estadísticas.

---

## 2. Autores

| Campo | Dato |
|---|---|
| Nombre completo | _Mariana Sofia Colmenares Lopez_ |
| Correo electrónico | _sofiacolmenares106@gmail.com_ |
| GitHub | _[https://github.com/soficolmenares10]_ |

---

## 3. Tecnologías utilizadas

| Categoría | Tecnología |
|---|---|
| Lenguaje | Java 17 (compilado con `--release 17`) |
| IDE | Apache NetBeans (proyecto Maven) |
| Base de datos | MySQL 8.0 o superior |
| Acceso a datos | JDBC con `PreparedStatement` |
| Driver JDBC | MySQL Connector/J 8.4.0 |
| Generación de PDF | OpenPDF 1.3.30 |
| Código QR | ZXing Core 3.5.3 |
| Control de versiones | Git y GitHub |

Las librerías externas se descargan solas con Maven (están declaradas en `pom.xml`).

---

## 4. Requerimientos previos

- **JDK 17 o superior** (se probó con JDK 21).
- **Apache NetBeans** con soporte Maven.
- **MySQL Server 8.0 o superior** — *obligatorio*: el script usa restricciones `CHECK`, que solo funcionan correctamente desde MySQL 8.0.
- MySQL Workbench (opcional, para ejecutar el script SQL).
- Conexión a internet la primera vez (Maven descarga las librerías).
- Git.

---

## 5. Instalación y ejecución

### 5.1 Clonar el repositorio
```bash
git clone https://github.com/<usuario>/<repositorio>.git
cd <repositorio>
```

### 5.2 Abrir el proyecto en el IDE
En NetBeans: **File → Open Project** y elegir la carpeta del proyecto (el icono muestra una "m" de Maven).

### 5.3 Compilar
Clic derecho en el proyecto → **Clean and Build**. Debe terminar con `BUILD SUCCESS`.

### 5.4 Ejecutar
Clic derecho en el proyecto → **Run** (o `F6`). La clase principal es `com.littlehorizons.erp.Main`.

> Si en la consola se ven símbolos `?` en lugar de tildes o marcos: **Properties → Run → VM Options** y escribir `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8`.

---

## 6. Configuración de la base de datos

### 6.1 Creación de la base de datos
El archivo [`docs/database.sql`](docs/database.sql) contiene el DDL y los datos de prueba. Crea la base `erp_little_horizons` desde cero.

- **Con MySQL Workbench:** *File → Open SQL Script*, elegir `docs/database.sql` y pulsar el rayo (Execute).
- **Con la terminal:** `mysql -u root -p < docs/database.sql`

Al final, el script muestra la lista de tablas y los conteos de los datos de prueba.

> **Atención:** el script empieza con `DROP DATABASE IF EXISTS erp_little_horizons`, así que borra los datos anteriores de esa base.

### 6.2 Configuración de credenciales
Editar `src/main/resources/database.properties`:
db.host=localhost
db.port=3306
db.name=erp_little_horizons
db.user=root
db.password=TU_CONTRASEÑA
```
La contraseña se deja **vacía en el repositorio** por seguridad; cada persona escribe la suya en su computador.

### 6.3 Instalación del driver JDBC
No hay que instalarlo a mano: Maven descarga `mysql-connector-j` al hacer *Clean and Build* (dependencia declarada en `pom.xml`).

### Datos opcionales de la factura
`src/main/resources/invoice.properties` guarda los datos del colegio (razón social, NIT, dirección, resolución simulada) que aparecen en las facturas. Todos los valores son ficticios y se pueden cambiar.

---

## 7. Estructura del proyecto

```
little-horizons-erp/
├── docs/
│   └── database.sql                  DDL + DML (datos de prueba)
├── src/main/java/com/littlehorizons/erp/
│   ├── Main.java                     Punto de entrada y conexión de las capas
│   ├── model/                        Entidades y objetos de datos (Student, Payment, Invoice...)
│   ├── dao/                          Acceso a datos: interfaces + implementaciones JDBC
│   ├── controller/                   Lógica de negocio y validaciones
│   ├── service/                      Generación del PDF de la factura
│   ├── view/                         Menús y pantallas de consola
│   └── util/                         Conexión, logger, validaciones, lectura de consola
├── src/main/resources/
│   ├── database.properties           Credenciales de MySQL
│   └── invoice.properties            Datos del colegio para las facturas
├── invoices/                         PDFs generados (se crea al facturar)
├── pom.xml
└── README.md
```

El enunciado muestra `src/com/littlehorizons/erp/...`; al ser un proyecto Maven, NetBeans usa la ruta estándar `src/main/java/...` con los mismos paquetes.

### Arquitectura MVC
| Capa | Responsabilidad | Regla |
|---|---|---|
| **Model** | Representa los datos del dominio | No sabe nada de la consola ni de la base de datos |
| **View** | Muestra menús y lee lo que escribe el usuario | No contiene reglas de negocio ni SQL |
| **Controller** | Valida y aplica las reglas del negocio | No imprime ni escribe SQL |
| **DAO** (apoyo del modelo) | Es lo único que habla con MySQL | Solo contiene SQL y conversión de filas a objetos |

El flujo de una operación es siempre: `View → Controller → DAO → MySQL`, y la respuesta vuelve por el mismo camino.

---

## 8. Principios y patrones de diseño aplicados

### 8.1 Principios SOLID

**S — Responsabilidad única.** Cada clase hace una sola cosa.
- `Validator` solo contiene reglas de validación; `ConsoleUtil` solo lee y escribe en consola (y usa a `Validator`).
- `AppLogger` solo escribe el log; `CufeGenerator` solo calcula el CUFE; `Money` solo da formato a los pesos.

**O — Abierto/cerrado.** Se puede ampliar sin modificar lo que ya funciona.
- `InvoiceController` genera facturas usando la interfaz `InvoiceDocumentGenerator`. Hoy la implementa `PdfInvoiceGenerator`; para generar otro formato bastaría crear otra clase, sin tocar el controlador.

**L — Sustitución de Liskov.** Cualquier implementación puede reemplazar a su interfaz.
- Todos los controladores funcionan igual con `JdbcStudentDao` o con cualquier otra clase que implemente `StudentDao` (por ejemplo, una versión en memoria para pruebas).

**I — Segregación de interfaces.** Interfaces pequeñas y específicas.
- Hay una interfaz por tabla o responsabilidad (`StudentDao`, `GuardianDao`, `PaymentDao`...) en lugar de una interfaz gigante con todos los métodos.

**D — Inversión de dependencias.** Se depende de abstracciones, no de clases concretas.
- Los controladores reciben interfaces `...Dao` en su constructor. Solo `Main` conoce las clases `Jdbc...`, y es quien las conecta (inyección de dependencias manual).

### 8.2 Patrones de diseño
| Patrón | Dónde | Justificación |
|---|---|---|
| **Singleton** | `DatabaseConnection` | Un único lugar lee la configuración y entrega las conexiones |
| **DAO** | paquete `dao` | Separa el SQL de la lógica de negocio; permite cambiar la base de datos sin tocar los controladores |
| **MVC** | todo el proyecto | Separa datos, reglas y pantalla |
| **Inyección de dependencias** | `Main` y constructores | Las clases no crean a sus colaboradores; los reciben |
| **Strategy** | `InvoiceDocumentGenerator` | Permite elegir cómo se genera el documento de la factura |
| **Objeto de valor inmutable** | `record` (`Payment`, `Invoice`, `Attendance`...) | Datos que solo se leen no pueden modificarse por error |

### 8.3 Otras buenas prácticas
- **Streams y lambdas** en el procesamiento de colecciones: promedios, agrupaciones, filtros, ordenamientos y reportes (por ejemplo `AcademicRecordController`, `ChargeController`, `ReportController`).
- **Transacciones**: operaciones que tocan varias tablas (pagar un cobro, prestar un recurso, retirar a un estudiante) se ejecutan completas o no se ejecutan.
- **Bloqueo de filas** (`SELECT ... FOR UPDATE`) al inscribir en un club y al prestar un recurso, para que dos personas no tomen el último cupo a la vez.

---

## 9. Funcionalidades principales

### Gestión de Estudiantes
Registro con acudiente obligatorio · listado, búsqueda y detalle · actualización · suspender o reactivar · acudientes (varios hijos por acudiente, contacto principal) · matrículas por grado y año (activo, graduado, retirado).

### Gestión Académica
Catálogo de materias · inscripción por período · notas de 0.0 a 5.0 con corrección · **promedios** por materia, por período y por curso · boletín · historial académico · asistencia diaria con reporte de inasistencias (más de 3 días por semana).

### Gestión de Profesores
Directorio, búsqueda, estados · asignación de materias · horario semanal **sin cruces** de profesor ni de grupo.

### Actividades Extracurriculares
Clubes con **cupo máximo validado**, inscripción y retiro · eventos con tipo y fecha · participantes · **posiciones** (1er, 2do, 3er lugar) en competencias · historial por estudiante.

### Gestión Financiera
Catálogo de conceptos con IVA · **valores personalizados** (hermanos, becas) · pagos con recibo consecutivo · historial por estudiante y por acudiente · cobros mensuales · **reporte de morosos** · **factura electrónica en PDF** (CUFE simulado, código QR simulado, resolución simulada, IVA y totales).

### Gestión de Recursos
Inventario por categoría y estado · préstamos a estudiantes y profesores (límites por persona) · devoluciones · detección de préstamos **vencidos**.

### Reportes y Estadísticas
Resumen general · estudiantes que necesitan apoyo · promedio por grado · rendimiento por profesor · participación extracurricular · recaudos por fechas · morosos · préstamos vencidos · inventario.

### Interfaz de consola
Menú principal por módulo · validación de todas las entradas con mensajes claros y reintento · confirmaciones para operaciones delicadas · navegación con la opción `0` para volver.

---

## 10. Manejo de errores y logs

### Sistema de logging
La clase `AppLogger` escribe en el archivo **`error_log.txt`**, que se crea en la carpeta desde donde se ejecuta el programa (en NetBeans, la raíz del proyecto). Cada línea lleva fecha, hora, nivel (`INFO`, `WARNING`, `ERROR`), el mensaje y, si hay una excepción, su traza completa.

### Estrategia de manejo de excepciones
| Excepción | Quién la lanza | Qué pasa |
|---|---|---|
| `BusinessException` | Controladores, cuando se rompe una regla del negocio | La vista muestra el mensaje al usuario; no se registra en el log |
| `DataAccessException` | DAO, cuando falla la base de datos | Se escribe el detalle en `error_log.txt` y la vista muestra un mensaje general |
| `InvoiceGenerationException` | Generador de PDF | Se registra en el log; el pago queda guardado y se puede generar la factura después |
| `RuntimeException` inesperada | Cualquier punto | `BaseView` la captura, la registra y evita que el programa se cierre |

Otras prácticas: `try-catch` en cada punto de entrada, `try-with-resources` en todas las conexiones y consultas, y `PreparedStatement` en todas las consultas SQL (los únicos nombres de tabla dinámicos salen de un `enum`, nunca del usuario).

---

## 11. Capturas de pantalla (opcional)

_Agregar aquí capturas del menú principal, un reporte y una factura PDF._

---

## 12. Problemas conocidos y limitaciones

- Aplicación de **consola**: no tiene interfaz gráfica.
- El **CUFE, el código QR y la resolución de facturación son simulados**: la factura no tiene validez fiscal ante la DIAN.
- Una factura corresponde a un solo pago, con una línea de detalle.
- No hay manejo de usuarios ni de permisos (cualquiera que ejecute el programa tiene acceso a todo).
- El pago de un cobro debe ser exacto (no se admiten abonos parciales).
- Los préstamos vencidos se marcan como tales cuando se consultan las listas de préstamos o los reportes.

---

## 13. Futuras mejoras

- Interfaz gráfica (JavaFX o web).
- Inicio de sesión con roles (rector, secretaría, profesor, acudiente).
- Abonos parciales y plan de pagos.
- Envío de la factura por correo electrónico.
- Integración real con la facturación electrónica de la DIAN.
- Pruebas automáticas con JUnit usando DAO en memoria.
- Exportar reportes a Excel o PDF.

---

## 14. Licencia

Proyecto con fines **académicos**, desarrollado como entrega de formación. Todos los datos de prueba (personas, documentos, NIT, resolución) son ficticios.

---

