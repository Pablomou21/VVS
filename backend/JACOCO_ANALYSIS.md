# Análisis de cobertura JaCoCo

## Ejecución

- Comando: `mvn clean test jacoco:report "-Dmaven.test.failure.ignore=true"`, con MySQL en marcha.
- JaCoCo: `0.8.14`; Surefire usa `forkCount=1` en el `pom.xml` (con `forkCount=0` JaCoCo no puede engancharse a los tests).
- El informe analiza las 51 clases de producción del backend.
- HTML: `target/site/jacoco/index.html`; XML: `target/site/jacoco/jacoco.xml`.
- El fichero de ejecución se configura como `target/jacoco.exec` y `append=false`, para que cada ejecución mida únicamente la suite actual.

**Nota de entorno (Java 25).** El proyecto está pensado para Java 21. Con un JDK 25, el Byte Buddy 1.14.19 que trae Spring Boot 3.3.8 no puede instrumentar `BCryptPasswordEncoder`, y los 21 tests de `UserServiceImplTest` dan error de Mockito (`Mockito cannot mock this class`) aunque el código esté bien. Pasa igual con `forkCount=0` y sin JaCoCo, así que no lo causa la medición. Con JDK 25 hay que ejecutar con la variable `JAVA_TOOL_OPTIONS=-Dnet.bytebuddy.experimental=true`; con JDK 21 no hace falta.

Se ejecutaron 450 tests: 11 fallos, 1 error y 0 omitidos. El parámetro `maven.test.failure.ignore` permite llegar a la generación del informe, pero **`BUILD SUCCESS` no significa que la suite esté verde**. Todos los fallos son tests que detectan bugs ya reportados:

