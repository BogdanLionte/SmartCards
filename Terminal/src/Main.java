import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;


import com.sun.javacard.apduio.*;


public class Main {

	private static byte CLA = (byte) 0x80;
	private static byte INS = (byte) 0xB8;
	private static byte P1 = (byte) 00;
	private static byte P2 = (byte) 00;


	static CadClientInterface cad;
	static Socket sock;

	static byte[] input = null;
	static byte[] output = null;
	static byte[] statusBytes = new byte[2];
	static String LcString;
	static int Lc = 0;;
	static byte[] dataIn = new byte[0];
	static int X = 0;
	static int Y = 0;
	static byte[] CVMs = new byte[12];

	static Apdu apdu = new Apdu();

	
	public static void main(String[] args) {
		
		apdu.command = new byte[]{(byte) 0x00, (byte) 0xa4, (byte) 0x04, (byte) 0x00};

		connectToCard();
		String capFileName = "C:\\Users\\Bogdan\\Desktop\\Wallet\\applet\\apdu_scripts\\cap-com.sun.jcclassic.samples.wallet.script";
		readCommandsFromFile(capFileName);
		
		String commandsFileName = "C:\\eclipse\\eclipse-workspace\\Terminal\\src\\commands.script";
		readCommandsFromFile(commandsFileName);	
		
		try {
			cad.powerDown();
		} catch (IOException | CadTransportException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		}
	}

	private static void connectToCard() {
		try {
			sock = new Socket("localhost", 9025);
			InputStream is = sock.getInputStream();
			OutputStream os = sock.getOutputStream();
			cad = CadDevice.getCadClientInstance(CadDevice.PROTOCOL_T1, is, os);
			byte[] ATR = cad.powerUp();
			System.out.println("Answer To Reset:\n");
			System.out.println(Arrays.toString(ATR));
		} catch (IOException e) {
		} catch (CadTransportException e) {
		}

		
	}

	private static void readCommandsFromFile(String capFileName) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new FileReader(capFileName));
		} catch (FileNotFoundException e1) {
			System.out.println(e1);
		}

		try {
			String line = reader.readLine();

			System.out.println("==========================");

			while(line != null) {

				if (line.equals("") || !(line.charAt(0) == '0')) {
					line = reader.readLine();
					continue;
				}

				setApduCommand(line, apdu);
				LcString = line.split(" ")[4].split("x")[1]; // 0x01
				Lc = Integer.parseInt(LcString, 16);
				dataIn = new byte[Lc + 1];
				fillDataIn(dataIn, line);
				dataIn[Lc] = (byte) 0x7F;
				apdu.setDataIn(dataIn, Lc);
				input = apdu.getDataIn();

				cad.exchangeApdu(apdu);

				output = apdu.getDataOut();
				statusBytes = apdu.sw1sw2;

				printAPDU(apdu, input, output, statusBytes, line, LcString);
				
				if (line.split(" ")[1].equals("0x60")) {
					//CVM List

					System.out.println("plm" + bytesToHex(output));
					String outputString = bytesToHex(output);
					X = Integer.parseInt(outputString.substring(0, 8), 16);
					Y = Integer.parseInt(outputString.substring(8, 16), 16);
					System.out.println("plm" + X + Y);
					System.out.println("len" + outputString.substring(16, 18));
					int length = Integer.parseInt(outputString.substring(16, 18), 16);
					for (int i = 0; i < length; i++) {
						System.out.println(outputString.charAt(18 + 2 * i));
						System.out.println(outputString.charAt(18 + 2 * i + 1));
						CVMs[2 * i] = (byte) Integer.parseInt(String.valueOf(outputString.charAt(18 + 2 * i)), 16);
						CVMs[2 * i + 1] = (byte) Integer.parseInt(String.valueOf(outputString.charAt(18 + 2 * i + 1)), 16);
					}
//					for (int i = 0; i < length; i++) {
//						System.out.println(CVMs[i]);
//					}
					
				}

				line = reader.readLine();

			}

		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static void setApduCommand(String line, Apdu apdu) {

		byte[] header = new byte[4];
		String[] parts = line.split(" ");
		for (int i = 0; i < 4; i++) {
			String stringByte = parts[i].split("x")[1];
			int intByte = Integer.parseUnsignedInt(stringByte, 16);
			header[i] = (byte) intByte;

		}

		apdu.command = header;

	}

	private static void printAPDU(Apdu apdu, byte[] input, byte[] output, byte[] statusBytes, String line,
			String LcString) {
		System.out.println("-----------------");
		System.out.println(line);
		if (input != null)
			System.out.println("input " + bytesToHex(apdu.command) + " " + LcString + bytesToHex(input));
		if (output!= null)
			System.out.println("output" + bytesToHex(output));
		System.out.println("SW1SW2 " + bytesToHex(statusBytes));
	}

	private static void fillDataIn(byte[] dataIn, String line) {
		String[] bytes = line.split(" ");
		int j = 0;
		for(int i = 5; i < bytes.length - 1; i++) {
			int intByte = 0;;

			intByte = Integer.parseInt(bytes[i].split("x")[1], 16);
			byte dataByte = (byte) intByte;
			dataIn[j++] = dataByte;
		}
	}

	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}
