package java_video_stream;

import com.sun.jna.Native;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;

import javax.swing.*;

import com.sun.jna.NativeLibrary;
import com.sun.jna.platform.win32.WinUser.POINT;

import java.nio.file.Files;
import uk.co.caprica.vlcj.binding.LibVlc;

import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.runtime.x.LibXUtil;
//import uk.co.caprica.vlcj.runtime.windows.WindowsRuntimeUtil;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

/**
 *
 * @author Nghia
 */
public class JavaServer {

	/**
	 * @param args
	 *            the command line arguments
	 */
        //Khai báo 
	public static InetAddress[] inet;
	public static int[] port;
	public static int i;
	static int count = 0;
	public static BufferedReader[] inFromClient;
	public static DataOutputStream[] outToClient;
	
	public static void main(String[] args) throws Exception
	{
		JavaServer jv = new JavaServer();
	}

	
	// tạo một máy chủ trong Java, lắng nghe kết nối từ nhiều máy khách thông qua nhiều giao thức khác nhau (TCP và UDP)
	public JavaServer() throws Exception {
		
		// sử dụng thư viện JNA (Java Native Access) để thêm đường dẫn tìm kiếm cho thư viện native "libvlc." Thư viện này được sử dụng bởi VLCJ (Java bindings for VLC) để truy cập các chức năng của VLC (VideoLAN Client). Đường dẫn "c:\Program Files\VideoLAN\VLC" được cung cấp cho JNA để tìm thư viện cần thiết.
		NativeLibrary.addSearchPath("libvlc", "c:\\Program Files\\VideoLAN\\VLC");
                // inet để lưu trữ địa chỉ IP của các máy client và mảng port để lưu trữ các cổng mà máy client sử dụng.
		JavaServer.inet = new InetAddress[30];
		port = new int[30];

		// TODO code application logic here
                //tạo một ServerSocket được liên kết với cổng 6782.
		ServerSocket welcomeSocket = new ServerSocket(6782);
		System.out.println(welcomeSocket.isClosed());
		// tạo ra các mảng để quản lý thông tin kết nối từ và đến các máy khách. connectionSocket là mảng các Socket để đại diện cho các kết nối với máy khách.
                Socket connectionSocket[] = new Socket[30];
                // inFromClient và outToClient là mảng các BufferedReader và DataOutputStream để đọc và ghi dữ liệu đến các máy khách.
		inFromClient = new BufferedReader[30];
		outToClient = new DataOutputStream[30];
                //Tạo DatagramSocket serv và liên kết với cổng 4321. DatagramSocket này sẽ được sử dụng để gửi và nhận dữ liệu qua giao thức UDP, cụ thể là dữ liệu video.
		DatagramSocket serv = new DatagramSocket(4321);
                // tạo một mảng byte buf để lưu trữ dữ liệu và một DatagramPacket có tên dp để gửi và nhận các gói tin qua UDP.
		byte[] buf = new byte[62000];
		DatagramPacket dp = new DatagramPacket(buf, buf.length);
                
                // giao diện người dùng (GUI) để phát video.
		Canvas_Demo canv = new Canvas_Demo();
		System.out.println("Gotcha");

		i = 0;
		// mảng lớp Sthread xử lý kết nối với từng máy khách.
		SThread[] st = new SThread[30];
		

		while (true) {

			System.out.println(serv.getPort());
                        //Lắng nghe gói tin UDP trên serv và lưu trữ nó trong dp
			serv.receive(dp);
			System.out.println(new String(dp.getData()));
                        //Đặt dữ liệu của mảng buf thành dòng "starts" dưới dạng mảng byte
			buf = "starts".getBytes();
                        //Lấy địa chỉ IP của máy khách và cổng từ DatagramPacket và lưu chúng vào mảng inet và port tương ứng.
			inet[i] = dp.getAddress();
			port[i] = dp.getPort();
                        //gửi dữ liệu với nội dung "starts" đến máy khách đã kết nối.
			DatagramPacket dsend = new DatagramPacket(buf, buf.length, inet[i], port[i]);
			serv.send(dsend);
                        //Tạo một đối tượng Vidthread với tham số là serv và chạy nó để truyền video.
			Vidthread sendvid = new Vidthread(serv);
                        //Chờ và chấp nhận kết nối từ máy khách
			System.out.println("waiting\n ");
			connectionSocket[i] = welcomeSocket.accept();
			System.out.println("connected " + i);
                        //Tạo BufferedReader để đọc dữ liệu từ máy khách và DataOutputStream để ghi dữ liệu đến máy khách thông qua Socket.
			inFromClient[i] = new BufferedReader(new InputStreamReader(connectionSocket[i].getInputStream()));
			outToClient[i] = new DataOutputStream(connectionSocket[i].getOutputStream());
			outToClient[i].writeBytes("Connected: from Server\n");

			//Tạo một đối tượng SThread (một loại luồng) để xử lý kết nối với máy khách, cho phép máy chủ xử lý nhiều kết nối từ các máy khách đồng thời.
			st[i] = new SThread(i);
			st[i].start();
			//lần đầu tiên kết nối máy client
			if(count == 0)
			{
                                //chạy luồng để gửi dữ liệu tin nhắn từ máy chủ đến máy client.
				Sentencefromserver sen = new Sentencefromserver();
				sen.start();
				count++;
			}
                        
			System.out.println(inet[i]);
                        //Bắt đầu luồng sendvid để gửi video đến máy khách.
			sendvid.start();
                        //lặp lại việc kết nối với các máy client tiếp theo.
			i++;

			if (i == 30) {
				break;
			}
		}
	}
}

