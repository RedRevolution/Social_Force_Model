import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class GenerateScript {
    private final int mapSize = Main.MAX_MAP;
    private final int roomSize = Main.MAX_ROOM;
    private final int CROWD_NUM = Main.MAX_CROWD;
    private int[][] map; //map记录整个教室构造和障碍物信息
    private ArrayList<Loc> crowd; //记录生成人群中每个人的位置
    private Set<Loc> door; //这里的房门位置是人完全走出房间后的位置

    public GenerateScript() {
        map = new int[mapSize][mapSize];
        crowd = new ArrayList<>(CROWD_NUM);
        door = new HashSet<>();
    }

    /**
     * model=0, 模拟一个无障碍物的空房间，房门在右侧中央
     * model=1, 模拟一个常见的教室场景，讲台在下侧中央，房门在下侧右方，详情见word文档
     */
    public void generateSenario(int model) {
        generateObstacle(model);
        generateDoor(model);
        generateCrowd();
    }

    public void generateObstacle(int model) {
        /**
         * 生成房间墙壁
         * 房间内壁范围： i~[1,roomSize]  j~[1,roomSize]
         * */
        for (int i = 0; i <= roomSize; i++) {
            map[0][i] = 1;  //填满map的上边框
            map[i + 1][0] = 1;  //填满map的左边框
            map[roomSize + 1][i + 1] = 1; //填满map中该房间的下边框
            map[i][roomSize + 1] = 1;  //填满map中该房间的右边框
        }

        /**
         * 模拟教室中的障碍物
         * 讲台在教室下边框中央(占两个单元)，并生成一排排座位
         * */
        if (model == 1) {
            map[roomSize - 1][roomSize / 2] = 1;
            map[roomSize - 1][1 + roomSize / 2] = 1;
            for (int j = 7; j <= 15; j++) {
                map[roomSize / 2 - 4][j] = 1;
                map[roomSize / 2 + 4][j] = 1;
            }
            /*
            for (int i = roomSize - 3; i > 1; i = i - 2) {
                for (int j = 0; j < roomSize; j++) {
                    if ((j > 1 && j <= 5) || (j > 7 && j <= 13) || (j > 15 && j <= 19)) {  //每一排座位有两个走廊通道
                        map[i][j] = 1;
                    }
                }
            }*/
        }
    }

    /**
     * model=0, 房门在下侧中央，占两个单元（这里的房门位置是人完全走出房间后的位置）
     * model=1, 房门在下侧右方
     * roomSize+1是墙壁，roomSize+2在墙壁外
     */
    public void generateDoor(int model) {
        if (model == 0) {
            door.add(new Loc(2 + roomSize, roomSize / 2));
            door.add(new Loc(2 + roomSize, 1 + roomSize / 2));
            door.add(new Loc(2 + roomSize, 2 + roomSize / 2));
            door.add(new Loc(2 + roomSize, 3 + roomSize / 2));
            map[1 + roomSize][roomSize / 2] = 0; //砸开大门处的墙壁
            map[1 + roomSize][1 + roomSize / 2] = 0;
            map[1 + roomSize][2 + roomSize / 2] = 0;
            map[1 + roomSize][3 + roomSize / 2] = 0;
        } else if (model == 1) {
            door.add(new Loc(2 + roomSize, roomSize - 2));
            door.add(new Loc(2 + roomSize, roomSize - 3));
            door.add(new Loc(2 + roomSize, roomSize - 4));
            door.add(new Loc(2 + roomSize, roomSize - 5));
            map[1 + roomSize][roomSize - 2] = 0; //砸开大门处的墙壁
            map[1 + roomSize][roomSize - 3] = 0;
            map[1 + roomSize][roomSize - 4] = 0;
            map[1 + roomSize][roomSize - 5] = 0;
        }
    }

    public void generateCrowd() {
        int[][] personMap = new int[mapSize][mapSize];
        for (int i = 0; i < CROWD_NUM; i++) {
            while (true) {
                int x = new Random().nextInt(roomSize) + 1; //生成的数据范围是[1,roomSize+1),即[1,roomSize]
                int y = new Random().nextInt(roomSize) + 1;
                if (personMap[x][y] == 0 && map[x][y] == 0) {  //有障碍物,墙壁,人的位置无法生成新的人
                    personMap[x][y] = 1;
                    crowd.add(new Loc(x, y));
                    break;
                }
            }
        }
    }

    public int[][] getMap() {
        return map;
    }

    public Set<Loc> getDoor() {
        return door;
    }

    public ArrayList<Loc> getCrowd() {
        return crowd;
    }

}
