/**
 *  Store glacier metadata in SimpleDB
 * 
 * 
 * @author ametzidis
 *
 */
package us.tonym.GlacierClient;
import us.tonym.GlacierClient.GlacierMetaItem;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.auth.AWSCredentials;

public class GlacierMetaData {

	private String domain = "test1";
	private AmazonSimpleDB db;
	
	public GlacierMetaData(AWSCredentials awsCredentials){
		db = new AmazonSimpleDBClient(awsCredentials);
	}
	
	public Boolean domainExists(){
		ListDomainsResult res;
		res = db.listDomains();
		for(String d : res.getDomainNames()){
			if(d.equals(domain)){
				return true;
			}
		}
		return false;
	}
	
	public void init(){
		// create the domain
		if(!domainExists()){
			db.createDomain(new CreateDomainRequest(domain));
		}
	}
	
	public Boolean putMetaItem(GlacierMetaItem gmi){
		try{
			db.putAttributes(new PutAttributesRequest(domain, gmi.archiveID, gmi.toAttributes()));
			return true;
		}
		catch(Exception e){
			System.err.println(e.getMessage());
		}
		return false;
	}
}
