import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// Assignment 9
// Forbidden Island
//Fliedner Jillian
//jfliedner
//Allen David
//dallen1212

// PLEASE NOTE: the associated image files we used are in a .rar folder
// uploaded to the handin server in a previous submission

//Represents a single square of the game area
class Cell {

  // represents absolute height of this cell, in feet
  double height;

  // In logical coordinates, with the origin at the top-left corner of the screen
  int x;
  int y;

  // the four adjacent cells to this one
  Cell left;
  Cell top; 
  Cell right;
  Cell bottom;

  // reports whether this cell is flooded or not
  boolean isFlooded = false;

  Cell(double height, int x, int y) {
    this.height = height;
    this.x = x;
    this.y = y;
  }

  // method to determine if a given Cell is close in position to this Cell
  boolean around(Cell c) {
    return (Math.abs(this.x - c.x) <= 2 
        && Math.abs(this.y - c.y) <= 2);
  }
  
  // set top cell
  // EFFECT: given cell changes the top field
  void setTop(Cell c) {
    this.top = c;
  }

  // set left cell
  // EFFECT: given cell changes the left field
  void setLeft(Cell c) {
    this.left = c;
  }

  // set right cell
  // EFFECT: given cell changes the right field
  void setRight(Cell c) {
    this.right = c;
  }

  // set bottom cell
  // EFFECT: given cell changes the bottom field
  void setBottom(Cell c) {
    this.bottom = c;
  }

  // renders the image of the Cell
  WorldImage renderCell(int waterHeight) {
    return new RectangleImage(ForbiddenIslandWorld.CELL_SIZE,
        ForbiddenIslandWorld.CELL_SIZE, "solid", this.heightColor(waterHeight));
  }

  // gets the height color of the cell
  Color heightColor(int waterHeight) {
    // if this Cell is next to an OceanCell and is about to be flooded
    // (waterHeight + 1), return a red Color
    if (this.height <= waterHeight + 1 && this.touchingFlooded()) {
      return new Color(255, 0, 0);
    }
    // else return a green Color proportional to height, with a max of 255
    else {
      int greenVal = (int) this.height * 3;
      if (greenVal < 254) {
        return new Color(0, greenVal, 0);
      } else {
        return new Color(0, 255, 0);
      }
    }
  }

  // changes this Cell's isFlooded field if its height is under waterHeight
  // and this Cell is touching an OceanCell
  void changeFlood(int waterHeight) {
    this.isFlooded = this.height <= waterHeight && this.touchingFlooded();
  }

  // returns whether any adjacent cell is flooded
  boolean touchingFlooded() {
    return this.left.isFlooded
        || this.right.isFlooded
        || this.top.isFlooded
        || this.bottom.isFlooded;
  }

}

// to represent an ocean cell
class OceanCell extends Cell {

  OceanCell(double height, int x, int y) {
    super(height, x, y);
    this.isFlooded = true;
  }

  // renders this OceanCell
  WorldImage renderCell(int waterHeight) {
    return new RectangleImage(ForbiddenIslandWorld.CELL_SIZE,
        ForbiddenIslandWorld.CELL_SIZE, "solid", this.heightColor(waterHeight));
  }


  // gets the height color of the cell
  Color heightColor(int waterHeight) {
    return new Color(0, 0, 255);
  }
}

// to represent the user-controlled Player
class Player {

  Cell onCell;  // Cell the player is currently standing on
  int x;
  int y;
  WorldImage pilot = new FromFileImage("src/pilot.jpg");
  int partsPickedUp = 0;
  IList<Cell> cells;  // the board of cells from a ForbiddenIslandWorld

  Player(IList<Cell> cells) {
    this.cells = cells;
    this.x = this.onCell.x;
    this.y = this.onCell.y;
  }

  Player(Cell onCell, IList<Cell> cells) {
    this.onCell = onCell;
    this.x = this.onCell.x;
    this.y = this.onCell.y;
    this.cells = cells;

  }

  // gets the cell this Player is currently on
  Cell getCell() {
    return this.onCell;
  }

}

// to represent a Target, either part or helicopter
class Target {
  boolean pickedUp = false;
  int x;
  int y;
  Cell onCell;  // Cell the Target is currently on

  Target(Cell onCell) {
    this.onCell = onCell;
    this.x = this.onCell.x;
    this.y = this.onCell.y;
  }

  // draws the target as an orange CircleImage
  WorldImage renderTarget() {
    if (this.pickedUp) {
      return new EmptyImage();
    }
    else {
      return new CircleImage(10, OutlineMode.SOLID, Color.orange);
    }
  }
}

