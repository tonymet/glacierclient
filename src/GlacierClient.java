import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

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
    private static String cmd = null;
    private static String archiveId; 
    private static String downloadFilePath;
    
    // valid commands
    private enum commands{
    	get, put, errcmd;
    	public static commands toCmd(String str)
        {
            try {
                return valueOf(str);
            } 
            catch (Exception ex) {
                return errcmd;
            }
        }   
    }
    
    public static void main(String[] args) throws IOException {
    	try{
        	cmd = args[0];
        }
        catch (Exception e){
        	usage();
        	System.exit(ERROR_ARGS);
        }
    	AWSCredentials credentials = new PropertiesCredentials(
    			GlacierClient.class.getResourceAsStream("AwsCredentials.properties"));
    	client = new AmazonGlacierClient(credentials);
    	client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
	
        switch(commands.toCmd(cmd)){
        case put:
        	System.err.println("DO PUT");
        	try{
            	archiveToUpload = args[1];
            }
            catch (Exception e){
            	usage();
            	System.exit(ERROR_ARGS);
            }
	      
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
	        break;
        case get:
        	System.err.println("DO GET");
        	try{
        		archiveId = args[1];
        		downloadFilePath  = args[2];
        	}
        	catch (Exception e){
        		usage();
        		System.exit(ERROR_ARGS);
        	}
        	System.err.println("Save archiveID " + archiveId + " to file " + downloadFilePath);
        	try {
        		ArchiveTransferManager atm = new ArchiveTransferManager(client, credentials);
        		atm.download(vaultName, archiveId, new File(downloadFilePath));

        	} catch (Exception e)
        	{
        		System.err.println(e);
        	}
        default:
        	usage();
        	System.exit(ERROR_ARGS);
        	break;
        }
    }
    private static void usage(){
    	System.out.println("GlacierClient CMD args..");
    	System.out.println("              get <archive id> <path to local file> -- retrieve file to local file");
    	System.out.println("              put <path to local file>  -- upload locale file to glacier");
    }

}