| Test que falla | Issue |
|---|---|
| `OrderTest.orderWithNonPositiveUnitsIsRejected` | [#1](https://github.com/Pablomou21/VVS/issues/1) |
| `RoomTest.roomWithNonPositiveCapacityIsRejected` | [#2](https://github.com/Pablomou21/VVS/issues/2) |
| `RoomTest.roomWithEmptyNameIsRejected` | [#3](https://github.com/Pablomou21/VVS/issues/3) |
| `OrderControllerIntegrationTest.testFindUserOrdersNegativePageReturnsBadRequest` (error) | [#4](https://github.com/Pablomou21/VVS/issues/4) |
| `OrderServiceImplTest.buyTicketsWithZeroTicketsIsRejected` y `buyTicketsWithNegativeTicketsIsRejected` | [#13](https://github.com/Pablomou21/VVS/issues/13) |
| `OrderServiceIntegrationTest.testBuyTicketsWithNonPositiveTickets` y `testBuyTicketsWithZeroTickets` | [#13](https://github.com/Pablomou21/VVS/issues/13) |
| `UserControllerIntegrationTest.testLoginFromServiceTokenWithoutTokenReturnsUnauthorized` | [#14](https://github.com/Pablomou21/VVS/issues/14) |
| `MovieServiceIntegrationTest.testViewBillboardExcludesSessionAtStartOfNextDay` | [#16](https://github.com/Pablomou21/VVS/issues/16) |
| `MovieTest.movieWithNonPositiveDurationIsRejected` | [#21](https://github.com/Pablomou21/VVS/issues/21) |
| `MovieTest.movieWithEmptyTitleIsRejected` | [#22](https://github.com/Pablomou21/VVS/issues/22) |

## Evolución respecto a la medición anterior

La medición anterior (5-nov) ejecutaba 285 tests; la actual, 450. Los 165 tests nuevos (servicios con Mockito, integración de servicios y REST, rendimiento JETM, `MovieTest`, `UserTest` y los casos inválidos de `RoomTest`) **no cambian ninguna cifra de cobertura**: recorren código que ya estaba cubierto. Su valor está en lo que comprueban, no en las líneas que añaden: han destapado los bugs #13, #14, #16, #21 y #22. Los huecos que quedan son los mismos que antes y se detallan abajo; ninguno de los tests nuevos los ataca.

## Resultado global

| Métrica | Cubierto | Total | Cobertura |
|---|---:|---:|---:|
| Líneas | 590 | 678 | 87,0 % |
| Ramas | 74 | 76 | 97,4 % |
| Instrucciones | 2.239 | 2.417 | 92,6 % |
| Métodos | 244 | 290 | 84,1 % |
| Clases | 51 | 51 | 100 % |

## Cobertura por paquete

| Paquete | Líneas | Ramas |
|---|---:|---:|
| `model.entities` (líneas compiladas) | 117/117 (100 %) | No aplica |
| `model.services` | 130/130 (100 %) | 65/66 (98,5 %) |
| `model.exceptions` | 34/36 (94,4 %) | No aplica |
| `rest.controllers` | 51/51 (100 %) | 4/4 (100 %) |
| `rest.dtos` | 147/219 (67,1 %) | 1/2 (50 %) |
| `rest.common` | 102/114 (89,5 %) | 4/4 (100 %) |
| Paquete raíz (`Application`) | 9/11 (81,8 %) | No aplica |

Las implementaciones proxy de Spring Data para los DAO se generan en ejecución y no aparecen como clases de producción en el informe JaCoCo. Sus consultas sí se verifican funcionalmente con los tests DAO e integración.

## Huecos y tests que podrían cubrirlos

### Entidades, DAOs y excepciones — Iago

- Entidades y DAOs: **100 % de líneas**. Las consultas y los comportamientos de persistencia se ejercitan en los tests DAO e integración.
- En [`InstanceException.java`](./src/main/java/es/udc/paproject/backend/model/exceptions/InstanceException.java) quedan sin cubrir las líneas 10–11: el constructor protegido que recibe un mensaje. Las excepciones concretas usan el constructor de nombre y clave; no hay una ruta de producción que invoque el constructor protegido. Si se busca cubrirlo, un test podría crear una subclase mínima y comprobar `getMessage()`. También es razonable justificar este hueco por no tener uso productivo.

### Servicios — Marqués

- Líneas: **100 %**. Falta una rama en [`MovieServiceImpl.java`](./src/main/java/es/udc/paproject/backend/model/services/MovieServiceImpl.java), línea 68, en `if (!movieSessions.isEmpty())`.
- La rama falsa es inalcanzable con el flujo actual: si `sessions` está vacío, el método retorna antes; si no está vacío, el bucle añade al menos una sesión a `movieSessions`. No existe un test válido que pueda cubrir esa rama sin cambiar el código. Puede dejarse justificada o revisarse la condición redundante en una tarea de producción separada.

### REST — Mou

- El mayor hueco está en [`rest/dtos`](./src/main/java/es/udc/paproject/backend/rest/dtos/): constructores sin argumentos y setters no ejercitados directamente en `AuthenticatedUserDto`, `BillboardEntryDto`, `BillboardSessionDto`, `BlockDto`, `MovieDto`, `OrderDto` y `SessionDto`. Se pueden añadir tests unitarios que creen cada DTO, invoquen sus setters y comprueben los valores con un assert por test.
- En [`UserDto.java`](./src/main/java/es/udc/paproject/backend/rest/dtos/UserDto.java) no se cubre la rama de `userName == null` del constructor (línea 26), ni los setters de `id` y `role` (líneas 39–40 y 98–99). Un test del constructor con nombre de usuario nulo y los demás datos requeridos no nulos cubriría la rama; otros tests pueden comprobar esos setters.
- Los constructores públicos de [`MovieConversor.java`](./src/main/java/es/udc/paproject/backend/rest/dtos/MovieConversor.java) y [`SessionConversor.java`](./src/main/java/es/udc/paproject/backend/rest/dtos/SessionConversor.java) no se ejecutan porque se invocan sus métodos estáticos. Instanciarlos cubriría esas líneas, aunque no añadiría una comprobación de conversión adicional.
- En `rest/common` faltan tests directos de setters de `ErrorsDto`, `FieldErrorDto` y `JwtInfo`. La cobertura de ramas de este paquete ya es del 100 %.
- Los controladores tienen **100 % de líneas y ramas**.

### Integración y arranque — Fidel

- En [`Application.java`](./src/main/java/es/udc/paproject/backend/Application.java) quedan sin cubrir las líneas 15–16, que corresponden a `main` y a la llamada de arranque. Los tests de integración cargan Spring mediante el contexto de test, no invocando `main`; para cubrirlo haría falta un test de humo que arranque explícitamente la aplicación. Es código de infraestructura y no contiene lógica de negocio.
- `ArchitectureTest` ejecutó sus 25 tests sin fallos. El aviso de bytecode versión 69 procede de ArchUnit 1.3.0 al inspeccionar clases de Java 25; no impidió esos tests ni la generación del informe JaCoCo.

## Justificación de cobertura inferior al 100 %

El 87 % de líneas se explica principalmente por setters y constructores de DTO que no se invocan directamente, además del método de arranque y un constructor de excepción sin uso productivo. El 97,4 % de ramas se debe a la rama nula todavía no probada de `UserDto` y a la rama lógicamente inalcanzable de `MovieServiceImpl`. La cobertura no debe elevarse eliminando o debilitando tests existentes; los 11 fallos y el error descritos arriba permanecen visibles en los informes de Surefire.

El resumen por responsable queda preparado en las secciones anteriores para compartirlo con cada compañero; no se ha enviado ningún mensaje fuera del repositorio.
