
# The Electric Rush Hour
El actual proyecto es la implementación del juego Rush Hour. En los proyectos anteriores de programación funcional y lógica, se trabajó en encontrar la solución algorítmica para liberar el vehículo . Ahora, la administración del estacionamiento inteligente ha decidido automatizar el proceso. Sin embargo, surge un problema físico: los vehículos son eléctricos y su movimiento consume energía, por lo que requieren asistencia externa para mantenerse operativos durante la maniobra de salida. Por lo tanto se nos solicitó implementar una solución concurrente para simular este escenario, utilizando monitores e hilos (Threads) en Java.

#### Procesos Involucrados
Para la solución del problema se utilizó la lógica de productor-consumidor.
- **Vehículos (Consumidores)**: Estos hilos cuentan con una batería, la cual se va consumiendo a cada paso que dan al quedarse sin batería se necesita recargarla por lo tanto se hace uso de las unidades de carga.

- **Unidades de carga (Productores)**: Hilos encargados de llenar la batería de los vehículos cuando estos llegen a 0. Nosotros decidimos que cuando un vehículo se quede sin batería las unidades de carga  ponen la batería del vehículo en 10 unidades.

#### Recursos Críticos
1. El primer recurso crítico es el tablero ya que dos o más hilos (vehículos) no deberían acceder a la misma casilla o espacio dentro del estacionamiento al mismo tiempo. por lo tanto su manipulación tiene que ser protegida para evitar condiciones de carrera. Este tablero se encuentra dentro del monitor `Parking`.

2. El segundo recurso crítico en nuestra implementación es la lista de vehículos descargados dentro del monitor ChargeMonitor. Ya que las unidades de carga no deben acceder a esta lista al mismo tiempo ya que puede ocurrir que dos unidades de carga quieran recargar la batería de un mismo vehículo.


#### Condiciones de Sincronización
- Si un vehículo quiere acceder a un espacio del estacionamiento que esté ocupado este debe esperar `wait()` hasta que se desocupe ese espacio.

- Si no hay vehículos en la lista vehiclesOnWait la unidad de carga espera .

- Si la batería llega a 0 el vehículo se detiene y debe esperar a ser cargado por la unidad de carga.

- Si el vehículo objetivo ID 0 alcanza la columna 5 del tablero . Al cumplirse la condición los hilos termina su ejecución detiene forma coordinada para que el programa no quede colgado.

#### Operaciones de acceso
- **Ocupación de celdas**: Ésta es la operación de acceso más importante ya que se maneja la logística de acceso a las celdas del tablero y a la liberación de las celdas también. Esta operación está dentro del monitor `Parking` y se llama requestMove y al ser synchronized no permite que dos vehículos traten de moverse a la vez a una celda vacia. Se va a hablar más sobre este monitor y este método más adelante.

- **Fin del juego (GameOver() )**: Esta operación es crítica porque determina si los hilos deben detener su ejecución. Ella verificar si el vehículo 0 llegó a la meta que en este caso es que llegó a la columna 5 . Es necesario que sea synchronized ya que esta accede y evalúa el recurso crítico, de lo contrario puede generar condiciones de carrera, por ejemplo que se modifique el tablero y a la mitad de la evaluación del método. Al igual que el anterior está implementando en el monitor `Parking`.

- **Cargar vehiculos (requestCharge)** Para gestionar de la mejor manera la logística de cargar los vehículos y minimizar interbloqueos en esta área en específico está la operación de acceso a cargar batería. Se realiza en un monitor`ChargeMonitor`que implementa el modelo productor-consumidor . Gestiona la lista de espera de vehículos varados (vehiclesOnwait) Asegura que un vehículo se bloquee hasta que una unidad de carga lo recargue.

- **Cargar (charge())** Se encarga de la logíca de cargar los vehículos, keepCharging() ve si hay vehiculos para cargar y si es verdad charge() los retira de la lista son syncronized porque acceden a la lista de vehiculos que necesitan ser cargados.


#### Decisiones de diseño
- Los vehículos se mueven un espacio a la vez, cada 5 movimientos es más probable que avance
y los siguientes 5 movimientos es más probable que se retroceda.

