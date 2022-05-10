package com.train.gccn;




import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import com.google.gson.Gson;
import com.train.gccn.client.GCCNClient;


public class GCCNTest {


    public class SSOJSON {
        String issuer;
        String claim;
    }


    @Test
    public  void main() {

        /* Extract from JSON */
        try {
            String json = new String ( Files.readAllBytes( Paths.get("test-gccn.json") ) );
            Gson gson = new Gson();
            SSOJSON SSOInput = gson.fromJson(json, SSOJSON.class);
            System.out.println("---- JSON -----");
            System.out.println(SSOInput.issuer);
            System.out.println(SSOInput.claim);

            GCCNClient client = new GCCNClient();
            GCCNClient.GCCNResponse Resp = client.VerifyIdentity(SSOInput.issuer,SSOInput.claim);
            System.out.println("SSIClient Verification Status: " + String.valueOf(Resp.VerificationStatus));



        }catch(IOException e){
            System.out.println("JSON parse error" + e.getMessage());
        }



    }
}
