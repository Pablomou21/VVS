# Análisis de rendimiento con JETM

## Objetivo

Las pruebas JETM son pruebas **dinámicas no funcionales**: no comprueban si el resultado de una operación es correcto (de eso se encargan las pruebas funcionales), sino **cuánto tarda** en obtenerse. Una operación que devuelve el resultado correcto pero es demasiado lenta también es un error del software.

JETM (*Java Execution Time Measurement*) solo mide: crea puntos de medida alrededor de la operación y agrega los tiempos (número de mediciones, media, mínimo y máximo, en milisegundos). Lo que convierte esa medida en una prueba es la aserción de JUnit, que compara la media con un **umbral** que representa un requisito de rendimiento.

## Ejecución

- Comando para obtener las mediciones:
  `mvn test -Dtest='*Test#*AverageTime*' | grep "\[JETM\]"`
- JETM: `1.2.3` (`fm.void.jetm:jetm`, ámbito `test`).
- Utilidad común: `src/test/java/es/udc/paproject/backend/testutil/PerformanceMeter.java`. Está en el paquete `testutil`, fuera del `targetTests` de PIT (`es.udc.paproject.backend.test.*`).
- Procedimiento de cada prueba:
  1. **Calentamiento:** 10 ejecuciones sin medir, para que no penalicen la compilación JIT, la inicialización de Hibernate ni el pool de conexiones.
  2. **Medición:** 50 ejecuciones, cada una dentro de un `EtmPoint`.
  3. **Aserción única:** `assertTrue(media < UMBRAL)`.
  4. El monitor se resetea al final, para que las medidas de distintos tests no se mezclen.
- En las pruebas contra la BD, cada ejecución medida termina con `flush()` y `clear()` del `EntityManager`. Así el tiempo incluye las escrituras y lecturas reales en MySQL, y no solo la caché de primer nivel de Hibernate.

### Entorno de medición

| Elemento | Valor |
|---|---|
| Fecha | 27/11/2026 |
| Procesador | Intel(R) Core(TM) Ultra 7 258V |
| Memoria RAM | 16 GB |
| Sistema operativo | Ubuntu 26.04.1 LTS |
| Java | openjdk version "21.0.13.1" 2026-11-17 |
| MySQL | 8.4.13-0ubuntu0.26.04.2 |

## Pruebas implementadas

Cada prueba está en la clase de test del componente que mide, junto a sus pruebas funcionales del mismo nivel.

| Nivel | Clase de test | Prueba | Operación medida |
|---|---|---|---|
| Unidad (Mockito) | `OrderServiceImplTest` | `buyTicketsAverageTimeIsBelowThreshold` | `buyTickets` con los DAO simulados |
| Unidad (Mockito) | `OrderServiceImplTest` | `findUserOrdersAverageTimeIsBelowThreshold` | `findUserOrders` (página de 10) |
| Unidad (Mockito) | `MovieServiceImplTest` | `viewBillboardAverageTimeIsBelowThreshold` | `viewBillboard` (5 películas × 4 sesiones) |
| Unidad (Mockito) | `UserServiceImplTest` | `loginAverageTimeIsBelowThreshold` | `login` con BCrypt simulado |
| Integración (servicio + BD) | `OrderServiceIntegrationTest` | `testBuyTicketsAverageTimeIsBelowThreshold` | `buyTickets` |
| Integración (servicio + BD) | `OrderServiceIntegrationTest` | `testFindUserOrdersAverageTimeIsBelowThreshold` | `findUserOrders` (página de 10 sobre 30 compras) |
| Integración (servicio + BD) | `MovieServiceIntegrationTest` | `testViewBillboardAverageTimeIsBelowThreshold` | `viewBillboard` (5 películas × 4 sesiones) |
| Integración (servicio + BD) | `UserServiceIntegrationTest` | `testLoginAverageTimeIsBelowThreshold` | `login` con BCrypt real |
| Integración REST (extremo a extremo) | `OrderControllerIntegrationTest` | `testBuyTicketsRequestAverageTimeIsBelowThreshold` | `POST /orders` con JWT real |
| Integración REST (extremo a extremo) | `MovieControllerIntegrationTest` | `testViewBillboardRequestAverageTimeIsBelowThreshold` | `GET /movies/billboard/{day}` |

