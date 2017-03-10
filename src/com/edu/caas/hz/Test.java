package com.edu.caas.hz;

/**
 * Created by huangzheng on 2017/3/10.
 */
public class Test {
    private int a;
    private int b;
    public void Atest(int c,int d){
//        this.a = a;
//        this.b = b;
        a = c;
        b = d;
    }
    public void printA(){
        System.out.println(a);
    }
    public void printB(){
        System.out.println(b);
    }

    public static void main(String[] args) {
        Test t = new Test();
        t.Atest(1,2);
        t.printA();
        t.printB();
    }
}