- Primero implementamos el movimiento de los carros en el tablero, esto fue bastante sencillo de implementar,básicamente los vehículos son hilos que tratan de moverse en un tablero, si la posición a la que se quiere moverse el vehículo esta ocupada por otro el vehículo se bloquea hasta que la posición sea liberada, la interacción con el tablero (recurso critico) se hace a traves del monitor `Parking`.

- implementamos las unidades de carga, estas fueron un poco más complicadas de pensar, puesto que las unidades decarga se tienen que comportar como "productores" y los vehículos como consumidores, los vehículos se bloquean hasta que son recargados y las unidades de carga se bloquean si no hay vehículos por recargar, el problema de la implementación fue que las unidades de carga seguian ejecutandose cuando se "completaba" el juego, pensamos en añadir una especie de tiempo límite, pero la implementación se complicaba y teníamos que usar ciertos mecanismos de java que no entendiamos, la solución a la que llegamos fue que los vehículos al terminar su ejecución, es decir, cuando se termina el juego, se recargaran, así el hilo de la unidad de carga que estaba bloqueado puede evaluar si ya se terminó el juego y terminar su ejecución.

- Luego nos dimos de cuenta que se podia producir un interbloqueo si cuatro vehículos se bloqueaban entre si, por lo tanto tenían que intentar moverse en la dirección contraria si la dirección que habian elegido estaba ocupada. Modificamos el comportamiento del monitor `Parking` para que revisara ambas direcciones y tomara una de ellas, esto para prevenir interbloqueos.



### Procesador de entrada
Se hizo la clase Main y en el Main está la logística de la entrada del proyecto. En el proyecto se nos solicitó que la entrada con la información de los vehículos y los cargadores(unidades de carga) fueran a través de un archivo. Por lo tanto implementamos la entrada del programa, estas se leen del archivo de entrada y se guarda la información en la clase `InputData` para luego construir todos los objetos necesarios para la ejecución del programa.

```java
public class Main {
  public static InputData getInput(String inputFilePath) throws FileNotFoundException {
    InputData input = new InputData();
    File archivo = new File(inputFilePath);

    Scanner reader = new Scanner(archivo);

    while (reader.hasNextLine()) {
      String linea = reader.nextLine();

      // Dividimos la línea por las comas
      String[] partes = linea.split(",");

      if (partes[0].trim().equals("cargadores")) {
        input.chargeUnits = Integer.parseInt(partes[1].trim());
      } else {
        input.vehicles.add(
            new InputVehicleData(Integer.parseInt(
                partes[0]),
                partes[1].charAt(0),
                Integer.parseInt(partes[2]),
                Integer.parseInt(partes[3]),
                Integer.parseInt(partes[4]),
                Integer.parseInt(partes[5])));
      }
    }
    reader.close();
    return input;
  }

    public static void main(String args[]) {
    if (args.length == 0) {
      System.out
          .println("Para usar el programa tienes que pasarle como primer argumento la ruta del archivo de entrada");
      return;
    }

    InputData input;

    try {
      input = getInput(args[0]);
    } catch (FileNotFoundException e) {
      System.out.println("Archivo no encontrado.");
      return;
    }
    input.print();

    Parking parking = new Parking();
    ChargeMonitor chargeMonitor = new ChargeMonitor(2);
    ArrayList<ChargeUnit> chargeUnits = new ArrayList<ChargeUnit>();
    ArrayList<Vehicle> vehicles = new ArrayList<>();
    ArrayList<Thread> threads = new ArrayList<>();

    // creamos las unidades de carga
    for (int i = 0; i < input.chargeUnits; i++) {
      chargeUnits.add(new ChargeUnit(chargeMonitor, parking));
    }

    // creamos los vehiculos y los aniadimos al tablero
    try {
      for (InputVehicleData v : input.vehicles) {
        parking.loadVehicle(v.ID, v.orientation, v.row, v.col, v.len);
        vehicles.add(new Vehicle(v.ID, v.orientation, v.row, v.col, v.len, v.battery, parking, chargeMonitor));
      }
    } catch (Exception e) {
      System.out.println("Los datos de entrada son invalidos, posiblemente porque hay dos vehiculos solapandose.");
      return;
    }

    parking.printBoard();

    // creamos los hilos
    for (Vehicle v : vehicles) {
      threads.add(new Thread(v));
    }
    for (ChargeUnit u : chargeUnits) {
      threads.add(new Thread(u));
    }
    // ejecutamos los hilos
    for (Thread t : threads) {
      t.start();
    }
  }}
```

