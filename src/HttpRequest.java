import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

public class HttpRequest {

    private String                  parameters;
    private Map<String, String>     requestHeaders;
    private HttpURLConnection       connection;
    private ResponseDatas           response;
    private int                     httpCode;

    public final int NO_TIMEOUT = 0;

    public HttpRequest(String url, String method) {
        try {
            createConnection(url, method);
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void createConnection(String url, String method) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if(!isValidConnection(url))
            url = "http://" + url;

        HttpURLConnection connection;
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(NO_TIMEOUT);
        connection.setReadTimeout(NO_TIMEOUT);
        connection.setInstanceFollowRedirects(true);
        connection.setDoInput(true);

        if(connection instanceof HttpsURLConnection) {
            SSLContext ssl = SSLContext.getInstance("TLSv1.2");
            ssl.init(null, null, new SecureRandom());

            ((HttpsURLConnection) connection).setSSLSocketFactory(ssl.getSocketFactory());
        }

        if(!method.equals("GET"))
            connection.setDoOutput(true);

        this.connection = connection;
    }

    public HttpRequest setHeaders(Map<String, String> headers) {
        if(headers == null)
            return this;

        this.requestHeaders = headers;

        for (String key : headers.keySet()) {
            connection.setRequestProperty(key, headers.get(key));
        }

        return this;
    }

    public HttpRequest setHeaders(String headers) throws ArrayIndexOutOfBoundsException {
        if(headers == null)
            return this;

        String[] headersArr = headers.split("&");
        for (String headerStr : headersArr) {
            String[] headerArr = headerStr.split(":");
            connection.setRequestProperty(headerArr[0], headerArr[1]);
        }

        return this;
    }

    public HttpRequest setParameters(String params) throws IOException {
        if(params == null || getRequestMethod().equals("GET"))
            return this;

        this.parameters = params;
        OutputStream os = connection.getOutputStream();
        os.write(params.getBytes());
        os.flush();

        return this;
    }

    public String getParameters() {
        return parameters;
    }

    public HttpRequest fireRequest() throws IOException {
        ResponseDatas data = new ResponseDatas();
        httpCode = connection.getResponseCode();

        switch(httpCode) {
            case HttpURLConnection.HTTP_CREATED:
            case HttpURLConnection.HTTP_OK:
                data.response_body = getResponseBody();
                break;
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_SEE_OTHER:
                data.response_body = connection.getResponseMessage() + " to " + connection.getHeaderField("Location");
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                data.response_body = "";
                data.errors = connection.getResponseMessage();
                break;
            case HttpURLConnection.HTTP_NO_CONTENT:
                data.response_body = connection.getResponseMessage();
                break;
            default:
                data.response_body ="";
                data.errors = getResponseBody();
        }

        data.http_code = httpCode;
        data.response_headers = connection.getHeaderFields();
        response = data;

        return this;
    }

    private String getResponseBody() throws IOException {
        BufferedReader in = null;
        String inLine;

        if(httpCode >= 200 && httpCode < 300)
            in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        else if(connection.getErrorStream() != null)
            in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));

        StringBuffer response = new StringBuffer();

        while(in != null && (inLine = in.readLine()) != null)
            response.append(inLine);

        if(in != null)
            in.close();

        return response.toString();
    }

    public String getUrl() {
        return connection.getURL().toString();
    }

    public String getRequestMethod() {
        return connection.getRequestMethod();
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public ResponseDatas getResponse() {
        return response;
    }

    private boolean isValidConnection(String url) {
        return (url.contains("https://") || url.contains("http://"));
    }

    public HttpRequest close() {
        connection.disconnect();
        return this;
    }

    @Override
    public String toString() {
        return "URL: " + getUrl() +
                "\n Method: " + getRequestMethod() +
                "\n Parameters: " + getParameters() +
                "\n Http code: " + httpCode +
                (getRequestHeaders() != null ? "\n Headers: " + getRequestHeaders().toString() : "");
    }

    public class ResponseDatas {

        public String                       response_body, errors;
        public Map<String, List<String>>    response_headers;
        public int                          http_code;

        public ResponseDatas() {
            response_body = "";
            http_code = 0;
            response_headers = new HashMap<>();
        }

        public ResponseDatas(String message, Integer http_code) {
            this();
            this.http_code = http_code;

            if(http_code >= 100 && http_code < 400)
                response_body = message;
            else
                errors = message;
        }
    }
}