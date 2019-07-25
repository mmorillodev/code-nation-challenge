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

    private String                  url;
    private String                  method;
    private StringBuilder           parameters;
    private HttpURLConnection       connection;
    private ResponseData            response;
    private Object                  headers;
    private int                     httpCode;

    public final int NO_TIMEOUT = 0;

    public HttpRequest(String url, String method) {
        this.parameters = new StringBuilder();
        this.url        = url;
        this.method     = method;
    }

    private void createConnection(String url, String method) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        if(!isValidConnection(url))
            url = "http://" + url;

        this.connection = (HttpURLConnection) new URL(url).openConnection();
        this.connection.setRequestMethod(method);
        this.connection.setConnectTimeout(NO_TIMEOUT);
        this.connection.setReadTimeout(NO_TIMEOUT);
        this.connection.setInstanceFollowRedirects(true);
        this.connection.setDoInput(true);

        if(this.connection instanceof HttpsURLConnection) {
            SSLContext ssl = SSLContext.getInstance("TLSv1.2");
            ssl.init(null, null, new SecureRandom());

            ((HttpsURLConnection) this.connection).setSSLSocketFactory(ssl.getSocketFactory());
        }

        if(this.headers != null) {
            if(this.headers instanceof String) {
                String[] headersArr = ((String) headers).split("&");
                for (String headerStr : headersArr) {
                    String[] headerArr = headerStr.split(":");
                    this.connection.setRequestProperty(headerArr[0], headerArr[1]);
                }
            }
            else if(this.headers instanceof Map) {
                for (String key : ((Map<String, String>) this.headers).keySet()) {
                    this.connection.setRequestProperty(key, ((Map<String, String>) this.headers).get(key));
                }
            }
        }

        if(!method.equals("GET") && this.parameters != null) {
            this.connection.setDoOutput(true);
            OutputStream os = this.connection.getOutputStream();
            os.write(this.parameters.toString().getBytes());
            os.flush();
        }
    }

    public HttpRequest setHeaders(Map<String, String> headers) {
        if(headers == null)
            return this;

        this.headers = headers;
        return this;
    }

    public HttpRequest setHeaders(String headers) {
        if(headers == null)
            return this;

        this.headers = headers;
        return this;
    }

    public HttpRequest setParameters(String params) {
        if(params == null || method.equals("GET"))
            return this;

        this.parameters.append(params);
        return this;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParameters() {
        return parameters.toString();
    }

    public HttpRequest fireRequest() throws IOException {
        try {
            createConnection(this.url, this.method);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        ResponseData data = new ResponseData();
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

        return response.toString();
    }

    public String getUrl() {
        return connection.getURL().toString();
    }

    public String getRequestMethod() {
        return connection.getRequestMethod();
    }

    public Object getRequestHeaders() {
        return headers;
    }

    public ResponseData getResponse() {
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

    public class ResponseData {

        public String                       response_body, errors;
        public Map<String, List<String>>    response_headers;
        public int                          http_code;

        public ResponseData() {
            response_body = "";
            http_code = 0;
            response_headers = new HashMap<>();
        }

        public ResponseData(String message, Integer http_code) {
            this();
            this.http_code = http_code;

            if(http_code >= 100 && http_code < 400)
                response_body = message;
            else
                errors = message;
        }

        public String toString() {
            return "[response_body] => " + response_body + "\n" +
                    "[http_code] => " + http_code + "\n" +
                    "[Errors] => " + errors + "\n" +
                    "[response_headers] => " + response_headers.toString();
        }
    }
}