// to represent the helicopter
class HelicopterTarget extends Target {

  boolean ableToPickUp = false;
  boolean pickedUp = false;

  HelicopterTarget(Cell onCell) {
    super(onCell);
  }


}


// to represent a Forbidden Island World
class ForbiddenIslandWorld extends World {

  // import images
  WorldImage pilot = new FromFileImage("src/pilot.jpg");
  WorldImage helicopter = new FromFileImage("src/helicopter.png");
  WorldImage dead = new FromFileImage("src/dead.png");
  WorldImage winner = new FromFileImage("src/win.jpg");
  
  // All the cells of the game, including the ocean
  IList<Cell> board = new MtList<Cell>(); 

  // the current height of the ocean
  int waterHeight = 0;

  // tick counter
  int tick = 1;

  // user-guided Player
  Player player;

  // the orange Targets the Player must gather
  IList<Target> helicopterParts = new MtList<Target>();

  // the helicopter
  HelicopterTarget heliTarget = new HelicopterTarget(this.highestPoint());

  // temporary array used in fixLinks and to update flooding
  ArrayList<ArrayList<Cell>> tempArray = new ArrayList<ArrayList<Cell>>();

  // island size
  static final int ISLAND_SIZE = 64;

  // size of one Cell
  static final int CELL_SIZE = 10;

  // size of background
  static final int BGD_SIZE = CELL_SIZE * ISLAND_SIZE;

  // background
  public WorldImage bgd = new RectangleImage(BGD_SIZE,
      BGD_SIZE, OutlineMode.SOLID, Color.WHITE);

