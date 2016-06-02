import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.TreeMap;

/**
 * Receiver class.
 * 
 * @author Akshai
 *
 */
public class Receiver {
	public static byte[] receiveData = new byte[2000];
	public static byte[] sendData = new byte[1024];
	public static InetAddress IPAddress;
	public static int port;
	public static int x;
	public static int seqNo = 0;
	static Packet packet = new Packet();
	public static TreeMap<Integer, String> segments = new TreeMap<Integer, String>();
	public static boolean flag = false;
	public ArrayList<Integer> ackList = new ArrayList<Integer>();
	public int r;

	/**
	 * Send class for receiver
	 * 
	 * @author Akshai
	 *
	 */
	public class Send extends Thread {
		DatagramSocket serverSocket;
		DatagramPacket sendPacket;

		public Send(DatagramSocket serverSocket) {
			this.serverSocket = serverSocket;
		}

		public void run() {
			handshake();

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		/**
		 * To send ack messages
		 */
		private synchronized void sendMessage() {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				ObjectOutputStream out = new ObjectOutputStream(bos);
				out.writeObject(packet);
				out.flush();
				byte[] bytes = bos.toByteArray();

				System.out.println("Sending ACK: " + packet.seq);
				sendPacket = new DatagramPacket(bytes, bytes.length, IPAddress,
						port);
				serverSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Handshake sender
		 */
		private synchronized void handshake() {
			sendData = new String("SYN.ACK: " + x).getBytes();
			sendPacket = new DatagramPacket(sendData, sendData.length,
					IPAddress, port);
			try {
				serverSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Receive class for receiver
	 * 
	 * @author Akshai
	 *
	 */
	public class Receive extends Thread {

		DatagramPacket receivePacket;
		DatagramSocket serverSocket;

		public void run() {
			Random rand = new Random();
			r = rand.nextInt(15);
			handshake();
			receiveMessage();
		}

		private synchronized void receiveMessage() {
			try {
				while (true) {
					receivePacket = new DatagramPacket(receiveData,
							receiveData.length);
					serverSocket.receive(receivePacket);
					ByteArrayInputStream bis = new ByteArrayInputStream(
							receivePacket.getData());
					ObjectInputStream in = new ObjectInputStream(bis);
					Packet p = (Packet) in.readObject();
					if ((p.checksum ^ 1) != 0) {
						System.out.println("Checksum failed");
						continue;
					}
					if (flag == false
							&& (p.seq == r || p.seq == (r + 1) || p.seq == (r + 2))) {
						packet.seq = seqNo;
						packet.ackSet = 1;
						System.out.println("Received: "
								+ new String("" + p.seq));
						segments.put(p.seq, new String(p.payload));
						if (p.seq == (r + 2)) {
							flag = true;
						}
					} else {
						if (flag == true) {
							flag = false;
						}
						seqNo = p.seq + 1;
						packet.seq = seqNo;
						packet.ackSet = 1;
						System.out.println("Received: "
								+ new String("" + p.seq));
						segments.put(p.seq, new String(p.payload));
					}

					Send send = new Send(serverSocket);
					send.sendMessage();
					if (p.eof == 1) {
						File file = new File("output.txt");
						FileWriter fw = new FileWriter(file.getAbsoluteFile());
						BufferedWriter bw = new BufferedWriter(fw);

						for (int i = 0; i < segments.size(); i++) {

							bw.write(segments.get(i));

						}
						bw.close();
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Handshake receiver
		 */
		private synchronized void handshake() {
			try {
				serverSocket = new DatagramSocket(9876);
			} catch (SocketException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			while (true) {
				receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				try {
					serverSocket.receive(receivePacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				String input = new String(receivePacket.getData()).trim();
				System.out.println(input);

				IPAddress = receivePacket.getAddress();
				port = receivePacket.getPort();
				x = Integer.parseInt(input.substring(input.indexOf(":") + 2));
				x++;

				Send send = new Send(serverSocket);
				send.start();
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				receivePacket = new DatagramPacket(receiveData,
						receiveData.length);
				try {
					serverSocket.receive(receivePacket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				input = new String(receivePacket.getData()).trim();
				System.out.println(input);
				System.out.println("Threeway handshake complete!!!");
				System.out.println("===================================="
						+ "===============================================");
				break;
			}

		}

	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		Receiver receiver = new Receiver();
		Receive ack = receiver.new Receive();
		ack.start();
	}
}