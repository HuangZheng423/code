package sample;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangzheng on 2017/1/31.
 */
public class ThreadTest extends Thread {
    private int i ;
    private long sec;
    int count;
    public ThreadTest(int i,long sec){
        this.i = i;
        this.sec = sec;
    }
    @Override
    public void run() {
        try {
            System.out.println("thread "+ i +" start");
            for (int j = 0; j < 10; j++) {
                count++;
                Thread.sleep(sec);
            }
            System.out.println("thread "+ i +" end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
