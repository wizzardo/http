package com.wizzardo.httpserver;

import com.wizzardo.epoll.Connection;
import com.wizzardo.epoll.readable.ReadableData;
import com.wizzardo.httpserver.request.Header;
import com.wizzardo.httpserver.request.RequestReader;
import com.wizzardo.httpserver.request.Request;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

/**
 * @author: wizzardo
 * Date: 3/14/14
 */
public class HttpConnection extends Connection {
    private volatile byte[] data = new byte[1024];
    private volatile int r = 0;
    private volatile int position = 0;
    private volatile Request request;
    private boolean headerReady = false;
    private RequestReader headersReader;

    public HttpConnection(int fd, int ip, int port) {
        super(fd, ip, port);
    }

    int getBufferSize() {
        return data.length - position;
    }

    public boolean check(ByteBuffer bb) {
        int limit = bb.limit();
        bb.get(data, 0, limit);
        if (headersReader == null)
            headersReader = new RequestReader(new LinkedHashMap<String, HeaderValue>(20));

        int i = headersReader.read(data, 0, limit);

        if (i < 0)
            return false;
        position = i;
        r = limit;
        request = new Request(this, headersReader.getHeaders(), headersReader.getMethod(), headersReader.getPath());
        headerReady = true;
        return true;
    }

    public boolean isRequestReady() {
        return headerReady;
    }

    public void reset(String reason) {
        position = 0;
        headerReady = false;
        r = 0;
        headersReader = null;
    }

    @Override
    public void onWriteData(ReadableData readable, boolean hasMore) {
        if (!Header.VALUE_CONNECTION_KEEP_ALIVE.value.equalsIgnoreCase(request.header(Header.KEY_CONNECTION.value))) {
            close();
        }
    }

    public RequestReader getHeadersReader() {
        return headersReader;
    }

    public Request getRequest() {
        return request;
    }
}