### Monitor Parking
La clase monitor Parking, como se ha mencionado anteriormente, se encarga del acceso al recurso crítico que es el estacionamiento y su implementación es la siguiente:

Primero, como se puede apreciar, contiene el tablero de 6x6. Nuestro tablero es de enteros y lo que nosotros hicimos fue que el valor de la casilla vacía es -1 y, cuando la casilla está ocupada por algún vehículo, esta contiene el valor del ID de este vehículo.

Método gameOver(): este se encarga de ver si ya se ganó; es decir, si ya el vehículo de ID 0 llegó a la columna 5. Si es así retorna true, si no retorna false.

Método loadVehicle: que se encarga de cargar los vehículos en el estacionamiento; este se llama cuando se evalúa el archivo de la entrada.

Método isValidMove: al igual que en los proyectos realizados a lo largo del curso, esta función es la encargada de ver si, al mover un vehículo, este no se solapa con algún otro y también verifica que este no se salga del tablero de 6x6.

Método move(): se encarga de realizar el movimiento del vehículo, en el cual pone su ID en la casilla a la que se quiere mover y vuelve a poner el valor -1 en la posición actual, volviendo así a estar disponible este espacio.

Método requestMove: este es el método más importante del monitor y este fue explicado con anterioridad. Este es el método para modificar el recurso crítico (el estacionamiento); este método es synchronized, ya que los hilos no deben acceder al tablero al mismo tiempo. Los vehículos deben esperar hasta que el movimiento sea válido; el movimiento no debe salirse del tablero; este metodo retorna `1` si se tomó el movimiento solicitado o `-1` si se tomó el contrario, si no se pudo mover (porque el juego se acabó) el metodo retorna `0`.

PrintBoard(): se tiene la logística para mostrar el tablero del estacionamiento.

```java
class Parking {
  private int[][] board;

  private final int vehicleMaxAttempts = 4;

  public Parking() {
    board = new int[6][6];

    for (int i = 0; i < 6; i++) {
      for (int j = 0; j < 6; j++) {
        board[i][j] = -1;
      }
    }
  }

  // verificamos si el vehiculo con id 0 llego a la izquierda
  // tiene que ser synchronized porque estamos evaluando el recurso critico
  public synchronized boolean gameOver() {
    for (int i = 0; i < 6; i++) {
      if (board[i][5] == 0) {
        return true;
      }
    }
    return false;
  }

  // Carga vehiculos en la matriz, debe ser llamada cuando se evalua el archivo de
  // configuracion, no esta pensada para que varios hilos llamen a esta funcion
  public void loadVehicle(int id, char orientation, int row, int col, int len) throws Exception {
    for (int i = 0; i < len; i++) {
      int vRow = row;
      int vCol = col;
      if (orientation == 'v')
        vRow = vRow + i;
      else
        vCol = vCol + i;

      if (board[vRow][vCol] != -1) {
        throw new Exception("Vehicle out of board: try to load a vehicle in a invalid position, row: " + row + " col: "
            + col + " len: " + len);
      }
      board[vRow][vCol] = id;
    }
  }

  // Chequea si mover al carro con cierta id, es valido, es decir, no se choca con
  // otro carro
  // ni se sale del tablero
  private boolean isValidMove(int move, int id, char orientation, int row, int col, int len) {
    for (int i = 0; i < len; i++) {
      int vRow = row;
      int vCol = col;
      if (orientation == 'v')
        vRow = vRow + i + move;
      else
        vCol = vCol + i + move;

      if (vRow < 0 || vRow > 5)
        return false;
      if (vCol < 0 || vCol > 5)
        return false;

      if (board[vRow][vCol] != -1 && board[vRow][vCol] != id) {
        return false;
      }
    }

    return true;
  }

  // Mueve al carro con cierta id, lo borra de su posicion actual y lo vuele a
  // poner
  // en la nueva posicion
  private void move(int move, int id, char orientation, int row, int col, int len) {
    for (int i = 0; i < len; i++) {
      int vRow = row;
      int vCol = col;
      if (orientation == 'v')
        vRow = vRow + i;
      else
        vCol = vCol + i;
      board[vRow][vCol] = -1;
    }
    for (int i = 0; i < len; i++) {
      int vRow = row;
      int vCol = col;
      if (orientation == 'v')
        vRow = vRow + i + move;
      else
        vCol = vCol + i + move;
      board[vRow][vCol] = id;
    }
  }

  public synchronized boolean requestMove(int move, int id, char orientation, int row, int col, int len) {
    int tries = 0;
    while (!isValidMove(move, id, orientation, row, col, len)) {
      tries++;
      if (gameOver()) {
        return false;
      }
      if (tries > vehicleMaxAttempts) {
        return false;
      }
      try {
        wait();
      } catch (InterruptedException e) {
        System.out.println("Error waiting.");
      }
    }
    move(move, id, orientation, row, col, len);
    clearScreen();
    printBoard();
    notifyAll();
    return true;
  }

  public static void clearScreen() {
    // ANSI escape codes: \033[H moves cursor to top-left, \033[2J clears the screen
    System.out.print("\033[H\033[2J");
    System.out.flush(); // Ensures the output is sent immediately
  }

  public void printBoard() {
    for (int i = 0; i < 6; i++) {
      System.out.print("|");
      for (int j = 0; j < 6; j++) {
        if (board[i][j] == -1) {
          System.out.print("#|");
        } else {
          System.out.print(board[i][j] + "|");
        }
      }
      System.out.println();
    }

    System.out.println();
    System.out.flush();
  }
}
```

