import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

class Loc {
    double x, y;
    int nodeX, nodeY;

    Loc(double x, double y) {
        this.x = x;
        this.y = y;
        nodeX = (int) Math.round(x);
        nodeY = (int) Math.round(y);
    }

    /**
     * 各种容器的contains,remove需重写equals和hashcode方法
     */
    @Override
    public int hashCode() {
        return nodeX^nodeY*11731;
    }

    @Override
    public boolean equals(Object obj) {
        if(this==obj) return true;
        if(!(obj instanceof Loc)) return false;
        Loc a = (Loc) obj;
        return (a.nodeX == this.nodeX && a.nodeY == this.nodeY);
    }
}

public class Main {
    final static int MAX_MAP = 25;  //绘制地图的大小
    final static int MAX_CROWD = 100; //疏散人群人数
    final static int MAX_ROOM = 20; //正方形房间的边长
    final static int INFINITY = 10000000;
    final static double UNIT_TIME = 0.005;  //仿真单位时间
    public static int[][] map;  //场景图，map[i][j]=1表示对应位置有障碍物或者墙壁
    public static ArrayList<Loc> direction = new ArrayList<>();

    public static void main(String[] args) {
        /**
         * 建立场景图：墙壁、障碍物，初始化人群
         * */
        GenerateScript generateScript = new GenerateScript();
        generateScript.generateSenario(0);
        //generateScript.generateSenario(1);
        map = generateScript.getMap();
        System.out.println("set GenerateScript ok");
        /**
         * 使用A*算法计算图
         * */
        calAStar(generateScript.getDoor());
        System.out.println("cal direction ok");
        /**
         * 模型仿真,得到每一时刻的人群分布图
         * */
        SFM model = new SFM(map, generateScript.getCrowd());
        System.out.println("start simulate");
        model.startSimulate();
        System.out.println("end SFM!");
        /**
         * GUI展示
         * */
        GUI gui = new GUI(map, generateScript.getDoor(), model.getCrowdTrace());
        gui.startSimulate();
    }

    /**
     * 将map中的二维点(x，y)映射到一维 i
     * 对于房间中的一点 i(非障碍物和墙壁)，根据A*算法可得到该点移动到door的一个最短路径
     * direction(i) 记录从当前 i 点沿该最短路径移动到到下一个点 j 的方向向量
     * 这里的方向向量以loc形式给出, 即(x，y)满足sqrt(x²+y²)=1,x和y的正负代表方向
     */
    public static void calAStar(Set<Loc> door) {
        for (int i = 0; i < MAX_MAP * MAX_MAP; i++) {
            int x = i / MAX_MAP;
            int y = i % MAX_MAP;
            /**
             * 若当前讨论的点 i 位于房间内，且非障碍物
             * 则调用A*算法找到一条最短路径，返回 i到下一个点的方向向量
             * */
            Loc dir = new Loc(0.0, 0.0);
            if (x >= 1 && x <= MAX_ROOM && y <= MAX_ROOM && y >= 1 && map[x][y] == 0) {
                dir = AStar(new Loc(x, y), door);
            }
            direction.add(dir);
        }
    }

    public static Loc getDirection(Loc src) {
        return direction.get(transLoc(src));
    }

    /**
     * 二维转一维
     */
    public static int transLoc(Loc p) {
        return p.nodeX * MAX_MAP + p.nodeY;
    }

