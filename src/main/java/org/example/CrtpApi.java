package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;


import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CrtpApi {
    private Lock lock = new ReentrantLock();
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long timeUnitMillis;
    private int requestLimit;

    public CrtpApi(TimeUnit timeUnit, int requestLimit){
        this.timeUnitMillis = timeUnit.toMillis(1);
        this.requestLimit = requestLimit;
    }

    public void createDocument(Object document, String signature) {

        if (!tryAcquireLock()) {
            awaitAvailableSlot();
        }

        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        try {
            String json = ow.writeValueAsString(document);

                URL url = new URL("https://ismp.crpt.ru/api/v3/lk/documents/create");

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");

                connection.setRequestProperty("Content-Type", "application/json");

                connection.setDoOutput(true);
                connection.setDoInput(true);

                OutputStream outputStream = connection.getOutputStream();

                outputStream.write(json.getBytes());
                outputStream.flush();
                outputStream.close();

                connection.disconnect();

        } catch (Exception e) {

        } finally {
            releaseLock();
        }
    }

    private boolean tryAcquireLock() {
        lock.lock();
        int count = requestCount.incrementAndGet();
        if (count > requestLimit) {
            lock.unlock();
            return false;
        }
        return true;
    }

    private void awaitAvailableSlot() {
        try {
            Thread.sleep(timeUnitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void releaseLock() {
        lock.lock();
        requestCount.decrementAndGet();
        lock.unlock();
    }
}