/*
Đây là một luồng (thread) riêng biệt được sử dụng để chụp khung hình màn hình và gửi chúng đến máy khách thông qua giao thức UDP.
- Hình ảnh màn hình được chụp bằng cách sử dụng lớp Robot để lấy mẫu màn hình.
- Khung hình được chuyển thành dữ liệu và gửi đi cho các máy khách thông qua UDP.
- Luồng này chạy vô hạn để liên tục gửi các khung hình mới đến máy khách.
*/
class Vidthread extends Thread {
        //khai báo
	int clientno;
	// InetAddress iadd = InetAddress.getLocalHost();
	JFrame jf = new JFrame("scrnshots before sending");
	JLabel jleb = new JLabel();

	DatagramSocket soc;

	Robot rb = new Robot();
	// Toolkit tk = Toolkit.getDefaultToolkit();

	// int x = tk.getScreenSize().height;
	// int y = tk.getScreenSize().width;

	byte[] outbuff = new byte[62000];

	BufferedImage mybuf;
	ImageIcon img;
	Rectangle rc;
	// độ lệch giữa panel và frame
	int bord = Canvas_Demo.panel.getY() - Canvas_Demo.frame.getY();
	// Rectangle rc = new Rectangle(new
	// Point(Canvas_Demo.frame.getX(),Canvas_Demo.frame.getY()),new
	// Dimension(Canvas_Demo.frame.getWidth(),Canvas_Demo.frame.getHeight()));

	// Rectangle rv = new Rectangle(d);
	public Vidthread(DatagramSocket ds) throws Exception {
		soc = ds;

		System.out.println(soc.getPort());
                //custom GUI
		jf.setSize(500, 400);
		jf.setLocation(500, 400);
		jf.setVisible(true);
	}

	public void run() {
		while (true) {
			try {
                                //Lấy số máy khách đã kết nối 
				int num = JavaServer.i;
                                //Tạo một đối tượng Rectangle để định nghĩa khu vực trên màn hình cần chụp: nửa phía trên của Canvas_Demo.panel.
				rc = new Rectangle(new Point(Canvas_Demo.frame.getX() + 8, Canvas_Demo.frame.getY() + 27),
						new Dimension(Canvas_Demo.panel.getWidth(), Canvas_Demo.frame.getHeight() / 2));

				// System.out.println("another frame sent ");
                                //Sử dụng đối tượng Robot để chụp hình ảnh màn hình trong khu vực đã xác định và lưu vào mybuf.
				mybuf = rb.createScreenCapture(rc);
                                //Tạo ảnh từ ảnh đã chụp
				img = new ImageIcon(mybuf);
                                //Đặt hình ảnh đã chụp vào JLabel.
				jleb.setIcon(img);
                                //Thêm JLabel vào cửa sổ JFrame.
				jf.add(jleb);
                                //Xóa và chuẩn bị để hiển thị ảnh mới
				jf.repaint();
				jf.revalidate();
				// jf.setVisible(true);
                                //luồng đầu ra để chuyển hình ảnh thành mảng byte.
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				//Ghi hình ảnh vào luồng đầu ra baos dưới dạng hình ảnh JPEG.
				ImageIO.write(mybuf, "jpg", baos);
				//Lấy mảng byte từ luồng đầu ra baos.
				outbuff = baos.toByteArray();
                                //gửi hình ảnh đã chụp đến tất cả các máy khách đã kết nối:
				for (int j = 0; j < num; j++) {
                                        //Tạo một gói dữ liệu UDP (DatagramPacket) chứa hình ảnh đã chụp và địa chỉ IP cùng cổng của máy khách thứ j.
					DatagramPacket dp = new DatagramPacket(outbuff, outbuff.length, JavaServer.inet[j],
							JavaServer.port[j]);
					//System.out.println("Frame Sent to: " + JavaServer.inet[j] + " port: " + JavaServer.port[j]
						//	+ " size: " + baos.toByteArray().length);
                                        //Gửi gói dữ liệu UDP đến máy khách.
					soc.send(dp);
					baos.flush();
				}
                                //delay 15ms
				Thread.sleep(15);

				// baos.flush();
				// byte[] buffer = baos.toByteArray();
			} catch (Exception e) {

			}
		}

	}

}
/*
- Sử dụng VLCJ để hiển thị video trong một cửa sổ GUI.
- Một cửa sổ JFrame được tạo để hiển thị video.
- Video được hiển thị trong một đối tượng Canvas.
*/
class Canvas_Demo {

