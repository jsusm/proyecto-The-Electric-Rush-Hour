# Cronicas del proyecto
Primero implementamos el movimiento de los carros en el tablero, esto fue bastante sencillo de implementar,
basicamente los vehiculos son hilos que tratan de moverse en un tablero, si la posición a la que se quiere moverse el
vehiculo esta ocupada por otro el vehiculo se bloquea hasta que la posicion sea liberada, la interaccion con el
tablero (recurso critico) se hace a traves del monitor `Parking`.

Luego implementamos las unidades de carga, estas fueron un poco más complicadas de pensar, puesto que las unidades de
carga se tienen que comportar como "productores" y los vehiculos como consumidores, los vehiculos se bloquean hasta que
son recargados y las unidades de carga se bloquean si no hay vehiculos por recargar, el problema de la implementación
fue que las unidades de carga seguian ejecutandose cuando se "completaba" el juego, pensamos en añadir una especie
de tiempo límite, pero la implementación se complicaba y teniamos que usar ciertos mecanismos de java que no
entendiamos, la solución a la que llegamos fue que los vehiculos al terminar su ejecución, es decir cuando se termina
el juego, se recargaran, así el hilo de la unidad de carga que estaba bloqueado puede evaluar si ya se termino el
juego y terminar su ejecución.

Por ultimo implementamos la entrada del programa, estas se leen del archivo de entrada y se guarda la información en
la clase `InputData` para luego construir todos los objetos necesarios para la ejecución del programa.
