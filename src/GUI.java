import java.awt.*;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * GUI控件列表，用于全局索引
 */
class guigv {
    /* GUI */
    public static JPanel drawboard;
}

/**
 * 绘图板类
 */
class DrawBoard extends JPanel {
    /* 序列号,用于序列化和反序列化 */
    private static final long serialVersionUID = 1L;

    /* g相当于一只画笔，重写该方法以对g完善细节，用于repaint时显示 */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2D = (Graphics2D) g;
        brush.draw(g2D);
    }
}

/**
 * 毛刷：draw方法告诉画板如何对g完善细节，即如何画图
 */
class brush {
    private static int traceIndex = 0;
    public static ArrayList<ArrayList<Loc>> crowdTrace;
    public static Set<Loc> door;
    public static int[][] map;
    private static ArrayList<Point> obstacle = new ArrayList<>();
    private static int obstacleNum;
    private static final int xOffset = 200;
    private static final int yOffset = 200;
    private static final int per = 800 / Main.MAX_MAP; //每一个单位的显示长度

    static void getObstacle() {
        for (int i = 0; i <= Main.MAX_ROOM + 2; i++) {
            for (int j = 0; j <= Main.MAX_ROOM + 2; j++) {
                if (map[i][j] == 1) {
                    obstacle.add(new Point(i, j));
                }
            }
        }
        obstacleNum = obstacle.size();
    }

    static void draw(Graphics2D g) {
        if (traceIndex == crowdTrace.size()) {
            GUI.isEnd = true;
            return;
        }
        ArrayList<Loc> crowd = crowdTrace.get(traceIndex);
        traceIndex++;
        for (int i = 0; i < obstacleNum; i++) {
            g.setColor(Color.GRAY);
            /* 填充矩阵 */
            g.fillRect(xOffset + obstacle.get(i).x * per, yOffset + obstacle.get(i).y * per, per, per);
        }
        for (int i = 0; i < Main.MAX_CROWD; i++) {
            Loc curLoc = crowd.get(i);
            /* 到达门外 */
            if (door.contains(new Loc(curLoc.nodeX, curLoc.nodeY))) continue;
            int xLoc = (int) (curLoc.x * per) + xOffset;
            int yLoc = (int) (curLoc.y * per) + yOffset;
            g.setColor(Color.YELLOW);
            /* 填充圆点 */
            g.fillOval(xLoc, yLoc, per / 2, per / 2);
        }
    }
}


/**
 * 窗口，一般是在窗口中布置GUI控件
 */
class window extends JFrame {
    private static final long serialVersionUID = 1L;

    public window() {
        super();
        /* 创建绘图板 */
        DrawBoard drawboard = new DrawBoard();
        /* 注册 */
        guigv.drawboard = drawboard;
        drawboard.setBounds(0, 0, 1000, 1000);
        drawboard.setBorder(BorderFactory.createLineBorder(Color.black, 1));
        drawboard.setOpaque(true);
        drawboard.setBackground(Color.DARK_GRAY);

        /* 设置窗体属性 */
        setTitle("Social Force Model");
        /* 使用绝对布局 */
        setLayout(null);
        /* 设置窗体位置大小 */
        setBounds(1000, 10, 1000, 1000);

        /* 获得容器，添加控件，显示窗体 */
        Container c = getContentPane();
        c.add(guigv.drawboard);
        /* 窗体可见，窗体置顶，设置默认关闭方式 */
        setVisible(true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}


class GUI {
    public static boolean isEnd = false;

    GUI(int[][] map, Set<Loc> door, ArrayList<ArrayList<Loc>> crowdTrace) {
        brush.map = map;
        brush.door = door;
        brush.crowdTrace = crowdTrace;
    }

    public void startSimulate() {
        new window();
        brush.getObstacle();
        /**
         * 新建线程对控件画图板进行repaint,以显示连续动画效果
         * */
        Thread th = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (!isEnd) {
                guigv.drawboard.repaint();
            }
            System.out.print("Total Time = ");
            System.out.println((System.currentTimeMillis() - startTime)/1000);
        });
        th.start();
    }
}