    public static Loc AStar(Loc src, Set<Loc> dstSet) {
        int[] xDirection = new int[]{0, 0, 1, -1, 1, 1, -1, -1};
        int[] yDirection = new int[]{1, -1, 0, 0, 1, -1, 1, -1};
        Set<Loc> closeSet = new HashSet<>();
        Set<Loc> openSet = new HashSet<>();
        int[] cameFrom = new int[MAX_MAP * MAX_MAP];
        double[] gScore = new double[MAX_MAP * MAX_MAP];
        double[] fScore = new double[MAX_MAP * MAX_MAP];
        ArrayList<Integer> path = new ArrayList<>(); //记录从src到dst的最短路径

        for (int i = 0; i < MAX_MAP * MAX_MAP; i++) {
            cameFrom[i] = INFINITY;
            gScore[i] = INFINITY;
            fScore[i] = INFINITY;
        }

        int srcIndex = transLoc(src);
        cameFrom[srcIndex] = srcIndex;
        gScore[srcIndex] = 0;
        fScore[srcIndex] = minManhattan(src, dstSet);
        openSet.add(src);

        while (!openSet.isEmpty()) {
            /**
             * 找出openSet中fScore最小的点
             * */
            double minFScore = INFINITY;
            Loc current = openSet.iterator().next();
            for (Loc open : openSet) {
                if (fScore[transLoc(open)] < minFScore) {
                    minFScore = fScore[transLoc(open)];
                    current = open;
                }
            }
            int currentIndex = transLoc(current);
            if (dstSet.contains(current)) {  //找到终点，生成路径
                int trace = currentIndex;
                while (trace != srcIndex) {
                    path.add(0, trace); //第一个参数是index，意味着从头部插入
                    trace = cameFrom[trace];
                }
                return getDirection(srcIndex, path.get(0)); //从src到path中第一个点的方向向量
            }
            openSet.remove(current);
            closeSet.add(current);

            Set<Loc> forbid = new HashSet<>();
            for (int i = 0; i < 8; i++) { //检查八个方向的点
                Loc neighbor = new Loc(current.nodeX + xDirection[i], current.nodeY + yDirection[i]);

                //该点不在在地图范围内
                if (neighbor.nodeX <= 0 || neighbor.nodeX >= MAX_MAP
                        || neighbor.nodeY <= 0 || neighbor.nodeY >= MAX_MAP) continue;

                //该点在closeSet中
                if (closeSet.contains(neighbor)) continue;

                //该点是障碍点，把障碍点与当前点连线垂直方向上的两个点设为不可一步到达
                if (map[neighbor.nodeX][neighbor.nodeY] == 1) {
                    //连线在水平方向
                    if (neighbor.nodeX == current.nodeX) {
                        forbid.add(new Loc(neighbor.nodeX + 1, neighbor.nodeY));
                        forbid.add(new Loc(neighbor.nodeX - 1, neighbor.nodeY));
                    } else {  //连线在竖直方向
                        forbid.add(new Loc(neighbor.nodeX, neighbor.nodeY - 1));
                        forbid.add(new Loc(neighbor.nodeX, neighbor.nodeY + 1));
                    }
                    continue;
                }

                //该点是禁止点
                if (forbid.contains(neighbor)) continue;

                double neighborGScore = gScore[currentIndex] + getDist(current, neighbor);
                int neighborIndex = transLoc(neighbor);
                //该点不在openSet中，添加
                if (!openSet.contains(neighbor)) {
                    openSet.add(neighbor);
                } else {
                    //在openSet中但不为优解
                    if (neighborGScore >= gScore[neighborIndex]) continue;
                }
                cameFrom[neighborIndex] = currentIndex;
                gScore[neighborIndex] = neighborGScore;
                fScore[neighborIndex] = neighborGScore + minManhattan(neighbor, dstSet);
            }
        }
        return (new Loc(0, 0)); //没有找到终点就退出，(0,0)表示没有下一个点的方向
    }

    /**
     * Map中a到 bSet中的点的最短曼哈顿距离
     */
    public static int minManhattan(Loc a, Set<Loc> bSet) {
        int minDist = INFINITY, tmpDist;
        for (Loc b : bSet) {
            int x = Math.abs(a.nodeX - b.nodeY);
            int y = Math.abs(a.nodeX - b.nodeY);
            tmpDist = x + y;  //曼哈顿距离
            if (tmpDist < minDist) {
                minDist = tmpDist;
            }
        }
        return minDist;
    }


    /**
     * 计算一维点a到b的方向向量
     */
    public static Loc getDirection(int a, int b) {
        int aX = a / MAX_MAP;
        int aY = a % MAX_MAP;
        int bX = b / MAX_MAP;
        int bY = b % MAX_MAP;
        double dist = Math.pow((double) (aX - bX), 2) + Math.pow((double) (aY - bY), 2);
        dist = Math.sqrt(dist);
        double x = (double) (bX - aX) / dist;
        double y = (double) (bY - aY) / dist;
        return (new Loc(x, y));
    }

    public static double getDist(Loc a, Loc b) {
        double dist = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2);
        return Math.sqrt(dist);
    }

}
