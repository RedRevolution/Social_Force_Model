import java.util.ArrayList;

public class SFM {
    private final double MI = 80.0;  //行人i的质量
    private final double BI = 0.08;
    private final double K = 120000;
    private final double KAPPA = 240000;
    private final double R = 0.25; //个体半径
    private int[][] map;
    private ArrayList<Loc> crowd;
    private int crowdNum = Main.MAX_CROWD;
    private ArrayList<ArrayList<Loc>> crowdTrace;
    private int[] out;
    private ArrayList<Loc> vt;

    /**
     * crowdTrace:二维动态表,crowdTrace[i][j]记录第i时刻第j个人的位置
     * out:out[i]=1表示第i个人已经离开房间
     * vt:速度表, 速度向量Loc(vx,vy),vx的绝对值表示x方向上速度大小,正负表示方向
     */


    public SFM(int[][] map, ArrayList<Loc> crowd) {
        this.map = map;
        this.crowd = crowd;
        crowdTrace = new ArrayList<>();
        out = new int[Main.MAX_CROWD];
        vt = new ArrayList<>();
        for (int i = 0; i < Main.MAX_CROWD; i++) {
            vt.add(new Loc(0.0, 0.0));
        }
    }

    public ArrayList<ArrayList<Loc>> getCrowdTrace() {
        return crowdTrace;
    }

    public void startSimulate() {
        while (crowdNum > 0) {
            /**
             * 每次循环，初始人群位置加入crowdTrace
             * */
            crowdTrace.add((ArrayList<Loc>) crowd.clone());

            ArrayList<Loc> newCrowd = new ArrayList<>();
            for (int i = 0; i < crowd.size(); i++) {
                Loc curLoc = crowd.get(i);
                if (out[i] == 1) {
                    newCrowd.add(curLoc);
                    continue;
                }
                Loc totalForce = calTotalForce(i);
                double aX = totalForce.x / MI;
                double aY = totalForce.y / MI;
                double vX = vt.get(i).x;
                double vY = vt.get(i).y;
                /**
                 * 下一个UNIT时间段内的平均速度
                 * */
                double newVX = vX + aX * Main.UNIT_TIME / 2;
                double newVY = vY + aY * Main.UNIT_TIME / 2;
                vt.set(i, new Loc(newVX, newVY));
                double newX = curLoc.x + newVX * Main.UNIT_TIME;
                double newY = curLoc.y + newVY * Main.UNIT_TIME;
                Loc newLoc = new Loc(newX, newY);
                newCrowd.add(newLoc);
                /**
                 * a.离开了房间范围
                 * b.到达了门外
                 * c.进入了障碍物
                 * */
                if ((newLoc.nodeX < 0 || newLoc.nodeX >= Main.MAX_ROOM + 1 || newLoc.nodeY < 0 || newLoc.nodeY >= Main.MAX_ROOM + 1) ||
                        (newLoc.x >= 1.5 + Main.MAX_ROOM) ||
                        (map[newLoc.nodeX][newLoc.nodeY] == 1)) {
                    crowdNum--;
                    out[i] = 1;
                }
            }
            crowd = newCrowd;
        }
    }

    private Loc calTotalForce(int i) {
        Loc curLoc = crowd.get(i);
        Loc cur = new Loc(curLoc.nodeX, curLoc.nodeY);
        /**
         * 主驱动力：
         * VI0：行人 i 运动期望方向的运动期望速度
         * vi0X: 期望望运动速度在x方向上的分量
         * viX：实际速度在x方向上的分量
         * TAU：松弛时间
         * individualX：主驱动力在x方向上的分量
         * */
        double VI0 = 1.2;
        Loc direction = Main.getDirection(cur);
        double vi0X = VI0 * direction.x;
        double viX = vt.get(i).x;
        double TAU = 0.5;
        double individualX = MI * (vi0X - viX) / TAU;
        double vi0Y = VI0 * direction.y;
        double viY = vt.get(i).y;
        double individualY = MI * (vi0Y - viY) / TAU;

        Loc forceFromPeople = calForceFromPeople(i);
        Loc forceFromWall = calForceFromWall(i, direction);

        double totalXForce = individualX + forceFromPeople.x + forceFromWall.x;
        double totalYForce = individualY + forceFromPeople.y + forceFromWall.y;
        return (new Loc(totalXForce, totalYForce));
    }