### Clase vehículo (Consumidor)
Clase vehículo: Esta contiene todos los atributos necesarios para el vehículo que son: ID, orientación, longitud, fila y columna (que es la lógica de la cabecera), batería, recibe el estacionamiento en el cual está ubicado y la clase monitor que es la encargada de cargar su batería en caso de que se descargue.

Tiene unos métodos que son:
chooseDirection(): que es para elegir a qué dirección se quiere mover, y tryToMove, que tiene la logística de que, si está descargado, llama a requestCharge (que va a ser explicado más adelante); si no, solicita moverse.

Run(): aquí están todas las tareas que el hilo debe realizar, que es ver si ya ganó, verificar que no esté descargado, elegir una dirección y solicitar moverse.

```java
class Vehicle implements Runnable {
  public int ID;
  private char orientation;
  private int len;
  private int row;
  private int col;
  private int battery;
  private Parking parking;
  private ChargeMonitor chargeMonitor;

  // el vehiculo tendra una mayor probabilidad de avansar por 5 movimientos
  // luego tendra una probabilidad mayor de retroceder por 5 movimientos
  private int goRandomFoward = 0;

  public Vehicle(int ID, char orientation, int row, int col, int len, int battery, Parking parking,
      ChargeMonitor chargeMonitor) {
    this.ID = ID;
    this.orientation = orientation;
    this.len = len;
    this.row = row;
    this.col = col;
    this.battery = battery;
    this.parking = parking;
    this.chargeMonitor = chargeMonitor;
  }

  public int chooseDirection() {
    if (orientation == 'h') {
      if (col == 0)
        return 1;
      if (col + len - 1 == 5)
        return -1;
    }
    if (orientation == 'v') {
      if (row == 0)
        return 1;
      if (row + len - 1 == 5)
        return -1;
    }

    // elegir direccion aleatoria
    double fowardProb = ((goRandomFoward % 10) < 5) ? 0.75 : 0.25;
    int direction = Math.random() < fowardProb ? 1 : -1;
    return direction;
  }

  public void tryToMove() {
    if (battery <= 0) {
      chargeMonitor.requestCharge(ID);
    }

    int move = chooseDirection();

    boolean moveSuccesfully = parking.requestMove(move, ID, orientation, row, col, len);

    if (moveSuccesfully) {
      if (orientation == 'h') {
        col = col + move;
      } else {
        row = row + move;
      }
      battery -= 1;
    }
  }

  @Override
  public void run() {
    while (true) {
      if (parking.gameOver()) {
        break;
      }
      tryToMove();

      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        // TODO: handle exception
      }
    }
    chargeMonitor.requestCharge(ID);
    System.out.println("Vehicle with id: " + ID + " stopped");
    Thread.currentThread().interrupt();
  }
}
```

