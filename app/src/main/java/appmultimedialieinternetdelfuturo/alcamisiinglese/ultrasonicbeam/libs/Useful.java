package appmultimedialieinternetdelfuturo.alcamisiinglese.ultrasonicbeam.libs;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


/**
 * Created by Pietro on 25/07/2016.
 */
public class Useful {
    Queue<Float> queue = new ConcurrentLinkedQueue<>();

    /**
     * Add data to a queue(s) for consumption
     */
    public void Put(float[] data) throws InterruptedException {
        for (float element : data)
            queue.add(element);
    }

    public float[] Take(int size) throws InterruptedException {
        float[] result = new float[size];
        for (int i = 0; i < size; i++)
            result[i] = queue.poll();
        return result;
    }

    public void EmptyQueue() throws InterruptedException {
        for (float element : queue) {
            queue.remove();
        }
    }

    public int Size() {
        return queue.size();
    }

}