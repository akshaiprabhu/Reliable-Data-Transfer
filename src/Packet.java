import java.io.Serializable;

/**
 * Packet class
 * 
 * @author Akshai
 *
 */
public class Packet implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1402731143643428376L;
	public int seq; // seq no
	public int ackSet; // if sck or not
	public byte[] payload;
	public int offset; // offset
	public int eof; // if end of file or not
	public int checksum;

	public Packet() {
		seq = 0;
		ackSet = 0;
		payload = new byte[1004];
		offset = 0;
		eof = 0;
		checksum = 1;
	}
}
