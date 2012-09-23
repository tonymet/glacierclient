import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;


public class GlacierClient {
    public static String vaultName = "test1";
    public static String archiveToUpload = null;
    public static final int ERROR_ARGS = 2;
    public static final int ERROR_DOWNLOAD = 3;
    public static AmazonGlacierClient client;
    private static File archiveFile = null;
    
    public static void main(String[] args) throws IOException {
        try{
        	archiveToUpload = args[0];
        }
        catch (Exception e){
        	usage();
        	System.exit(ERROR_ARGS);
        }
        AWSCredentials credentials = new PropertiesCredentials(
                GlacierClient.class.getResourceAsStream("AwsCredentials.properties"));
        client = new AmazonGlacierClient(credentials);
        client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");

        try {
        	archiveFile = new File(archiveToUpload);
            ArchiveTransferManager atm = new ArchiveTransferManager(client, credentials); 
            UploadResult result = atm.upload(vaultName, "my archive " + (new Date()), archiveFile);
            System.out.println("Archive ID: " + result.getArchiveId());
            System.exit(0);
        } catch (Exception e)
        {
            System.err.println(e);
            System.exit(ERROR_DOWNLOAD);
        }
    }
    private static void usage(){
    	System.out.println("GlacierClient <file to upload>");
    }

}