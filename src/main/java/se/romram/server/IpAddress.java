package se.romram.server;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by micke on 2015-12-28.
 */
public class IpAddress extends Thread {
    private static Executor executor = Executors.newFixedThreadPool(4);
    int port;
    String messageFormat = "HTTP/1.0 200 OK\nServer: MicroServer\nContent-Length: %s\nContent-type: text/html; charset=UTF-8\n\n%s";
    private static boolean logon = false;


    public IpAddress(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws IOException {
        new IpAddress(8686).run();
    }

    public void run() {
        try {
            System.out.println(String.format("The server is up, monitoring port %s", port));
            String payload = "<html><body><h1>It works!</h1></body></html>\n\n";
            byte[] favicon = "favicon.ico".getBytes();

            byte[] icon = Base64.decode("AAABAAEAEBAQAAEABAAoAQAAFgAAACgAAAAQAAAAIAAAAAEABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAfX19AP///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAiIiIiIiIiAgIiIiIiIiAiICICIiAiAiIiAgIiICAiIiIhAiIgEiIiIAACIiAAAiIiIiIiIiIiIiIiIhEiIiIiIiIiESIiIiIiIiIiIiIiIiAAAiIgAAIiIiECIiASIiIiAgIiICAiIiAiAiIgIgIiAiIiIiIiICAiIiIiIiIiAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            int length = payload.getBytes().length;
            //byte[] message = String.format(messageFormat, length, payload).getBytes();
            byte[] iconHeader = String.format("HTTP/1.0 200 OK\nServer: MicroServer\nContent-Length: %s\nContent-type: image/x-icon\n\n",
                    icon.length
            ).getBytes();
            long count = 0;
            //System.out.println(new String(message));
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                final Socket socket = serverSocket.accept();
                count++;
                final long c = count;
                socket.setSoTimeout(10000);
                Runnable runnable = () -> {
                    List<String> log = new ArrayList<>();
                    try {
                        Request request = new Request();
                        request.raw = readAll(socket.getInputStream());
                        log.add("" + System.currentTimeMillis() + " - " + c + " - " + request.getLogLine());
                        printLogIf(log.size() > 1000, log);
                        if (request.getUrl().equals("/favicon.ico")) {
                            writeBinary(iconHeader, icon, socket.getOutputStream());
                        } else {
                            String ipPayload = request.getHeader("X-Forwarded-For");
                            if (ipPayload == null) {
                                ipPayload = socket.getLocalAddress().getHostAddress();
                            }
                            byte[] message = String.format(messageFormat, ipPayload.getBytes(Charset.defaultCharset()).length, ipPayload).getBytes(Charset.defaultCharset());
                            writeBinary(message, null, socket.getOutputStream());
                        }
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        printLogIf(true, log);
                    }
                };
                executor.execute(runnable);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printLogIf(boolean fulfilled, List<String> log) {
        if (logon && fulfilled) {
            for (String row : log) {
                System.out.println(row);
            }
            log.clear();
        }
    }

    private static byte[] readAll(InputStream inputStream) {
        byte[] bytes = new byte[8000];
        int res = 0;
        try {
            while ((res = inputStream.read(bytes)) != -1) {
                if (res > 0 && res < bytes.length) return Arrays.copyOf(bytes, res);
            }
        } catch (IOException e) {
            System.out.format("BufferLength: %s, Last res: %s\nBuffer: %s\n", bytes.length, res, new String(bytes).replaceAll(" ", "·").replaceAll("\n", "↓").replaceAll("\r", "←"));
            e.printStackTrace();
        }
        return new byte[0];
    }

    private static void writeBinary(byte[] header, byte[] payload, OutputStream outputStream) {
        try {
            outputStream.write(header);
            if (payload != null) {
                outputStream.write(payload);
            }
            outputStream.flush();
            //outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