  // creates a mountain island
  // EFFECT: fills the tempArray field
  void createMountainIsland() {

    // reset waterHeight
    this.waterHeight = 30;

    // heights of the cell
    ArrayList<ArrayList<Double>> cellHeights = new ArrayList<ArrayList<Double>>();

    // loops through, calculating manhattan distance for each point and adding
    // to cell heights
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i = i + 1) {
      cellHeights.add(new ArrayList<Double>());
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k = k + 1) {
        cellHeights.get(i).add((double) ForbiddenIslandWorld.ISLAND_SIZE - this.manhattan(i, k));
      }
    }

    // list of cells
    ArrayList<ArrayList<Cell>> cells = new ArrayList<ArrayList<Cell>>();

    // loops through, adding the cell corresponding to the cellHeights
    // checks to see what the height is, which dictates if it's a
    // ocean or normal cell
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i = i + 1) {
      cells.add(new ArrayList<Cell>());
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k = k + 1) {
        double cellHeight = cellHeights.get(i).get(k);

        if (cellHeight <= this.waterHeight + 1) {
          cells.get(i).add(new OceanCell(cellHeight, i, k));
        }
        else {
          cells.get(i).add(new Cell(cellHeight, i, k));
        }
      }
    }

    // fixes the links
    this.fixLinks(cells);
    this.tempArray = cells;

    // places Targets on random positions on the map
    IList<Target> tempList = new MtList<Target>();
    for (int i = 0; i <= 4; i++) {
      Cell randomCell = this.randomCell(cells);
      tempList = new ConsList<Target>(new Target(randomCell), tempList);
    }

    // sets the list of Target
    this.helicopterParts = tempList;
    
    // constructs the Player with a randomCell position, and this.board's IList<Cell>
    this.player = new Player(this.randomCell(cells), this.board);
    
    // constructs the helicopter on the highest point in the map
    this.heliTarget = new HelicopterTarget(this.highestPoint());

  }

  // method to run on every tick of the World
  public void onTick() {
    Utils util = new Utils(); // used for Utils methods

    // raise water level every 10 ticks and update flooded Cells,
    // then reset counter
    if (this.tick == 10) {
      this.waterHeight++;
      util.updateFlooded(this.tempArray, this.waterHeight);
      this.tick = 0;
    }
    
    // fix links between Cells
    this.fixLinks(this.tempArray);
    
    // add to tick counter
    this.tick++;

  }

  // fixes the cell's links
  // EFFECT: changes the Cell fields in each of the cells in the given list
  // At the end the updated cells are sent to this.board
  void fixLinks(ArrayList<ArrayList<Cell>> c) {
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i++) {
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k++) {

        Cell cell = c.get(i).get(k);
        int rightSide;
        int leftSide;
        int topSide;
        int bottomSide;

        // define indexes so that Cells on the edges of the World
        // are self-referential for their edge (e.g. the leftSide Cell
        // of a Cell on the left edge of the window would be itself)
        if (k == ForbiddenIslandWorld.ISLAND_SIZE) {
          bottomSide = k;
        }
        else {
          bottomSide = k + 1;
        }

        if (i == ForbiddenIslandWorld.ISLAND_SIZE) {
          rightSide = i;
        }
        else {
          rightSide = i + 1;
        }

        if (k == 0) {
          topSide = k;
        }
        else {
          topSide = k - 1;
        }

        if (i == 0) {
          leftSide = i;
        }
        else {
          leftSide = i - 1;
        }

        // sets links for each side
        if (cell.y > 0) {
          if (cell.y >= ForbiddenIslandWorld.ISLAND_SIZE) {
            cell.setTop(c.get(i).get(topSide));
            cell.setBottom(cell);
          }
          else {
            cell.setTop(c.get(i).get(topSide));
            cell.setBottom(c.get(i).get(bottomSide));
          }
        }

        else {
          cell.setTop(cell);
          cell.setBottom(c.get(i).get(bottomSide));
        }

        if (cell.x > 0) {
          if (cell.x >= ForbiddenIslandWorld.ISLAND_SIZE) {
            cell.setRight(cell);
            cell.setLeft(c.get(leftSide).get(k));
          }
          else {
            cell.setRight(c.get(rightSide).get(k));
            cell.setLeft(c.get(leftSide).get(k));
          }
        }
        else {
          cell.setRight(cell);
          cell.setLeft(c.get(leftSide).get(k));
        }
      }


    }
    
    // update this.board to reflect link changes
    Utils utils = new Utils();
    this.board = utils.arrayListToIList(c);
  }

  // WorldScene to be displayed if the player loses the game
  public WorldScene deathScene() {
    WorldScene end = this.makeScene();
    WorldImage text = new TextImage("You drowned!", 35, FontStyle.BOLD_ITALIC,
        Color.black);
    end.placeImageXY(this.dead, 320, 320);
    end.placeImageXY(text, 320, 500);

    return end;
  }

  // WorldScene to be displayed if the player wins the game
  public WorldScene winScene() {
    WorldScene end = this.getEmptyScene();
    WorldImage text = new TextImage("You survived!", 24, FontStyle.BOLD_ITALIC,
        Color.black);
    end.placeImageXY(this.winner, 320, 320);
    end.placeImageXY(text, 320, 400);

    return end;
  }
  
  // method to handle the end of the World (game)
  public WorldEnd worldEnds() { 
    
    // return the death scene when the player drowns
    if (this.player.getCell().isFlooded) {
      return new WorldEnd(true, this.deathScene());
    }
    
    // return the win scene when the player collects
    // all the parts and returns to the helicopter
    if (this.helicopterReady() && this.player.getCell()
        .equals(this.heliTarget.onCell)) {
      return new WorldEnd(true, this.winScene());
    }
    
    // else the World hasn't ended
    else {
      return new WorldEnd(false, this.makeScene());
    }

  } 


  // calculates the Manhattan distance
  int manhattan(int x, int y) {
    int center = ForbiddenIslandWorld.ISLAND_SIZE / 2;
    return (Math.abs(x - center)
        + Math.abs(y - center));
  }

  // creates a random island
  // EFFECT: fills the tempArray field
  void createRandomIsland() {

    // reset waterHeight
    this.waterHeight = 30;

    Random rand = new Random();
    
    // initializes a cell height (double) array
    ArrayList<ArrayList<Double>> cellHeights = new ArrayList<ArrayList<Double>>();
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i = i + 1) {
      cellHeights.add(new ArrayList<Double>());
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k = k + 1) {
        // if the manhatten distance is greater than half the island size,
        // it's the height of the water
        if (this.manhattan(i, k) > (ForbiddenIslandWorld.ISLAND_SIZE / 2)) {
          cellHeights.get(i).add((double) this.waterHeight); // ocean
        }
        // else it's a randomly generated height above the water level
        else {
          cellHeights.get(i).add((double) 1 + this.waterHeight + rand.nextInt(30));
        }
      }
    }

    // use above heights to construct a Cell array
    ArrayList<ArrayList<Cell>> cells = new ArrayList<ArrayList<Cell>>();
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i = i + 1) {
      cells.add(new ArrayList<Cell>());
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k = k + 1) {
        double cellHeight = cellHeights.get(i).get(k);

        if (cellHeight <= this.waterHeight) {
          cells.get(i).add(new OceanCell(cellHeight, i, k));
        }
        else {
          cells.get(i).add(new Cell(cellHeight, i, k));
        }
      }
    }

    // pushes the created cells to the tempArray
    this.tempArray = cells;

    // fixes links
    this.fixLinks(cells);

    // creates Targets in random locations on the map
    IList<Target> tempList = new MtList<Target>();
    for (int i = 0; i <= 4; i++) {
      Cell randomCell = this.randomCell(cells);
      tempList = new ConsList<Target>(new Target(randomCell), tempList);
    }

    // initializes game components
    this.helicopterParts = tempList;
    this.player = new Player(this.randomCell(cells), this.board);
    this.heliTarget = new HelicopterTarget(this.highestPoint());
  }

  //creates a terrain island
  void createTerrainIsland() {

    this.waterHeight = 15;
    Random rand = new Random();
    ArrayList<ArrayList<Double>> cellHeights = new ArrayList<ArrayList<Double>>();

    // initialize height array to zeros
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i++) {
      cellHeights.add(new ArrayList<Double>());
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k++) {
        cellHeights.get(i).add(0.0);
      }
    }

    // initializes middle and corner heights
    cellHeights.get(ForbiddenIslandWorld.ISLAND_SIZE / 2)
        .set(ForbiddenIslandWorld.ISLAND_SIZE / 2,
        ForbiddenIslandWorld.ISLAND_SIZE / 2.0);
    cellHeights.get(ForbiddenIslandWorld.ISLAND_SIZE / 2)
      .set(0, 1.0);
    cellHeights.get(ForbiddenIslandWorld.ISLAND_SIZE)
      .set(ForbiddenIslandWorld.ISLAND_SIZE / 2, 1.0);
    cellHeights.get(ForbiddenIslandWorld.ISLAND_SIZE / 2)
      .set(ForbiddenIslandWorld.ISLAND_SIZE, 1.0);
    cellHeights.get(0).set(ForbiddenIslandWorld.ISLAND_SIZE / 2, 1.0);

    // finish array of cellHeights using the subdivision algorithm
    this.terrainHeights(0, ForbiddenIslandWorld.ISLAND_SIZE,
        0, ForbiddenIslandWorld.ISLAND_SIZE, cellHeights);

    // construct Cells
    ArrayList<ArrayList<Cell>> cells = new ArrayList<ArrayList<Cell>>();
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i++) {
      cells.add(new ArrayList<Cell>());
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k++) {
        double cellHeight = cellHeights.get(i).get(k);

        if (cellHeight <= this.waterHeight) {
          cells.get(i).add(new OceanCell(cellHeight, i, k));
        }
        else {
          cells.get(i).add(new Cell(cellHeight, i, k));
        }
      }
    }

    // fix links
    this.fixLinks(cells);
    this.tempArray = cells;
    
    // construct random Targets
    IList<Target> tempList = new MtList<Target>();
    for (int i = 0; i <= 4; i++) {
      Cell randomCell = this.randomCell(cells);
      tempList = new ConsList<Target>(new Target(randomCell), tempList);
    }

    // initialize game components
    this.helicopterParts = tempList;
    this.player = new Player(this.randomCell(cells), this.board);
    this.heliTarget = new HelicopterTarget(this.highestPoint());
  }

  // method to pick a random Cell
  Cell randomCell(ArrayList<ArrayList<Cell>> cells) {
    Cell cell = new Cell(0.0, 0, 0);
    boolean notFound = true;
    Random rand = new Random();
    while (notFound) {
      cell = cells.get(rand.nextInt(ForbiddenIslandWorld.ISLAND_SIZE))
          .get(rand.nextInt(ForbiddenIslandWorld.ISLAND_SIZE));
      if (!cell.touchingFlooded() || !cell.isFlooded
          || (!cell.touchingFlooded() && !cell.isFlooded)) {
        notFound = false;
      }
    }

    return cell;

  }
  
  // calculates terrain heights
  // EFFECT: changes the list of the given heights
  void terrainHeights(int x, int maxX, int y, int maxY,
      ArrayList<ArrayList<Double>> heights) {
    if (maxX - x > 1 && maxY - y > 1) {
      double tl = heights.get(x).get(y);
      double tr = heights.get(maxX).get(y);
      double bl = heights.get(x).get(maxY);
      double br = heights.get(maxX).get(maxY);

      double area = (maxX - x) * (maxY - y);

      int midx = (maxX + x) / 2;
      int midy = (maxY + y) / 2;

      double smallerArea = Math.sqrt(area);
      double nudge = .5 * smallerArea;

      double average = (tl + tr + bl + br) / 4;
      Random rand = new Random();

      double t = rand.nextDouble() * nudge + ((tl + tr) / 2);
      double b = rand.nextDouble() * nudge + ((bl + br) / 2);
      double l = rand.nextDouble() * nudge + ((tl + bl) / 2);
      double r = rand.nextDouble() * nudge + ((tr + br) / 2);

      double m = rand.nextDouble() * nudge + average;

      heights.get(x).set(midy, l);
      heights.get(midx).set(y, t);
      heights.get(maxX).set(midy, r);
      heights.get(midx).set(maxY, b);
      heights.get(midx).set(midy, m);

      // recurs on four subdivisions
      this.terrainHeights(x, midx, y, midy, heights);
      this.terrainHeights(midx, maxX, y, midy, heights);
      this.terrainHeights(x, midx, midy, maxY, heights);     
      this.terrainHeights(midx, maxX, midy, maxY, heights);
    }
  }

  // Make the WorldScene
  public WorldScene makeScene() {
    WorldScene background = this.getEmptyScene();
    for (Cell c : this.board) {
      background.placeImageXY(c.renderCell(this.waterHeight), c.x * 10, c.y * 10);
    }
    background.placeImageXY(this.player.pilot, this.player.getCell().x * 10, 
        this.player.getCell().y * 10);
    background.placeImageXY(this.helicopter, this.highestPoint().x * 10,
        this.highestPoint().y * 10);

    for (Target t : this.helicopterParts) {
      background.placeImageXY(t.renderTarget(), t.x * 10, t.y * 10);
    } 

    return background;


  }

  // flag for if the Player has collected all the Targets
  boolean helicopterReady() {
    return this.player.partsPickedUp == 5;
  }

  // method to handle collisions
  void collisionWithParts() {
    boolean pickedUp = true;
    for (Target t : this.helicopterParts) {
      if (t.onCell.equals(this.player.getCell())) {
        t.pickedUp = true;
        this.player.partsPickedUp++;
      }

      pickedUp = pickedUp && t.pickedUp;
    }

  }
  
  // returns the highest Cell on the map
  Cell highestPoint() {
    Cell highest = new Cell(0.0, 0, 0);
    for (Cell c : this.board) {
      if (c.height > highest.height) {
        highest = c;
      }
    }
    return highest;
  }
  
  // Allows for changing to different islands
  public void onKeyEvent(String ke) {
    if (ke.equals("m")) {
      this.waterHeight = 0;
      this.board = new MtList<Cell>(); 
      this.tempArray = new ArrayList<ArrayList<Cell>>();
      this.createMountainIsland();
    }
    else if (ke.equals("r")) {
      this.waterHeight = 0;
      this.board = new MtList<Cell>(); 
      this.tempArray = new ArrayList<ArrayList<Cell>>();
      this.createRandomIsland();
    }
    else if (ke.equals("t")) {
      this.waterHeight = 0;
      this.board = new MtList<Cell>(); 
      this.tempArray = new ArrayList<ArrayList<Cell>>();
      this.createTerrainIsland();
    }
    else if (ke.equals("right") && !this.player.getCell().right.touchingFlooded()) {
      this.player.x = this.player.getCell().right.x;
      this.player.y = this.player.getCell().right.y;
      this.player.onCell = this.player.getCell().right;

    }
    else if (ke.equals("left") && !this.player.getCell().left.touchingFlooded()) {
      this.player.x = this.player.getCell().left.x;
      this.player.y = this.player.getCell().left.y;
      this.player.onCell = this.player.getCell().left;

    }
    else if (ke.equals("down") && !this.player.getCell().bottom.touchingFlooded()) {
      this.player.x = this.player.getCell().bottom.x;
      this.player.y = this.player.getCell().bottom.y;
      this.player.onCell = this.player.getCell().bottom;

    }
    else if (ke.equals("up") && !this.player.getCell().top.touchingFlooded()) {
      this.player.x = this.player.getCell().top.x;
      this.player.y = this.player.getCell().top.y;
      this.player.onCell = this.player.getCell().top;

    }

    this.collisionWithParts();


  }
}

