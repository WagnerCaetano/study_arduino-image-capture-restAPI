
import capture.ImageCapture;
import capture.ImageFrame;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by indrek on 1.05.2016.
 */
public class MainWindow {

  private static Integer MAX_IMAGE_W = 640;
  private static Integer MAX_IMAGE_H = 480;

  private static final String WINDOW_TITLE = "Arduino Image Capture";
  private static final String BUTTON_NAME_LISTEN = "Listen";
  private static final String BUTTON_NAME_STOP = "Stop";
  private static final String BUTTON_NAME_SELECT_SAVE_FOLDER = "Select save folder";
  private static final String SELECT_SAVE_FOLDER_TILE = "Save images to";

  private static final String DEFAULT_IMAGE_DIRECTORY = "/img";


  private JFrame windowFrame;
  private JPanel mainPanel;
  private File selectedFolder;
  private BufferedImage imageBuffer;
  private JLabel imageContainer;
  private TextArea debugWindow;
  private JComboBox<String> comPortSelection;
  private JComboBox<Integer> baudRateSelection;

  private SerialReader serialReader;
  private ImageCapture imageCapture;



  public MainWindow(JFrame frame) {
    windowFrame = frame;
    imageCapture = new ImageCapture(this::drawImage, this::debugTextReceived);
    serialReader = new SerialReader(imageCapture::addReceivedByte);

    mainPanel = new JPanel(new BorderLayout());
    mainPanel.add(createToolbar(), BorderLayout.PAGE_START);

    JPanel contentPanel = new JPanel(new BorderLayout());
    contentPanel.add(createImagePanel(), BorderLayout.PAGE_START);
    contentPanel.add(createDebugWindow());
    mainPanel.add(new JScrollPane(contentPanel));

    mainPanel.add(createSavePanel(), BorderLayout.PAGE_END);
  }


  private JComponent createSavePanel() {
    JPanel saveBar = new JPanel ();
    saveBar.setLayout(new BoxLayout(saveBar, BoxLayout.X_AXIS));
    JLabel filePathLabel = new JLabel();

    saveBar.add(createSelectFolderButton(filePathLabel));
    saveBar.add(Box.createHorizontalStrut(10));
    saveBar.add(filePathLabel);

    return saveBar;
  }

  private JButton createSelectFolderButton(JLabel filePathLabel) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.setDialogTitle(SELECT_SAVE_FOLDER_TILE);