## Criterio de los umbrales

El enunciado no fija requisitos de rendimiento. Como las pruebas deben derivarse de una línea base que determine qué es un error, esa línea base se define a partir de los límites de tiempo de respuesta de J. Nielsen (*Response Times: The 3 Important Limits*):

- **0,1 s (100 ms):** el usuario percibe la respuesta como instantánea.
- **1 s:** el usuario nota el retraso, pero no pierde el hilo de la interacción.

| Nivel | Umbral (media) | Justificación |
|---|---:|---|
| REST de extremo a extremo | 100 ms | Es el presupuesto completo de respuesta instantánea; incluye el filtro JWT, la seguridad, el controlador, el servicio, la BD y la serialización JSON. |
| Servicio + BD real | 50 ms | La mitad del presupuesto; el resto queda para la red, la capa REST y el frontend. |
| `login` (servicio + BD real) | 1000 ms | BCrypt es lento a propósito para dificultar los ataques de fuerza bruta. Se acepta el límite de 1 s porque el login se hace una sola vez por sesión. |
| Unidad (DAO simulados) | 1 ms | El 2 % del umbral de integración: con los DAO simulados solo se mide la lógica del servicio, que no debería consumir el margen reservado a la BD. |

Los umbrales se comparan con la **media**, no con el máximo. El máximo depende de sucesos ajenos a la operación (recolector de basura, planificación del sistema operativo) y daría pruebas inestables.

## Mediciones de referencia

Tres ejecuciones completas de las pruebas JETM. En cada celda, la media de las 50 mediciones de esa ejecución, en ms.

| Operación | Nivel | Ejec. 1 | Ejec. 2 | Ejec. 3 | Media | Máx. observado | Umbral | Margen |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| `buyTickets` | Unidad | 0,080 | 0,111 | 0,118 | 0,103 | 0,650 | 1 | 10× |
| `findUserOrders` | Unidad | 0,051 | 0,084 | 0,050 | 0,062 | 0,596 | 1 | 16× |
| `viewBillboard` | Unidad | 0,049 | 0,042 | 0,069 | 0,053 | 0,542 | 1 | 19× |
| `login` | Unidad | 0,131 | 0,095 | 0,165 | 0,130 | 0,544 | 1 | 8× |
| `buyTickets` | Integración | 6,260 | 5,373 | 5,758 | 5,797 | 10,130 | 50 | 9× |
| `findUserOrders` | Integración | 4,588 | 3,544 | 3,875 | 4,002 | 21,923 | 50 | 12× |
| `viewBillboard` | Integración | 2,227 | 2,427 | 3,071 | 2,575 | 5,555 | 50 | 19× |
| `login` | Integración | 54,140 | 50,613 | 53,473 | 52,742 | 66,262 | 1000 | 19× |
| `POST /orders` | REST | 8,535 | 7,972 | 7,728 | 8,078 | 17,878 | 100 | 12× |
| `GET /movies/billboard/{day}` | REST | 8,217 | 8,227 | 8,096 | 8,180 | 14,242 | 100 | 12× |

- **Media:** media aritmética de las tres ejecuciones.
- **Máx. observado:** el mayor de los tres máximos.
- **Margen:** umbral ÷ media, redondeado; indica cuántas veces más lenta tendría que volverse la operación para que la prueba fallase.
- Todos los tiempos están en milisegundos.

Resultado: las 10 pruebas de rendimiento pasan en las tres ejecuciones.

## Interpretación

### Dónde se va el tiempo

Comparando la misma operación en los tres niveles:

| Operación | Unidad | Integración | REST | Coste de la BD (integración − unidad) | Coste de la capa REST (REST − integración) | Lógica del servicio (unidad) |
|---|---:|---:|---:|---:|---:|---:|
| `buyTickets` / `POST /orders` | 0,103 | 5,797 | 8,078 | 5,694 ms (70 %) | 2,281 ms (28 %) | 1 % |
| `viewBillboard` / `GET /movies/billboard/{day}` | 0,053 | 2,575 | 8,180 | 2,522 ms (31 %) | 5,605 ms (69 %) | 1 % |

