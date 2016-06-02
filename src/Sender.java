import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Sender class to send messages and receive acks
 * 
 * @author Akshai
 *
 */
public class Sender {
	private static double cwnd = 1.0;
	private static int cur_ack = 0;
	public static int dup_count = 0;
	private static int packetNo = 0;
	public static byte[] sendData = new byte[1024];
	public static byte[] receiveData = new byte[2000];
	public static String input;
	public static ArrayList<Packet> segments;
	public static InetAddress IPAddress;
	public static int port = 9876;
	public static long RTT = 500;
	public ArrayList<Integer> ackList = new ArrayList<Integer>();
	public double ssthreash = 1;
	public static boolean flag = false;
	public static long TO;

	public class Send extends Thread {

		DatagramSocket clientSocket;
		DatagramPacket dataPacket;
		Random r = new Random();
		int x = r.nextInt(9);

		/**
		 * Run method
		 */
		public void run() {
			System.out.println("Starting Threeway Handshake!!!");
			handshake();
			System.out.println("===================================="
					+ "===============================================");
			sendMessage();
		}

		/**
		 * To send packets to receiver
		 */
		private void sendMessage() {
			while (packetNo != segments.size()) {
				try {
					clientSocket = new DatagramSocket();
				} catch (SocketException e) {
					e.printStackTrace();
				}
				int i = 0;
				TO = System.currentTimeMillis();
				while (i < cwnd && packetNo < segments.size()) {

					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					try {
						ObjectOutputStream out = new ObjectOutputStream(bos);
						out.writeObject(segments.get(packetNo));
						out.flush();
						byte[] bytes = bos.toByteArray();
						System.out.println("Sending: "
								+ new String("" + segments.get(packetNo).seq));
						packetNo++;
						dataPacket = new DatagramPacket(bytes, bytes.length,
								IPAddress, port);

						clientSocket.send(dataPacket);
						i++;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				try {
					Thread.sleep(RTT);
					Collections.sort(ackList);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		/**
		 * For handshake receiving.
		 */
		public synchronized void handshake() {
			try {
				clientSocket = new DatagramSocket();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				IPAddress = InetAddress.getByName("localhost");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			sendData = new String("SYN: " + x).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData,
					sendData.length, IPAddress, port);
			Receive receive = new Receive(clientSocket);
			receive.start();
			try {
				clientSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(RTT);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			int y = Integer.parseInt(input.substring(input.indexOf(":") + 2));
			y++;
			sendData = new String("ACK: " + y).getBytes();
			sendPacket = new DatagramPacket(sendData, sendData.length,
					IPAddress, port);
			try {
				clientSocket.send(sendPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * Retransmit when packet loss
		 * 
		 * @param packetNo
		 */
		public void retransmit(int packetNo) {
			try {
				clientSocket = new DatagramSocket();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				ObjectOutputStream out = new ObjectOutputStream(bos);
				out.writeObject(segments.get(packetNo));
				out.flush();
				byte[] bytes = bos.toByteArray();
				System.out.println("Sending: "
						+ new String("" + segments.get(packetNo).seq));
				packetNo++;
				dataPacket = new DatagramPacket(bytes, bytes.length, IPAddress,
						port);
				clientSocket.send(dataPacket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * To receive from receiver's send.
	 * 
	 * @author Akshai
	 *
	 */
	public class Receive extends Thread {
		DatagramSocket clientSocket;
		DatagramPacket receivePacket;

		public Receive(DatagramSocket clientSocket) {
			this.clientSocket = clientSocket;
		}

		public void run() {
			handshake();
			try {
				Thread.sleep(RTT);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			receiveMessage();
		}

		/**
		 * To receive acknowledgement from the receiver.
		 */
		private void receiveMessage() {
			try {
				while (true) {
					receivePacket = new DatagramPacket(receiveData,
							receiveData.length);

					clientSocket.receive(receivePacket);
					ByteArrayInputStream bis = new ByteArrayInputStream(
							receivePacket.getData());
					ObjectInputStream in = new ObjectInputStream(bis);
					Packet p = (Packet) in.readObject();
					System.out.println("Received ACK: " + p.seq);
					if (ackList.contains(p.seq)) {
						dup_count++;
						cur_ack = p.seq;
					} else {
						ackList.add(p.seq);
						if (flag == false) {
							cwnd++;
							System.out.println("----cwnd: " + cwnd);
						} else {
							if (cwnd <= ssthreash) {
								cwnd++;
								System.out.println("----cwnd: " + cwnd);
							}
							if (cwnd > ssthreash) {
								cwnd += 1 / cwnd;
								System.out.println("----cwnd: " + cwnd);
							}
						}
					}
					if (cwnd % ackList.size() == 0) {
						if (System.currentTimeMillis() - TO > 1) {
							for (int i = 0; i < ackList.size(); i++) {
								if (i != 0) {
									if (ackList.get(i) - ackList.get(i - 1) != 1) {
										Send s = new Send();
										s.retransmit(ackList.get(i));
										ssthreash = cwnd / 2;
										cwnd = 1.0;
										flag = true;
									}
								}
							}
						}
					}

					if (dup_count == 3) {
						Send s = new Send();
						s.retransmit(cur_ack);
						dup_count = 0;
						cur_ack = 0;
						ssthreash = cwnd / 2;
						cwnd = 1.0;
						flag = true;
					}
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Handshake receiver.
		 */
		private synchronized void handshake() {
			receivePacket = new DatagramPacket(receiveData, receiveData.length);
			try {
				clientSocket.receive(receivePacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			input = new String(receivePacket.getData()).trim();
			System.out.println(input);
		}
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		FileReader fr = new FileReader();
		File file = new File(args[0]);
		segments = new ArrayList<Packet>();
		fr.ReadFile(segments, file);
		System.out.println(segments.size());
		Sender sender = new Sender();
		Send syn = sender.new Send();
		syn.start();
		try {
			syn.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