    JButton listenButton = new JButton(BUTTON_NAME_SELECT_SAVE_FOLDER);
    listenButton.addActionListener((event)->{
      fileChooser.setCurrentDirectory(selectedFolder == null ? getDefaultSaveDirectory() : selectedFolder);

      if (fileChooser.showOpenDialog(listenButton) == JFileChooser.APPROVE_OPTION) {
        selectedFolder = fileChooser.getSelectedFile();
        filePathLabel.setText(selectedFolder.getAbsolutePath());
      }
    });
    return listenButton;
  }

  private File getDefaultSaveDirectory() {
      String currentDir = System.getProperty("user.dir");
      File dir = new File(currentDir + DEFAULT_IMAGE_DIRECTORY);
      return dir.exists() ? dir : new File(currentDir);
  }

  private JComponent createImagePanel() {
    imageBuffer = new BufferedImage(MAX_IMAGE_W,MAX_IMAGE_H, BufferedImage.TYPE_INT_ARGB);
    imageContainer = new JLabel(new ImageIcon(imageBuffer));
    return imageContainer;
  }


  private Component createDebugWindow() {
    debugWindow = new TextArea();
    return debugWindow;
  }


  private JToolBar createToolbar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    createComPortOption(toolBar);
    createBaudRateOption(toolBar);
    createListeningButtons(toolBar);
    return toolBar;
  }


  private void createComPortOption(JToolBar toolBar) {
    comPortSelection = new JComboBox<>();
    serialReader.getAvailablePorts().forEach(comPortSelection::addItem);
    toolBar.add(comPortSelection);
  }


  private void createBaudRateOption(JToolBar toolBar) {
    baudRateSelection = new JComboBox<>();
    serialReader.getAvailableBaudRates().forEach(baudRateSelection::addItem);
    baudRateSelection.setSelectedItem(serialReader.getDefaultBaudRate());
    toolBar.add(baudRateSelection);
  }


  private void createListeningButtons(JToolBar toolBar) {
    JButton startListenButton = new JButton(BUTTON_NAME_LISTEN);
    JButton stopListenButton = new JButton(BUTTON_NAME_STOP);
    stopListenButton.setEnabled(false);

    startListenButton.addActionListener((event)->{
      this.startListening(startListenButton, stopListenButton, event);
    });

    stopListenButton.addActionListener((event)->{
      this.stopListening(startListenButton, stopListenButton, event);
    });

    toolBar.add(startListenButton);
    toolBar.add(stopListenButton);
  }

  private void startListening(JButton startListenButton, JButton stopListenButton, ActionEvent event) {
    try {
      String selectedComPort = (String)comPortSelection.getSelectedItem();
      Integer baudRate = (Integer)baudRateSelection.getSelectedItem();
      serialReader.startListening(selectedComPort, baudRate);
      startListenButton.setEnabled(false);
      stopListenButton.setEnabled(true);
    } catch (SerialReaderException e) {
      JOptionPane.showMessageDialog(windowFrame, e.getMessage());
    }
  }

  private void stopListening(JButton startListenButton, JButton stopListenButton, ActionEvent event) {
    try {
      serialReader.stopListening();
      startListenButton.setEnabled(true);
      stopListenButton.setEnabled(false);
    } catch (SerialReaderException e) {
      JOptionPane.showMessageDialog(windowFrame, e.getMessage());
    }
  }


  private void drawImage(ImageFrame frame, Integer lineIndex) {
    JLabel imageContainer = this.imageContainer;

    // update image in a separate thread so it would not block reading data
    new Thread(() -> {
      synchronized (imageContainer) {
        Graphics2D g = imageBuffer.createGraphics();
        int fromLine = lineIndex != null ? lineIndex : 0;
        int toLine = lineIndex != null ? lineIndex : frame.getLineCount() - 1;

        for (int y = fromLine; y <= toLine; y++) {
          for (int x = 0; x < frame.getLineLength(); x++) {
            if (x < MAX_IMAGE_W && y < MAX_IMAGE_H) {
              g.setColor(frame.getPixelColor(x, y));
              g.drawLine(x, y, x, y);
            }
          }
        }
        imageContainer.repaint();
        // wait for last line to be drawn
        if (selectedFolder != null && lineIndex == frame.getLineCount() - 1) {
          saveImageToFile(imageBuffer.getSubimage(0, 0, frame.getLineLength(), frame.getLineCount()), selectedFolder);
        }
      }
    }).start();
  }


  private void debugTextReceived(String debugText) {
    System.out.println(debugText);
    if(debugText.contains("Button Pressed")) {
      new Thread(() -> {
        try {
          //send image to backend
          sendImageBackend();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }).start();
    }
    debugWindow.append(debugText + "\n");
  }


  private void saveImageToFile(BufferedImage image, File toFolder) {
    try {
      // save image to png file
      File newFile = new File(toFolder.getAbsolutePath(),  "output.png");
      FileOutputStream fos = new FileOutputStream(newFile);
      ImageIO.write(image, "png", fos);
      fos.close();
    } catch (Exception e) {
      System.out.println("Saving file failed: " + e.getMessage());
    }
  }

  private static void sendImageBackend() {
    File screenshotFile = new File("./img/output.png");

    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addPart("file", new FileBody(screenshotFile, ContentType.create("image/png"), "output.png"));

    HttpEntity multipartEntity = builder.build();
    HttpPost request = new HttpPost("http://ec2-18-228-199-45.sa-east-1.compute.amazonaws.com:5000/photos");
    request.setEntity(multipartEntity);

    HttpClient httpClient = HttpClients.createDefault();
    try {
      HttpResponse response = httpClient.execute(request);
      System.out.println(response);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  public static void main(String[] args) {
    JFrame frame = new JFrame(WINDOW_TITLE);
    MainWindow window = new MainWindow(frame);
    frame.setContentPane(window.mainPanel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.pack();
    frame.setVisible(true);
  }

}