	// Create a media player factory
	private MediaPlayerFactory mediaPlayerFactory;

	// Create a new media player instance for the run-time platform
	private EmbeddedMediaPlayer mediaPlayer;

	public static JPanel panel;
	public static JPanel myjp;
        public static JPanel jp2;
	private Canvas canvas;
	public static JFrame frame;
	public static JTextArea ta;
	public static JTextArea txinp;
	public static int xpos = 0, ypos = 0;
	String url = "D:\\DownLoads\\Video\\freerun.MP4";

	// Constructor
	public Canvas_Demo() {

		// Tạo một JPanel (panel) để chứa đối tượng canvas
		panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel mypanel = new JPanel();
		mypanel.setLayout(new GridLayout(2, 1));

		// Tạo đối tượng Canvas (canvas) và đặt màu nền thành màu đen. Đối tượng canvas sẽ hiển thị video.
		canvas = new Canvas();
		canvas.setBackground(Color.BLACK);
		// canvas.setSize(640, 480);
		panel.add(canvas, BorderLayout.CENTER);
		// panel.revalidate();
		// panel.repaint();

		// MediaPlayerFactory và EmbeddedMediaPlayer để phát video trên đối tượng canvas.
		mediaPlayerFactory = new MediaPlayerFactory();
		mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
                //video sẽ được phát trên canvas bằng cách thiết lập videoSurface của mediaPlayer thành đối tượng CanvasVideoSurface liên quan đến canvas.
		CanvasVideoSurface videoSurface = mediaPlayerFactory.newVideoSurface(canvas);
		mediaPlayer.setVideoSurface(videoSurface);

		// GUI
		frame = new JFrame("Video Streaming");
		// frame.setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocation(200, 0);
		frame.setSize(640, 960);
		frame.setAlwaysOnTop(true);

		// Adding the panel to the
		// panel.setSize(940, 480);
		// mypanel.setLayout(new BorderLayout());
		mypanel.add(panel);
		frame.add(mypanel);
		frame.setVisible(true);
		xpos = frame.getX();
		ypos = frame.getY();

		// Playing the video
                JLabel label= new JLabel();
                label.setText("Chat box:");
                JLabel label1= new JLabel();
                label1.setText("Comment:");
                ImageIcon icon= new ImageIcon("G:\\c# 10-11\\Java-UDP-Video-Stream-Server-master\\src\\java_video_stream\\375.png");
		myjp = new JPanel(new GridLayout(6, 1));
                //nút openfile
		JButton bn = new JButton("Open File");
                myjp.add(bn);
                myjp.add(label);
                //nút sendtext
		JButton sender = new JButton("Send Text");
                //thanh cuộn JScrollPane (jpane) được sử dụng để đặt các JTextArea cho tin nhắn và ô nhập tin nhắn.
		JScrollPane jpane = new JScrollPane();
		jpane.setSize(300, 200);
		ta = new JTextArea();
		txinp = new JTextArea();
		jpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		jpane.add(ta);
		jpane.setViewportView(ta);
		myjp.add(jpane);
                myjp.add(label1);
		myjp.add(txinp);
                jp2= new JPanel();
		jp2.add(sender);
		ta.setText("Waiting...");

		ta.setCaretPosition(ta.getDocument().getLength());
                myjp.add(jp2);
		mypanel.add(myjp);
                //mypanel.add(jp2);
		mypanel.revalidate();
		mypanel.repaint();

		// textArea.setPreferredSize(new Dimension(500, 100));
		// textArea.setEditable(false);
		// textArea.setLineWrap(true);
		// textArea.setWrapStyleWord(true);

		// JScrollPane scroller = new JScrollPane(textArea);
		// scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

		// textArea.append(" tuighadha\n");
		// textArea.setCaretPosition(textArea.getDocument().getLength());

		// myjp.add(textArea);
		// myjp.
                
                //ActionListener của nút open file
		bn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
                                //hiển thị hộp thoại mở tệp để người dùng có thể chọn một tệp từ hệ thống tệp của họ.
				JFileChooser jf = new JFileChooser();
				jf.showOpenDialog(frame);
                                //Lấy tệp được chọn bởi người dùng (nếu có) và lưu trữ nó trong đối tượng File (f).
				File f;
				f = jf.getSelectedFile();
				//ấy đường dẫn đầy đủ của tệp
                                url = f.getPath();
				System.out.println(url);
				ta.setText("check text\n");
				ta.append(url+"\n");
                                //Phát video
				mediaPlayer.playMedia(url);
			}
		});
                //ActionListener của gửi tin nhắn
		sender.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
                                //lấy value từ txinp và gửi qua máy khách
				Sentencefromserver.sendingSentence = txinp.getText();
                                //Xóa nội dung
				txinp.setText(null);
                                //hiển thị nội dung trên GUI
				Canvas_Demo.ta.append("From Myself: " + Sentencefromserver.sendingSentence + "\n");
                                //cập nhật lại giao diện người dùng. đảm bảo rằng nội dung mới trong JTextArea (ta) sẽ hiển thị trên giao diện người dùng.
				Canvas_Demo.myjp.revalidate();
				Canvas_Demo.myjp.repaint();
			}
		});
                

	}
}
/*
mở một luồng riêng biệt để xử lý việc giao tiếp văn bản giữa máy chủ và máy khách.
lắng nghe và xử lý tin nhắn văn bản từ máy khách và gửi chúng đến các máy khách khác.
Dữ liệu văn bản được truyền giữa máy chủ và máy khách qua các đối tượng BufferedReader và DataOutputStream.
*/
class SThread extends Thread {

