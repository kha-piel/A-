import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;

public class PathfindingFX extends Application {

    // Cấu hình lưới
    private static final int COLS = 25;
    private static final int ROWS = 15;
    private static final int SIZE = 40;
    private static final int GAP = 1;

    // Màu sắc (Flat Design)
    private final Color COLOR_BG = Color.web("#ecf0f1");
    private final Color COLOR_NODE = Color.web("#ffffff");
    private final Color COLOR_WALL = Color.web("#2c3e50");
    private final Color COLOR_START = Color.web("#2ecc71"); // Xanh lá
    private final Color COLOR_END = Color.web("#e74c3c");   // Đỏ
    private final Color COLOR_VISITED = Color.web("#3498db"); // Xanh dương
    private final Color COLOR_PATH = Color.web("#f1c40f");    // Vàng

    private NodeFX[][] grid = new NodeFX[COLS][ROWS];
    private NodeFX startNode, endNode;
    
    private boolean isRunning = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #ecf0f1;");

        // 1. TẠO LƯỚI (GRID)
        GridPane gridPane = new GridPane();
        gridPane.setPadding(new Insets(20));
        gridPane.setHgap(GAP);
        gridPane.setVgap(GAP);
        gridPane.setAlignment(Pos.CENTER);

        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                NodeFX node = new NodeFX(x, y);
                grid[x][y] = node;
                gridPane.add(node, x, y);