### Monitor ChargeMonitor

Monitor de carga: Este monitor es el encargado de la logística de cargar la batería de los vehículos del estacionamiento. Contiene el recurso crítico de los vehículos en carga y la cantidad de unidades de carga.

El método más importante es el de requestCharge (usado por el proceso vehículo); aquí es donde el vehículo que se ha quedado sin baterías solicita permiso para ser recargado. Este método garantiza que un solo vehículo a la vez pueda modificar la lista de espera, evitando errores. Los vehiculos entran a la lista y espera hasta que una unidad de carga cargue la batería, valga la redundancia.

Método charge(): Este método es usado por las unidades de carga; si no hay vehículos que cargar, entonces este espera. Si hay, lo carga y lo elimina de la lista.Este método tambien es syncronized.

```java
class ChargeMonitor {
  private int avairableBatteryUnits = 0;
  private ArrayList<Integer> vehiclesOnWait = new ArrayList<>();

  public ChargeMonitor(int units) {
    avairableBatteryUnits = units;
  }

  public boolean keepChargin() {
    return vehiclesOnWait.size() > 0;
  }

  public synchronized int requestCharge(int id) {
    System.out.println("Vehicle id: " + id + " Request Charged!");
    while (vehiclesOnWait.size() > avairableBatteryUnits) {
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO: handle exception
      }
    }
    vehiclesOnWait.add(id);
    notifyAll();
    System.out.println("Vehicle id: " + id + " waits to Charged!");
    while (vehiclesOnWait.contains(id)) {
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO: handle exception
      }
    }
    notifyAll();
    System.out.println("Vehicle id: " + id + " Charged!");
    return 10;
  }

  public synchronized boolean keepCharging() {
    return vehiclesOnWait.size() > 0;
  }

  public synchronized void charge() {
    while (vehiclesOnWait.size() == 0) {
      try {
        wait();
      } catch (InterruptedException e) {
        // TODO: handle
      }
    }
    int vehicleIdToRemove = vehiclesOnWait.remove(0);
    // System.out.println("Charging Vehicle id: " + vehicleIdToRemove);

    notifyAll();
  }
}
```

### Clase Unidad de Carga (Productor).
Clase unidad de carga: que tiene los dos monitores, el de carga y el del estacionamiento.

Run(): el hilo verifica que no se haya acabado el juego, o mientras queden vehículos por cargar. La unidad de carga debe cargar la batería de los vehículos.

```java
class ChargeUnit implements Runnable {
  private ChargeMonitor chargeMonitor;
  private Parking parking;

  public ChargeUnit(ChargeMonitor chargeMonitor, Parking parking) {
    this.chargeMonitor = chargeMonitor;
    this.parking = parking;
  }

  @Override
  public void run() {
    while (!parking.gameOver() || chargeMonitor.keepChargin()) {
      chargeMonitor.charge();
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        // TODO: handle exception
      }
    }
    System.out.println("Charge unit stopped");
    Thread.currentThread().interrupt();
  }
}
```

### Como ejecutar el proyecto

Para esto hacemos uso de la regla run implementada en el makefile, indicando los argumento que recibe el programa usando el parametro entrada. Esta regla compila y ejecuta, si estas en windows, recomiendo usar la consola Git bash. Puede llegar a fallar alguno de los comandos que se usaron en el makefile si se usa powershell o la cmd de windows.

-Windows
```bash
mingw32-make run entrada="ruta/del/archivo.txt"
```

-Linux
```bash
make run entrada="ruta/del/archivo.txt"
```

Importante: make es la herramienta que se utiliza para ejecutar archivos makefile. En windows este viene junto con la instalación de C/C++ o con Git Bash. Y si estas en linux este viene junto con el entorno Unix.

Tambien hay recetas para ejecutar los casos de prueba.

```bash
make ejemplo0
make ejemplo1
make ejemplo2
make ejemplo3
```
