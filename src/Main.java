import com.google.gson.Gson;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;

@SuppressWarnings("All")
public class Main {

    private static File             file;
    private static List<Character>  illeteral;

    private static final int        MAX_LITERAL;
    private static final int        MIN_LITERAL;
    private static final String     GET_URL;
    private static final String     SEND_URL;

    static {
        GET_URL     = "https://api.codenation.dev/v1/challenge/dev-ps/generate-data?token=0d67bca1cbc630fdeeb6a4ace6b5963b2224261d";
        SEND_URL    = "https://api.codenation.dev/v1/challenge/dev-ps/submit-solution?token=0d67bca1cbc630fdeeb6a4ace6b5963b2224261d";
        MIN_LITERAL = 97;
        MAX_LITERAL = 122;
        file        = new File("C:\\Users\\Nescara\\Desktop\\answer.json");
        illeteral   = new ArrayList<>(29){{
            add('.');  add('?');  add('!');  add(',');
            add(':');  add('/');  add('\\'); add('\'');
            add('=');  add('-');  add('%');  add('"');
            add('{');  add('}');  add('[');  add(']');
            add('(');  add(')');  add(' ');  add('0');
            add('1');  add('2');  add('3');  add('4');
            add('5');  add('6');  add('7');  add('8');
            add('9');
        }};
    }

    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        PrintWriter writer = new PrintWriter(file);
        file.createNewFile();

        String json = getJson();

        if(json.length() == 0)
            return;

        ResponseData data;
        data = gson.fromJson(json, ResponseData.class);

        decrypt(data);
        data.resumo_criptografico = String.format("%040x", new BigInteger(1, new SHA1().digest(data.decifrado.getBytes())));

        json = gson.toJson(data);

        writer.print(json);
        writer.flush();
        writer.close();

        sendJson(file);
    }

    private static String getJson() throws IOException {
        String resp = "";

        HttpRequest request = new HttpRequest(GET_URL, "GET");
        request.fireRequest();

        if(request.getResponse().response_body.length() > 0 && request.getResponse().http_code == 200)
            resp = request.getResponse().response_body;
        else
            System.out.println("Error dispatching request: " + request.getResponse().http_code);

        request.close();
        return resp;
    }

    private static void sendJson(File file) throws IOException {
        URL url = new URL(SEND_URL);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

        con.setDoOutput(true);
        con.setDoInput(true);

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Length", String.valueOf(file.length()));
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=WebKitFormBoundary");

        OutputStream oStream = con.getOutputStream();

        oStream.write(
            (
                "WebKitFormBoundary\r\r\n\n" +
                "Content-Disposition: form-data; name=\"answer\"; filename=" + file.getName() + "\"\r\n" +
                "Content-Type: application/json\r\r\n\n"
            ).getBytes()
        );

        oStream.write(getFileAsByteArray(file));
        oStream.write("\n\n\r\r".getBytes());

        oStream.write("WebKitFormBoundary".getBytes());
        oStream.flush();
        oStream.close();

        System.out.println(con.getResponseCode());
        System.out.println(getResponse(con));
        System.out.println(oStream.toString());
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

    private static byte[] getFileAsByteArray(File file) {
        FileInputStream stream;
        byte[] arrayFile = new byte[(int)file.length()];
        try {
            stream = new FileInputStream(file);
            stream.read(arrayFile);
            stream.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        return arrayFile;
    }

    private static String getResponse(HttpsURLConnection con) throws IOException {
        BufferedReader in = null;
        String inLine;

        if(con.getResponseCode() >= 200 && con.getResponseCode()< 300)
            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        else if(con.getErrorStream() != null)
            in = new BufferedReader(new InputStreamReader(con.getErrorStream()));

        StringBuffer response = new StringBuffer();

        while(in != null && (inLine = in.readLine()) != null)
            response.append(inLine);

        return response.toString();
    }

    private static class ResponseData {
        int numero_casas;
        String token;
        String cifrado;
        String decifrado;
        String resumo_criptografico;
    }
}
