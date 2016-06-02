import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * To convert file to byte array segments
 * 
 * @author Akshai
 *
 */
public class FileReader {

	public void ReadFile(ArrayList<Packet> segments, File file) {
		Packet pkt;
		Scanner sc = null;
		try {
			sc = new Scanner(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StringBuffer buf = new StringBuffer();
		while (sc.hasNext()) {
			buf.append(sc.next() + " ");
		}
		buf.trimToSize();
		System.out.println(buf.capacity());
		String buffer = buf.toString();
		// System.out.println(buffer);

		byte[] b = buffer.getBytes();
		int counter = 0;
		for (int i = 0; i < b.length - 1004 + 1; i += 1004) {
			pkt = new Packet();
			pkt.payload = Arrays.copyOfRange(b, i, i + 1004);
			pkt.offset = 1004 * (counter + 1);
			pkt.seq = counter;
			segments.add(pkt);
			// System.out.println("----" + counter + "===="
			//		+ new String(segments.get(counter).payload));
			counter++;
			// System.out.println(pkt.offset);
		}

		if (b.length % 1004 != 0) {
			pkt = new Packet();
			pkt.payload = Arrays.copyOfRange(b, b.length - b.length % 1004,
					b.length);
			pkt.offset = b.length;
			pkt.seq = counter;
			segments.add(pkt);
		}
		segments.get(segments.size()-1).eof = 1;
	}
}