                // Xử lý chuột để vẽ tường
                node.setOnMousePressed(e -> handleMouse(node, e.getButton()));
                node.setOnMouseDragEntered(e -> handleMouse(node, MouseButton.PRIMARY));
            }
        }
        
        // Đặt điểm đầu cuối mặc định
        setStartNode(4, 7);
        setEndNode(20, 7);

        root.setCenter(gridPane);

        // 2. TẠO THANH ĐIỀU KHIỂN (BUTTONS)
        HBox controls = new HBox(15);
        controls.setPadding(new Insets(20));
        controls.setAlignment(Pos.CENTER);

        Button btnStart = createStyledButton("BẮT ĐẦU (A*)", "#2ecc71");
        Button btnReset = createStyledButton("LÀM MỚI", "#e74c3c");
        Button btnRandom = createStyledButton("TẠO TƯỜNG", "#34495e");

        // Thêm hiệu ứng click cho nút
        addClickEffect(btnStart);
        addClickEffect(btnReset);
        addClickEffect(btnRandom);

        // Gán hành động
        btnStart.setOnAction(e -> startAStar());
        btnReset.setOnAction(e -> resetGrid());
        btnRandom.setOnAction(e -> generateWalls());

        controls.getChildren().addAll(btnStart, btnReset, btnRandom);
        root.setBottom(controls);

        Scene scene = new Scene(root, COLS * SIZE + 100, ROWS * SIZE + 150);
        primaryStage.setTitle("JavaFX Pathfinding Visualizer - A* Algorithm");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- XỬ LÝ LOGIC GIAO DIỆN ---

    private void handleMouse(NodeFX node, MouseButton btn) {
        if (isRunning) return;
        if (node == startNode || node == endNode) return;

        if (btn == MouseButton.PRIMARY) {
            node.setAsWall(true); // Chuột trái vẽ tường
        } else if (btn == MouseButton.SECONDARY) {
            node.setAsWall(false); // Chuột phải xóa
        }
    }

    // --- THUẬT TOÁN A* (Logic tách biệt) ---
    private void startAStar() {
        if (isRunning) return;
        isRunning = true;
        
        // Xóa trạng thái cũ (giữ tường)
        clearPathData();

        PriorityQueue<NodeFX> openList = new PriorityQueue<>();
        ArrayList<NodeFX> visitedOrder = new ArrayList<>(); // Để lưu thứ tự animation
        
        startNode.g = 0;
        startNode.calculateH(endNode);
        startNode.calculateF();
        openList.add(startNode);

        boolean found = false;

        while (!openList.isEmpty()) {
            NodeFX current = openList.poll();
            visitedOrder.add(current); // Lưu lại để diễn hoạt

            if (current == endNode) {
                found = true;
                break;
            }

            current.visited = true;

            for (NodeFX neighbor : getNeighbors(current)) {
                if (neighbor.isWall || neighbor.visited) continue;

                double tempG = current.g + 1;

                if (tempG < neighbor.g) {
                    neighbor.parent = current;
                    neighbor.g = tempG;
                    neighbor.calculateH(endNode);
                    neighbor.calculateF();

                    if (!openList.contains(neighbor)) {
                        openList.add(neighbor);
                    }
                }
            }
        }

        if (found) {
            animateSearch(visitedOrder); // Bắt đầu hiệu ứng
        } else {
            System.out.println("Không tìm thấy đường!");
            isRunning = false;
        }
    }

    // --- PHẦN QUAN TRỌNG: ANIMATION (HIỆU ỨNG) ---
    
    private void animateSearch(List<NodeFX> visitedNodes) {
        // Dùng SequentialTransition để chạy các animation nối tiếp nhau
        SequentialTransition seq = new SequentialTransition();

        // 1. Animation quá trình tìm kiếm (Visited)
        for (NodeFX node : visitedNodes) {
            if (node == startNode || node == endNode) continue;
            
            // Tạo hiệu ứng: Đổi màu + Phóng to thu nhỏ (Pop effect)
            ScaleTransition st = new ScaleTransition(Duration.millis(20), node);
            st.setFromX(0.5); st.setFromY(0.5);
            st.setToX(1.0); st.setToY(1.0);
            
            // Dùng sự kiện onFinished để đổi màu vĩnh viễn
            PauseTransition pt = new PauseTransition(Duration.millis(5));
            pt.setOnFinished(e -> node.setColor(COLOR_VISITED));
            
            seq.getChildren().add(pt);
        }

        // 2. Animation vẽ đường đi ngắn nhất (Path)
        seq.setOnFinished(e -> animatePath());
        seq.play();
    }

    private void animatePath() {
        ArrayList<NodeFX> path = new ArrayList<>();
        NodeFX current = endNode.parent;
        while (current != startNode && current != null) {
            path.add(current);
            current = current.parent;
        }
        Collections.reverse(path); // Đảo ngược để vẽ từ Start -> End

        SequentialTransition pathSeq = new SequentialTransition();

        for (NodeFX node : path) {
            // Hiệu ứng vẽ đường đi: Màu vàng rực rỡ
            FillTransition ft = new FillTransition(Duration.millis(300), node, (Color) node.getFill(), COLOR_PATH);
            ScaleTransition st = new ScaleTransition(Duration.millis(300), node);
            st.setFromX(0.8); st.setFromY(0.8);
            st.setToX(1.2); st.setToY(1.2);
            st.setAutoReverse(true);
            st.setCycleCount(2); // Nhịp đập 2 lần

            ParallelTransition parallel = new ParallelTransition(node, ft, st);
            pathSeq.getChildren().add(parallel);
        }
        
        pathSeq.setOnFinished(e -> isRunning = false);
        pathSeq.play();
    }

    // --- CÁC HÀM PHỤ TRỢ ---

    private List<NodeFX> getNeighbors(NodeFX node) {
        List<NodeFX> list = new ArrayList<>();
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};

        for (int i = 0; i < 4; i++) {
            int nx = node.x + dx[i];
            int ny = node.y + dy[i];
            if (nx >= 0 && nx < COLS && ny >= 0 && ny < ROWS) {
                list.add(grid[nx][ny]);
            }
        }
        return list;
    }
    
    private void resetGrid() {
        isRunning = false;
        for(int x=0; x<COLS; x++) {
            for(int y=0; y<ROWS; y++) {
                grid[x][y].reset(false); // False: Xóa hết cả tường
                if(grid[x][y] == startNode) grid[x][y].setColor(COLOR_START);
                if(grid[x][y] == endNode) grid[x][y].setColor(COLOR_END);
            }
        }
    }
    
    private void clearPathData() {
        for(int x=0; x<COLS; x++) {
            for(int y=0; y<ROWS; y++) {
                grid[x][y].visited = false;
                grid[x][y].parent = null;
                grid[x][y].g = Double.MAX_VALUE;
                if(!grid[x][y].isWall && grid[x][y] != startNode && grid[x][y] != endNode) {
                    grid[x][y].setColor(COLOR_NODE);
                }
            }
        }
    }
    
    private void generateWalls() {
        resetGrid();
        Random rand = new Random();
        for(int x=0; x<COLS; x++) {
            for(int y=0; y<ROWS; y++) {
                if(grid[x][y] != startNode && grid[x][y] != endNode) {
                    if(rand.nextDouble() < 0.3) grid[x][y].setAsWall(true);
                }
            }
        }
    }

    private void setStartNode(int x, int y) {
        startNode = grid[x][y];
        startNode.setColor(COLOR_START);
    }
    private void setEndNode(int x, int y) {
        endNode = grid[x][y];
        endNode.setColor(COLOR_END);
    }

    // --- CUSTOM UI COMPONENTS ---

    // Nút bấm đẹp với CSS
    private Button createStyledButton(String text, String colorHex) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: " + colorHex + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 14px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 5;" +
            "-fx-min-width: 120px;"
        );
        return btn;
    }

    // Hiệu ứng nhấn nút (Scale effect)
    private void addClickEffect(Button btn) {
        btn.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(0.9); st.setToY(0.9); // Thu nhỏ lại 90%
            st.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(100), btn);
            st.setToX(1.0); st.setToY(1.0); // Trở về bình thường
            st.play();
        });
    }

    // --- NODE CLASS (JavaFX Rectangle) ---
    private class NodeFX extends Rectangle implements Comparable<NodeFX> {
        int x, y;
        boolean isWall = false;
        boolean visited = false;
        NodeFX parent = null;
        double g = Double.MAX_VALUE;
        double h = 0;
        double f = 0;

        public NodeFX(int x, int y) {
            super(SIZE, SIZE); // Kích thước ô
            this.x = x;
            this.y = y;
            this.setFill(COLOR_NODE);
            this.setStroke(Color.web("#bdc3c7")); // Viền xám nhạt
            this.setArcWidth(10); // Bo góc
            this.setArcHeight(10);
        }

        public void setAsWall(boolean isWall) {
            this.isWall = isWall;
            if (isWall) {
                // Hiệu ứng khi tạo tường
                FillTransition ft = new FillTransition(Duration.millis(200), this, (Color)this.getFill(), COLOR_WALL);
                ft.play();
            } else {
                this.setFill(COLOR_NODE);
            }
        }

        public void setColor(Color c) {
            this.setFill(c);
        }

        public void reset(boolean keepWall) {
            visited = false;
            parent = null;
            g = Double.MAX_VALUE;
            f = 0;
            h = 0;
            if (!keepWall) {
                isWall = false;
                this.setFill(COLOR_NODE);
            } else if (!isWall) {
                this.setFill(COLOR_NODE);
            }
        }

        public void calculateH(NodeFX target) {
            // Manhattan Distance
            this.h = Math.abs(this.x - target.x) + Math.abs(this.y - target.y);
        }
        public void calculateF() { this.f = this.g + this.h; }

        @Override
        public int compareTo(NodeFX o) {
            return Double.compare(this.f, o.f);
        }
    }
}