// class representing a IListIterator<T>
class IListIterator<T> implements Iterator<T> {

  IList<T> list;

  IListIterator(IList<T> list) {
    this.list = list;
  }

  // method to return if there is a next element in the IList<T>
  public boolean hasNext() {
    return list.isCons();
  }

  // returns the next object of type T
  public T next() {
    ConsList<T> newList = (ConsList<T>)this.list;
    T answer = newList.first;
    this.list = newList.rest;
    return answer;
  }



}

// to represent an IList<T> interface
interface IList<T> extends Iterable<T> {

  IList<T> add(T t);

  boolean isCons();

  Iterator<T> iterator();

  IList<T> remove(T t);

  int size();
}


// to represent an MtList of type T
class MtList<T> implements IList<T> {


  public IList<T> add(T t) {
    return new ConsList<T>(t, this);

  }

  public boolean isCons() {
    return false;
  }

  public Iterator<T> iterator() {
    return new IListIterator<T>(this);
  }

  public IList<T> remove(T t) {
    return this;
  }

  public int size() {
    return 0;
  }
}

// to represent a non-empty list of type T
class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }


  public IList<T> add(T t) {
    return new ConsList<T>(t, this);

  }


  public boolean isCons() {
    return true;
  }

  public ConsList<T> asCons() {
    return this;
  }

  public Iterator<T> iterator() {
    return new IListIterator<T>(this);
  }

  public IList<T> remove(T t) {
    if (this.first.equals(t)) {
      return this.rest;
    }
    else {
      return this.rest.remove(t);
    }

  }



  public int size() {
    return 1 + this.rest.size();
  }

}



