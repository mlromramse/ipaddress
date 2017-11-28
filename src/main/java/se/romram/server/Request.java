package se.romram.server;

import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by micke on 2017-11-28.
 */
public class Request {
    public Socket socket = null;
    public byte[] raw = null;
    private String url = null;
    private String method = null;
    private String httpVersion = null;
    private Map<String, String[]> headerMap = null;

    public String getLogLine() {
        return new String(raw, Charset.defaultCharset()).replaceAll("\\r", "←").replaceAll("\\n", "↓");
    }

    public String getUrl() {
        if (url == null) {
            parseHeader();
        }
        return url;
    }

    public String getHeader(String key) {
        if (headerMap == null) {
            parseHeader();
        }
        String[] valuesArr = headerMap.get(key.toLowerCase());
        return valuesArr != null ? String.join(";", valuesArr) : null;
    }

    private void parseHeader() {
        int eoh = indexOf("\r\n\r\n", raw);
        int eol = indexOf("\r\n", raw);
        String firstRow = new String(Arrays.copyOfRange(raw, 0, eol));
        String[] firstRowArr = firstRow.split(" ");
        method = firstRowArr[0];
        url = firstRowArr[1];
        httpVersion = firstRowArr[2];
        String headerSection = new String(Arrays.copyOfRange(raw, eol+2, eoh));
        String[] headerSectionArr = headerSection.split("\r\n");
        headerMap = new ConcurrentHashMap<>();
        for (String header : headerSectionArr) {
            int firstColon = header.indexOf(":");
            String key = header.substring(0, firstColon).trim();
            String[] valueArr = header.substring(firstColon+1).trim().split(";");
            headerMap.put(key.toLowerCase(), valueArr);
        }
    }

    private static int indexOf(String toFind, byte[] source) {
        return indexOf(toFind.getBytes(Charset.defaultCharset()), source);
    }

    private static int indexOf(byte[] toFind, byte[] source) {
        sourceLoop:
        for (int sourceIndex=0; sourceIndex<source.length; sourceIndex++) {
            for (byte toFindByte : toFind) {
                byte sourceByte = source[sourceIndex];
                if (toFindByte != sourceByte) {
                    continue sourceLoop;
                }
                sourceIndex++;
            }
            return sourceIndex-toFind.length;
        }
        return -1;
    }


}
