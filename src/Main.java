import com.google.gson.Gson;
import java.io.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static File             file;
    private static List<Character>  illeteral;

    private static final int        MAX_LITERAL;
    private static final int        MIN_LITERAL;
    private static final String     GET_URL;
    private static final String     SEND_URL;

    static {
        GET_URL = "https://api.codenation.dev/v1/challenge/dev-ps/generate-data?token=0d67bca1cbc630fdeeb6a4ace6b5963b2224261d";
        SEND_URL = "https://api.codenation.dev/v1/challenge/dev-ps/submit-solution?token=0d67bca1cbc630fdeeb6a4ace6b5963b2224261d";
        MIN_LITERAL = 97;
        MAX_LITERAL = 122;
        file        = new File("C:\\Users\\Nescara\\Desktop\\answer.json");
        illeteral   = new ArrayList<>(){{
            add('.');
            add('?');
            add('!');
            add(',');
            add(':');
            add('/');
            add('\\');
            add('\'');
            add('"');
            add('{');
            add('}');
            add('[');
            add(']');
            add('(');
            add(')');
            add(' ');
        }};
    }

    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        PrintWriter writer = new PrintWriter(file);
        file.createNewFile();

        String json = getJson();

        if(json.length() < 0)
            return;

        writer.print(json);
        writer.flush();
        writer.close();

        ResponseData data;
        data = gson.fromJson(json, ResponseData.class);

        writer = new PrintWriter(file);

        decrypt(data);

        data.resumo_criptografico = String.format("%040x", new BigInteger(1, new SHA1().digest(data.decifrado.getBytes())));

        json = gson.toJson(data);

        writer.print(json);
        writer.flush();
        writer.close();

        //sendJson(file);
    }

    private static String getJson() throws IOException {
        String resp = "";

        HttpRequest request = new HttpRequest(GET_URL, "GET");
        request.fireRequest();

        if(request.getResponse().response_body.length() > 0 && request.getResponse().http_code == 200)
            resp = request.getResponse().response_body;
        else
            System.out.println(request.getResponse().http_code);

        request.close();
        return resp;
    }

    private static void sendJson(File file) throws IOException {
        Map<String, String> headers = new HashMap<>(){{
            put("Content-Length", String.valueOf(file.length()));
            put("Content-Type", "multipart/form-data");
        }};

        HttpRequest request = new HttpRequest(SEND_URL, "POST");
        request.setHeaders(headers);
        request.setParameters(getForm());
        request.fireRequest();
        HttpRequest.ResponseDatas response = request.getResponse();

        System.out.println(response.http_code);
        request.close();
    }

    private static void decrypt(ResponseData encrypted) {
        byte[] byteStr = encrypted.cifrado.getBytes();
        char currentChar;

        for(int i = 0; i < byteStr.length; i++) {
            currentChar = encrypted.cifrado.charAt(i);
            if(illeteral.contains(currentChar))
                continue;

            byteStr[i] -= encrypted.numero_casas;

            if(byteStr[i] < MIN_LITERAL) {
                byteStr[i] = (byte)(MAX_LITERAL - ((MIN_LITERAL - byteStr[i])-1));
            }
        }

        encrypted.decifrado = new String(byteStr);
    }

    private static String getForm() {
        return
                "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"answer\"; filename=\"C:\\Users\\Nescara\\Desktop\\answer.json\"\r\nContent-Type: application/json\r\n\r\n\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--";
    }

    private static class ResponseData {
        int numero_casas;
        String token;
        String cifrado;
        String decifrado;
        String resumo_criptografico;
    }

}