// utility class, primarily for Arrays
class Utils {

  // method to update Cell arrays for if they're flooded
  void updateFlooded(ArrayList<ArrayList<Cell>> list, int waterHeight) {
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i = i + 1) {
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k = k + 1) {

        Cell thisCell = list.get(i).get(k);
        thisCell.changeFlood(waterHeight);

        if (thisCell.isFlooded) {
          list.get(i).set(k, new OceanCell(thisCell.height, thisCell.x, thisCell.y));
        }

      }
    }
  }
  
  // converts arrays of Cells to an IList<Cell>
  public IList<Cell> arrayListToIList(ArrayList<ArrayList<Cell>> list) {
    IList<Cell> cellList = new MtList<Cell>();
    for (int i = 0; i <= ForbiddenIslandWorld.ISLAND_SIZE; i++) {
      for (int k = 0; k <= ForbiddenIslandWorld.ISLAND_SIZE; k++) {

        cellList = cellList.add(list.get(i).get(k));
      }
    }
    return cellList;
  }
}

// Tests
class Examples {

  ForbiddenIslandWorld world = new ForbiddenIslandWorld();
  
  Cell cell0 = new Cell(30.0, 30, 30);
  Cell cell1 = new Cell(30.0, 3, 3);
  Cell cell2 = new Cell(0, 3, 2);
  Cell cell3 = new Cell(1, 2, 3);
  Cell cell4 = new Cell(5, 4, 3);
  Cell cell5 = new Cell(1, 3, 4);
  Cell ocean1 = new OceanCell(4, 2, 2);
  Cell ocean2 = new OceanCell(4, 4, 2);
  Cell ocean3 = new OceanCell(4, 2, 4);
  Cell ocean4 = new OceanCell(4, 4, 4);
  
