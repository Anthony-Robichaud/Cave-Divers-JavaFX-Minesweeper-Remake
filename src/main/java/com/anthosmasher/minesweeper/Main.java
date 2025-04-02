package com.anthosmasher.minesweeper;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;

public class Main extends Application {
    private static final int TILE_SIZE = 60;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int X_TILES = WIDTH / TILE_SIZE;
    private static final int Y_TILES = HEIGHT / TILE_SIZE;
    private static final int BOMB_COUNT = 10;

    private Tile[][] grid;
    private GraphicsContext gc;
    private boolean gameOver = false;
    private boolean gameCleared = false;

    private Parent createContent() {
        Canvas canvas = new Canvas(WIDTH, HEIGHT + 150);
        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #c0c0c0");
        gc = canvas.getGraphicsContext2D();

        restartGame();
        canvas.setOnMouseClicked(this::handleMouseClick);

        return root;
    }

    private void restartGame() {
        gameOver = false;
        gameCleared = false;
        gc.clearRect(0, 0, WIDTH, HEIGHT + 150);
        initializeGrid();
        drawFaceButton();
    }

    private void initializeGrid() {
        grid = new Tile[X_TILES][Y_TILES];
        ArrayList<int[]> positions = generateShuffledPositions();
        placeBombs(positions);
        populateTiles();
        setBombCounts();
    }

    private ArrayList<int[]> generateShuffledPositions() {
        ArrayList<int[]> positions = new ArrayList<>();
        for (int x = 0; x < X_TILES; x++) {
            for (int y = 0; y < Y_TILES; y++) {
                positions.add(new int[]{x, y});
            }
        }
        Collections.shuffle(positions);
        return positions;
    }

    private void placeBombs(ArrayList<int[]> positions) {
        for (int i = 0; i < BOMB_COUNT; i++) {
            int[] pos = positions.get(i);
            grid[pos[0]][pos[1]] = new Tile(pos[0], pos[1], true);
        }
    }

    private void populateTiles() {
        for (int x = 0; x < X_TILES; x++) {
            for (int y = 0; y < Y_TILES; y++) {
                if (grid[x][y] == null) {
                    grid[x][y] = new Tile(x, y, false);
                }
            }
        }
    }

    private void setBombCounts() {
        for (int x = 0; x < X_TILES; x++) {
            for (int y = 0; y < Y_TILES; y++) {
                Tile tile = grid[x][y];
                tile.setBombCount((int) getNeighbors(tile).stream().filter(t -> t.hasBomb).count());
                tile.drawHidden();
            }
        }
    }

    private void handleMouseClick(javafx.scene.input.MouseEvent event) {
        int x = (int) (event.getX() / TILE_SIZE);
        int y = (int) (event.getY() / TILE_SIZE);

        if (isFaceButtonClicked(event)) {
            restartGame();
            return;
        }

        if (gameOver || gameCleared || !isValidTile(x, y)) return;

        Tile clickedTile = grid[x][y];

        if (event.getButton() == MouseButton.SECONDARY) {
            clickedTile.toggleFlag();
            checkGameCleared();
        } else {
            clickedTile.open();
        }
    }

    private boolean isFaceButtonClicked(javafx.scene.input.MouseEvent event) {
        double x = event.getX();
        double y = event.getY();
        return (x >= WIDTH / 2 - 30 && x <= WIDTH / 2 + 30 && y >= HEIGHT + 45 && y <= HEIGHT + 105);
    }

    private boolean isValidTile(int x, int y) {
        return x >= 0 && x < X_TILES && y >= 0 && y < Y_TILES;
    }

    private ArrayList<Tile> getNeighbors(Tile tile) {
        ArrayList<Tile> neighbors = new ArrayList<>();
        int[] directions = {-1, -1, -1, 0, -1, 1, 0, -1, 0, 1, 1, -1, 1, 0, 1, 1};

        for (int i = 0; i < directions.length; i += 2) {
            int newX = tile.x + directions[i];
            int newY = tile.y + directions[i + 1];

            if (isValidTile(newX, newY)) {
                neighbors.add(grid[newX][newY]);
            }
        }
        return neighbors;
    }

    private void checkGameCleared() {
        for (int x = 0; x < X_TILES; x++) {
            for (int y = 0; y < Y_TILES; y++) {
                Tile tile = grid[x][y];
                if (tile.hasBomb && !tile.isFlagged) return;
            }
        }
        gameCleared = true;
        drawMapClearedMessage();
    }

    private void drawMapClearedMessage() {
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 50)); // Bold and bigger text
        gc.setTextAlign(TextAlignment.CENTER);

        double textX = WIDTH / 2;
        double textY = HEIGHT / 2 + 15; // Slightly adjusted for better centering

        // Draw white outline (by drawing multiple white texts around the main text)
        gc.setFill(Color.WHITE);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                if (dx != 0 || dy != 0) { // Avoid drawing the center twice
                    gc.fillText("MAP CLEARED", textX + dx, textY + dy);
                }
            }
        }

        // Draw the black main text
        gc.setFill(Color.BLACK);
        gc.fillText("MAP CLEARED", textX, textY);
    }


    private void drawFaceButton() {
        gc.drawImage(new Image(getClass().getResource("/face.png").toExternalForm()), WIDTH / 2 - 30, HEIGHT + 45);
    }

    private class Tile {
        int x, y;
        boolean hasBomb;
        boolean isOpen = false;
        boolean isFlagged;
        int bombCount;

        public Tile(int x, int y, boolean hasBomb) {
            this.x = x;
            this.y = y;
            this.hasBomb = hasBomb;
        }

        public void setBombCount(int count) {
            this.bombCount = count;
        }

        public void drawHidden() {
            gc.drawImage(new Image(getClass().getResource("/tile.png").toExternalForm()), x * TILE_SIZE, y * TILE_SIZE);
        }

        public void drawRevealed() {
            String imgPath = hasBomb ? "/tile_bomb.png" :
                    bombCount > 0 ? "/tile_revealed_" + bombCount + ".png" : "/tile_revealed.png";
            gc.drawImage(new Image(getClass().getResource(imgPath).toExternalForm()), x * TILE_SIZE, y * TILE_SIZE);
        }

        public void open() {
            if (isOpen || isFlagged || gameOver || gameCleared) return;

            isOpen = true;
            drawRevealed();

            if (hasBomb) {
                gameOver = true;
            } else if (bombCount == 0) {
                getNeighbors(this).forEach(Tile::open);
            }
        }

        public void toggleFlag() {
            if (isOpen || gameCleared) return;

            isFlagged = !isFlagged;
            String imgPath = isFlagged ? "/tile_flag.png" : "/tile.png";
            gc.drawImage(new Image(getClass().getResource(imgPath).toExternalForm()), x * TILE_SIZE, y * TILE_SIZE);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        Scene scene = new Scene(createContent());
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setTitle("Cave Divers");
        primaryStage.getIcons().add(new Image(getClass().getResource("/game_icon.png").toExternalForm()));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
