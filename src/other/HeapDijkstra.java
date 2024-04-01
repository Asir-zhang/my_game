package other;


import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * 基于堆优化的Dijkstra算法
 */
public class HeapDijkstra {
    // 优先队列模拟堆
    static PriorityQueue<Point> heap = new PriorityQueue<>();

    static class Point {
        int index;      // 点的索引
        int distance;       // 点到源点的距离

        public Point(int index, int distance) {
            this.index = index;
            this.distance = distance;
        }
    }

    static int N = 100010;
    static int idx = 0;     // 邻接表中的索引
    static int[] dist = new int[N];     // 存储每个点到源点的最短距离
    static boolean[] st = new boolean[N];   // 存储每个点是否已经确定了最短距离
    static int[] h = new int[N];    // 存储每个点的第一条边
    static int[] e = new int[N];    // 存储每条边的终点
    static int[] ne = new int[N];       // 存储每个点的下一条边
    static int[] w = new int[N];        // 存储每条边的权重

    public static void add(int a, int b, int w) {
        e[idx] = b;
        ne[idx] = h[a];
        h[a] = idx;
        HeapDijkstra.w[idx++] = w;
    }

    public static int dijkstra(int n) {
        Point start = new Point(1, 0);
        heap.add(start);
        while (!heap.isEmpty()) {
            Point t = heap.poll();
            int index = t.index;
            int distance = t.distance;

            if (st[index]) {
                continue;
            }

            st[index] = true;

            // 遍历所有与index相连的点
            for (int i = h[index]; i != -1; i = ne[i]) {
                int j = e[i];
                // 如果当前点到源点的距离大于distance + w[i]，则更新dist[j]
                if (dist[j] > distance + w[i]) {
                    dist[j] = distance + w[i];
                    heap.add(new Point(j, dist[j]));
                }
            }
        }

        if (dist[n] == Integer.MAX_VALUE) {
            return -1;
        }

        return dist[n];
    }

    public static void main(String[] args) {
        // 初始化dist无穷大
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[1] = 0;


    }
}