	public static String clientSentence;
	int srcid;
        //lấy BufferedReader tương ứng với máy khách được xác định bởi srcid từ mảng JavaServer.inFromClient.
	BufferedReader inFromClient = JavaServer.inFromClient[srcid];
        // mảng chứa DataOutputStream cho tất cả các máy khách. Điều này cho phép lớp SThread gửi dữ liệu đến tất cả các máy khách.
	DataOutputStream outToClient[] = JavaServer.outToClient;

	public SThread(int a) {
		srcid = a;
		// start();
		// fowl fl = new fowl(inFromClient, srcid);
		// fl.start();
	}

	public void run() {
		while (true) {
			try {
                                //Đọc một dòng dữ liệu từ BufferedReader inFromClient. Nó chờ đến khi có dữ liệu mới để đọc. Dữ liệu này là dữ liệu được gửi từ máy khách được xác định bởi srcid.
				clientSentence = inFromClient.readLine();
                                
				System.out.println("From Client " + srcid + ": " + clientSentence);
                                //hiển thị thông điệp từ máy khách trên giao diện người dùng.
				Canvas_Demo.ta.append("From Client " + srcid + ": " + clientSentence + "\n");
				//duyệt qua mảng outToClient (chứa các DataOutputStream cho tất cả máy khách) và gửi thông điệp đến các máy khách khác (từ máy hiện tại => máy khách khác) bằng cách sử dụng outToClient[i].writeBytes(). 
				for(int i=0; i<JavaServer.i; i++){
                    
                    if(i!=srcid)
                        outToClient[i].writeBytes("Client "+srcid+": "+clientSentence + '\n');	//'\n' is necessary
                }
                                //reset
				Canvas_Demo.myjp.revalidate();
				Canvas_Demo.myjp.repaint();
					} catch (Exception e) {
			}

		}
	}
}
/*
Gửi tin nhắn từ server qua client. chạy liên tục và kiểm tra xem có tin nhắn cần gửi không.
*/
class Sentencefromserver extends Thread {
	
	public static String sendingSentence;
	
	public Sentencefromserver() {

	}

	public void run() {

		while (true) {

			try {
                                //Nếu có dữ liệu
				if(sendingSentence.length()>0)
				{
                                        //duyệt qua máy khách và gửi đến từng máy khách tương ứng
					for (int i = 0; i < JavaServer.i; i++) {
						JavaServer.outToClient[i].writeBytes("From Server: "+sendingSentence+'\n');
						
					}
                                        System.out.println(JavaServer.i);
                                        //xác định đã được gửi đi.
					sendingSentence = null;
				}

			} catch (Exception e) {

			}
		}
	}
}
