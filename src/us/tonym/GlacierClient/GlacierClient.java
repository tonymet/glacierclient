package us.tonym.GlacierClient;

import us.tonym.GlacierClient.GlacierMetaData;
import us.tonym.GlacierClient.GlacierMetaItem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import java.security.DigestInputStream;
import javax.activation.MimetypesFileTypeMap;


import java.io.FileInputStream;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.glacier.AmazonGlacierClient;
import com.amazonaws.services.glacier.internal.TreeHashInputStream;
import com.amazonaws.services.glacier.model.GetJobOutputRequest;
import com.amazonaws.services.glacier.model.GetJobOutputResult;
import com.amazonaws.services.glacier.transfer.ArchiveTransferManager;
import com.amazonaws.services.glacier.transfer.UploadResult;
import com.amazonaws.services.glacier.model.ListJobsRequest;
import com.amazonaws.services.glacier.model.ListJobsResult;
import com.amazonaws.services.glacier.model.GlacierJobDescription;


public class GlacierClient {
    public static String vaultName = "test1";
    public static String archiveToUpload = null;
    public static final int ERROR_ARGS = 2;
    public static final int ERROR_DOWNLOAD = 3;
    private  AmazonGlacierClient client;
    private static String cmd = null;
    private static String archiveId; 
    private static String jobId;
    private static String downloadFilePath;
    private static GetJobOutputResult jobOutputResult = null;
    private GlacierMetaItem gmi;
    private GlacierMetaData metaDataDB = null;
    private MimetypesFileTypeMap typeMap = null;
    private AWSCredentials credentials;
    private static GlacierClient instance;
    private ArchiveTransferManager atm  = null;
    
    // valid commands
    private enum commands{
    	get, put, getjob, listjobs, errcmd;
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
    
    public  GlacierClient() throws IOException{
    	this.credentials = new PropertiesCredentials(
    			GlacierClient.class.getResourceAsStream("AwsCredentials.properties"));
    	this.client = new AmazonGlacierClient(credentials);
    	this.client.setEndpoint("https://glacier.us-east-1.amazonaws.com/");
    	this.metaDataDB = new GlacierMetaData(credentials);
    	this.metaDataDB.init();
    	this.typeMap = new MimetypesFileTypeMap();
    	this.atm = new ArchiveTransferManager(client, credentials); 
    	
    }
    
    public Boolean putFile(File archiveFile) throws Exception{
    	// calculate the checksum
    	
    	UploadResult result = atm.upload(vaultName, "my archive " + (new Date()), archiveFile);
    	// store meta data in db.
    	this.gmi = new GlacierMetaItem();
    	this.gmi.archiveID = result.getArchiveId();
    	this.gmi.filename = archiveFile.getName();
    	this.gmi.length = new Long(archiveFile.length());
    	this.gmi.md5 = checksum(archiveFile);
    	this.gmi.mimeType = typeMap.getContentType(archiveToUpload);
    	System.out.println("Archive ID: " + result.getArchiveId());
    	System.err.println("metaData: " + this.gmi);
    	if(metaDataDB.putMetaItem(gmi)){
    		System.err.println("SimpleDB post success");
    	}
    	else{
    		System.err.println("ERROR: SimpleDB post failed");
    	}

    	return false;
    }
    
    public static void main(String[] args) throws IOException {
    	
    	try{
        	cmd = args[0];
        }
        catch (Exception e){
        	usage();
        	System.exit(ERROR_ARGS);
        }
    	instance = new GlacierClient();
    	
    	
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
        	try{
        		instance.putFile(new File(archiveToUpload));
	            System.exit(0);
	        } catch (Exception e)
	        {
	            System.err.println(e.getMessage());
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
        		instance.atm.download(vaultName, archiveId, new File(downloadFilePath));

        	} catch (Exception e)
        	{
        		System.err.println(e);
        	}
        	break;
        case getjob:
        	System.err.println("DO GETJOB");
        	try{
        		jobId = args[1];
        		downloadFilePath  = args[2];
        	}
        	catch (Exception e){
        		usage();
        		System.exit(ERROR_ARGS);
        	}
        	System.err.println("Save jobId " + jobId + " to file " + downloadFilePath);
        	try{
        		jobOutputResult = instance.client.getJobOutput(new GetJobOutputRequest()
        		.withAccountId("-")
        		.withVaultName(vaultName)
        		.withJobId(jobId));
        		downloadJobOutput(jobOutputResult, new File(downloadFilePath));
        	}
        	catch (Exception e)
        	{
        		System.err.println(e);
        	}
        	break;
        case listjobs:
        	ListJobsResult listJobsResult = instance.client.listJobs(new ListJobsRequest(vaultName));
        	for(GlacierJobDescription d : listJobsResult.getJobList()){
        		System.out.println("JobId: " + d.getJobId() + " -- description: " + d.getJobDescription());
        	}
        	break;
        default:
        	usage();
        	System.exit(ERROR_ARGS);
        	break;
        }
    }
    
    private static void downloadJobOutput(GetJobOutputResult jobOutputResult, File file) {
        TreeHashInputStream input;
        OutputStream output = null;
        byte[] buffer = new byte[1024 * 1024];
		try {
			input = new TreeHashInputStream(new BufferedInputStream(jobOutputResult.getBody()));
		} catch (NoSuchAlgorithmException e) {
			throw new AmazonClientException("Unable to compute hash for data integrity", e);
		}

        try {
            output = new BufferedOutputStream(new FileOutputStream(file));

            int bytesRead = 0;
            do {
            	bytesRead = input.read(buffer);
            	if (bytesRead <= 0) break;
            	output.write(buffer, 0, bytesRead);
            } while (bytesRead > 0);
        } catch (IOException e) {
            throw new AmazonClientException("Unable to save archive to disk", e);
        } finally {
            try {input.close();}  catch (Exception e) {}
            try {output.close();} catch (Exception e) {}

            try {
				String clientSideTreeHash = input.getTreeHash();
				String serverSideTreeHash = jobOutputResult.getChecksum();
				if (!clientSideTreeHash.equalsIgnoreCase(serverSideTreeHash)) {
					throw new AmazonClientException("Client side computed hash doesn't match server side hash; possible data corruption");
				}
			} catch (IOException e) {
				throw new AmazonClientException("Error while trying to confirm data integrity for archive download", e);
			}
        }
    }

    private static String checksum(File f) throws IOException{
    	StringBuffer hex =  new StringBuffer();
    	InputStream is = new FileInputStream(f);
    	DigestInputStream dis = null;
    	MessageDigest md = null;
    	try{
    		md = MessageDigest.getInstance("MD5");
    		dis = new DigestInputStream(is, md);
    		byte[] buf = new byte[256];
    		// read through file to get md5
    		while(dis.read(buf) != -1);
    	}catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	catch(IOException e){
    		e.printStackTrace();
    	}
    	finally{
    		dis.close();
    	}
    	byte[] hash = md.digest();

    	for (int i = 0; i < hash.length; i++) {
    		if ((0xff & hash[i]) < 0x10) {
    			hex.append("0"
    					+ Integer.toHexString((0xFF & hash[i])));
    		} else {
    			hex.append(Integer.toHexString(0xFF & hash[i]));
    		}
    	}
    	return hex.toString();
    }
    
    private static void usage(){
    	System.out.println("GlacierClient CMD args..");
    	System.out.println("              get <archive id> <path to local file> -- retrieve file to local file");
    	System.out.println("              put <path to local file>  -- upload locale file to glacier");
    	System.out.println("              getjob <job id> <path to local file> ");
    }

}