  // to initialize with what world we'd like to create
  void initWorld() {
    this.world.createMountainIsland();
  }
  
  // to initialize cells for testing
  void initCells() {
    this.cell1.setBottom(cell5);
    this.cell1.setTop(cell2);
    this.cell1.setLeft(cell3);
    this.cell1.setRight(cell4);
    this.cell2.setTop(cell2);
    this.cell2.setBottom(cell1);
    this.cell2.setLeft(ocean1);
    this.cell2.setRight(ocean2);
  }

  // to test (run) the game
  void testGame(Tester t) {
    initWorld();
    this.world.bigBang(640, 640, .3);
  }

  // to test manhattan
  boolean testManhattan(Tester t) {
    return t.checkExpect(this.world.manhattan(3, 4), 57) 
        && t.checkExpect(this.world.manhattan(20, 12), 32);
  }
  
  // to test around
  boolean testAround(Tester t) {
    return t.checkExpect(this.cell2.around(cell0), false) 
        && t.checkExpect(this.ocean1.around(ocean4), true);
  }
  
  // to test around
  boolean testTouchingFlooded(Tester t) {
    initCells();
    return t.checkExpect(this.cell1.touchingFlooded(), false) 
        && t.checkExpect(this.cell2.touchingFlooded(), true);
  }
  
}