    /**
     * 行人间的相互作用力
     * AI:社会力模型常量形参数
     * rij:行人i的个体半径与行人j的个体半径之和
     * dij:行人间间距
     * nij:行人j指向行人i的单位方向向量(行人间切线方向上的单位向量)
     * psy:行人间过度拥挤的现象而产生的心理作用力
     * phy:行人受到的压力和滑动摩擦力
     * G(dij,rij):若dij>rij,则距离压迫感为0,否则为rij-dij
     * deltaVji:行人切线方向上的速度差
     */
    private Loc calForceFromPeople(int i) {
        double AI = 2000.0;
        double totalXForce = 0.0;
        double totalYForce = 0.0;
        for (int j = 0; j < crowd.size(); j++) {
            if (j == i || out[j] == 1) continue;
            Loc iLoc = crowd.get(i);
            Loc jLoc = crowd.get(j);
            double rij = 2 * R;
            double dij = getDIJ(iLoc, jLoc);
            double nij1 = (iLoc.x - jLoc.x) / dij;
            double nij2 = (iLoc.y - jLoc.y) / dij;
            double psy1 = AI * Math.pow(Math.E, (rij - dij) / BI);
            double psy2 = K * G(dij, rij);
            double psyX = (psy1 + psy2) * nij1;
            double psyY = (psy1 + psy2) * nij2;

            double vjiX = vt.get(j).x - vt.get(i).x;
            double vjiY = vt.get(j).y - vt.get(i).y;
            double deltaVji = -vjiX * nij2 + vjiY * nij1;
            double phyX = -KAPPA * G(dij, rij) * deltaVji * nij2;
            double phyY = KAPPA * G(dij, rij) * deltaVji * nij1;

            totalXForce += (psyX + phyX);
            totalYForce += (psyY + phyY);
        }
        return (new Loc(totalXForce, totalYForce));
    }

    /**
     * 行人与障碍物之间的相互作用力
     * AI：社会力模型常量形参数
     * diw:行人i与障碍物 w之间的距离
     * niw:行人指向障碍物 w的单位向量
     * tiw:切线向量
     * direct: 0:行人上方的障碍物, 1:下，2：左，3：右
     */
    private Loc calForceFromWall(int i, Loc direction) {
        double AI = 2000.0;
        Loc curLoc = crowd.get(i);
        double totalXForce = 0.0;
        double totalYForce = 0.0;
        for (int direct = 0; direct < 4; direct++) {
            double diw = 0.0;
            double niwX = 0.0;
            double niwY = 0.0;
            double tiwX = 0.0;
            double tiwY = 0.0;
            if (direct == 0) {
                for (int index = curLoc.nodeY + 1; index <= Main.MAX_ROOM + 1; index++) {
                    if (map[curLoc.nodeX][index] != 0) {
                        diw = (double) index - 0.5 - curLoc.y;
                        niwY = -1.0;
                        tiwX = (direction.x >= 0) ? -1.0 : 1.0;
                        break;
                    }
                }
            } else if (direct == 1) {
                for (int index = curLoc.nodeY - 1; index >= 0; index--) {
                    if (map[curLoc.nodeX][index] != 0) {
                        diw = curLoc.y - 0.5 - (double) index;
                        niwY = 1.0;
                        tiwX = (direction.x >= 0) ? -1.0 : 1.0;
                        break;
                    }
                }
            } else if (direct == 2) {
                for (int index = curLoc.nodeX - 1; index >= 0; index--) {
                    if (map[index][curLoc.nodeY] != 0) {
                        diw = curLoc.x - 0.5 - (double) index;
                        niwX = 1.0;
                        tiwY = (direction.y >= 0) ? -1.0 : 1.0;
                        break;
                    }
                }
            } else {
                for (int index = curLoc.nodeX + 1; index <= Main.MAX_ROOM + 1; index++) {
                    if (map[index][curLoc.nodeY] != 0) {
                        diw = (double) index - 0.5 - curLoc.x;
                        niwX = -1.0;
                        tiwY = (direction.x >= 0) ? -1.0 : 1.0;
                        break;
                    }
                }
            }
            double tmp1 = AI * Math.pow(Math.E, (R - diw) / BI);
            double tmp2 = K * G(diw, R);
            double wallXForce = (tmp1 + tmp2) * niwX;
            double wallYForce = (tmp1 + tmp2) * niwY;
            double delta = vt.get(i).x * tiwX + vt.get(i).y * tiwY;
            double slidingXForce = KAPPA * G(diw, R) * delta * tiwX;
            double slidingYForce = KAPPA * G(diw, R) * delta * tiwY;
            totalXForce += (wallXForce + slidingXForce) * 0.01;
            totalYForce += (wallYForce + slidingYForce) * 0.01;
        }
        return (new Loc(totalXForce, totalYForce));
    }


    private double getDIJ(Loc i, Loc j) {
        double dij = Math.sqrt(Math.pow((i.x - j.x), 2) + Math.pow((i.y - j.y), 2));
        return dij;
    }

    private double G(double dij, double rij) {
        double g = (dij > rij) ? 0.0 : rij - dij;
        return g;
    }

}