Los porcentajes son sobre el tiempo de la petición REST. Como los mocks eliminan la BD, la diferencia entre unidad e integración es el coste de la persistencia, y la diferencia entre integración y REST es el coste de la capa web (HTTP, JWT, seguridad y JSON).

- **La lógica del servicio es despreciable:** con los DAO simulados, todas las operaciones tardan de media 0,130 ms o menos, frente a los 2,6 ms o más que tardan contra la BD. Supone alrededor del 1 % del tiempo de una petición REST.
- **En `buyTickets` domina la persistencia:** el 70 % del tiempo de la petición corresponde al servicio con la BD (lecturas del usuario y de la sesión, actualización de las plazas libres e inserción de la compra). La capa REST añade 2,3 ms (28 %).
- **En `viewBillboard` domina la capa REST:** la consulta a la BD es una sola `SELECT` y cuesta 2,5 ms (31 %), mientras que la capa REST añade 5,6 ms (69 %). A diferencia de la compra, que devuelve un único identificador, la cartelera devuelve 5 películas con 4 sesiones cada una, y convertir esas 20 sesiones a DTO y serializarlas a JSON cuesta más que obtenerlas. Si la cartelera creciera, este sería el primer punto a vigilar.
- **`login` es la excepción:** a nivel de unidad tarda 0,130 ms y contra la BD 52,7 ms, unas 400 veces más. Casi todo ese tiempo es la verificación BCrypt de la contraseña, que a nivel de unidad está simulada, no la consulta a la BD (que en las demás operaciones cuesta pocos milisegundos). Es el comportamiento esperado de BCrypt y el motivo de su umbral propio.

### Estabilidad de las mediciones

Variación de la media entre las tres ejecuciones, calculada como (mayor − menor) ÷ menor:

- **Integración y REST: del 2 % al 38 %.** Las más estables son las peticiones REST (2 % y 10 %) y `login` (7 %). La media es un indicador fiable.
- **Unidad: del 47 % al 74 %.** La variación relativa es alta porque los tiempos son de centésimas de milisegundo, y una sola interrupción del sistema operativo o del recolector de basura pesa mucho en proporción. En valor absoluto, las diferencias son de 0,07 ms como mucho, y el margen respecto al umbral (8× o más) absorbe esa variación de sobra.
- **Los máximos varían mucho más que las medias:** el mayor observado es 21,923 ms en `findUserOrders` (integración), cuya media es 4,0 ms. Por eso las aserciones usan la media y no el máximo.

### Margen respecto a los umbrales

El margen mínimo es de 8× (`login` a nivel de unidad); en integración es de 9× (`buyTickets`), y en REST, de 12×. Ninguna operación está cerca de su umbral, así que no se ha detectado ningún problema de rendimiento y no ha sido necesario abrir ninguna issue. Si una prueba de rendimiento fallase, se trataría como cualquier otro bug: el test se quedaría fallando y se abriría su issue.

## Limitaciones

- **Entorno de desarrollo:** las pruebas no funcionales de sistema deberían ejecutarse en un entorno lo más parecido posible a producción, no solo en el de desarrollo. Aquí se ejecutan en local, con MySQL en la misma máquina y sin latencia de red, así que los tiempos absolutos son optimistas. Los umbrales dejan un margen amplio para compensarlo.
- **Dependencia de la máquina:** los tiempos varían según el hardware. Las mediciones de referencia solo son válidas para el entorno de la tabla anterior.
- **No son pruebas de carga ni de estrés:** las operaciones se ejecutan de una en una, en un solo hilo. No miden el comportamiento con muchos usuarios simultáneos, la carga máxima que soporta el sistema ni el consumo de recursos a lo largo del tiempo. Para eso harían falta herramientas como JMeter o Gatling, que quedan fuera del alcance de la práctica.
- **Datos de prueba pequeños:** las carteleras y los historiales medidos son de tamaño reducido (20 sesiones, 30 compras). Con volúmenes mayores, el coste de las consultas podría crecer y convendría ampliar las pruebas.
