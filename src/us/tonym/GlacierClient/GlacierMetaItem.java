package us.tonym.GlacierClient;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import java.util.Vector;
import java.util.List;
public class GlacierMetaItem {
	public String archiveID;
	public String md5;
	public String filename;
	public String mimeType;
	public Long length;
	
	public List<ReplaceableAttribute> toAttributes(){
		Vector<ReplaceableAttribute> attrs = new Vector<ReplaceableAttribute>();
		attrs.add(new ReplaceableAttribute("archiveID", archiveID, true));
		attrs.add(new ReplaceableAttribute("md5", md5, true));
		attrs.add(new ReplaceableAttribute("filename", filename, true));
		attrs.add(new ReplaceableAttribute("mimeType", mimeType, true));
		attrs.add(new ReplaceableAttribute("length", length.toString(), true));
		return attrs;
	}
	
	public String toString(){
		return " archiveID: archiveID" + 
		 "md5: " + md5 + 
		 " filename: " + filename +
		 " mimeType: " + mimeType +
		 " length: " + length.toString();
	}
}
