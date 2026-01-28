package exercises;

import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class ComplexCalculation {
    public BigInteger calculateResult(BigInteger base1, BigInteger power1, BigInteger base2, BigInteger power2) throws InterruptedException{
        BigInteger result = new BigInteger("0");

        List<PowerCalculatingThread> threads = new ArrayList<>();
        threads.add(new PowerCalculatingThread(base1, power1));
        threads.add(new PowerCalculatingThread(base2, power2));

        for(PowerCalculatingThread t: threads){
            t.start();
            t.join();
        }

        for(PowerCalculatingThread t: threads){
            result = result.add(t.getResult());
        }

        return result;
    }

    private static class PowerCalculatingThread extends Thread {
        private BigInteger result = BigInteger.ONE;
        private BigInteger base;
        private BigInteger power;

        public PowerCalculatingThread(BigInteger base, BigInteger power) {
            this.base = base;
            this.power = power;
        }

        @Override
        public void run() {
            for(long i = power.intValue(); i>0; i--){
                result = result.multiply(base);
            }
            ReentrantReadWriteLock.ReadLock a = new ReentrantReadWriteLock().readLock();
        }

        public BigInteger getResult() {
            return result;

        }
    }

    public static void main(String[] args) throws InterruptedException {
        ComplexCalculation cc = new ComplexCalculation();
        BigInteger base1 = new BigInteger("2");
        BigInteger power1 = new BigInteger("10");
        BigInteger base2 = new BigInteger("3");
        BigInteger power2 = new BigInteger("5");

        BigInteger result = cc.calculateResult(base1, power1, base2, power2);
        System.out.println("Result: " + result); // Expected: 1024 + 243 = 1267
    }
}