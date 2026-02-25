import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

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
  }
}

class InputVehicleData {
  public int ID;
  public char orientation;
  public int len;
  public int row;
  public int col;
  public int battery;

  public InputVehicleData(int ID, char orientation, int row, int col, int len, int battery) {
    this.ID = ID;
    this.orientation = orientation;
    this.len = len;
    this.row = row;
    this.col = col;
    this.battery = battery;
  }

  // Método print para mostrar los detalles de un solo vehículo
  public void print() {
    // %d = entero, %c = caracter. El %-3d alinea el ID a la izquierda.
    System.out.printf("Vehículo ID: %-3d | Ori: %c | Len: %d | Pos: (%d, %d) | Bat: %d%%%n",
        ID, orientation, len, row, col, battery);
  }
}

class InputData {
  public ArrayList<InputVehicleData> vehicles = new ArrayList<>();
  public int chargeUnits = 0;

  public void print() {
    System.out.println("========== INPUT DATA ==========");
    System.out.println("Charge Units disponibles: " + chargeUnits);
    System.out.println("Total de vehículos: " + vehicles.size());
    System.out.println("--------------------------------");

    if (vehicles.isEmpty()) {
      System.out.println("(No hay vehículos en la lista)");
    } else {
      for (InputVehicleData v : vehicles) {
        // Llamamos al print del vehículo individual
        v.print();
      }
    }
    System.out.println("================================");
  }
}

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

  // Metodo del Monitor para modificar el recurso critico (el tablero)
  // los carros deberan esperar hasta que su movimiento sea valido, el movimiento
  // debe tener sentido, el movimiento no deberia salirse del tablero por ejemplo,
  // eso
  // bloquearia el hilo del carro indefinidamente, el metodo retorna true si el
  // carro
  // se pudo mover, retorna false si no se puede ejecutar el movimiento, la unica
  // razon
  // por la que no se podria ejecutar un movimiento es porque el juego ya se acabo
  // o se ha hecho un numero x de intentos dado por la constante
  // vehicleMaxAttempts
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
