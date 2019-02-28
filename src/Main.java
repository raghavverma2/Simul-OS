import java.util.Hashtable;
import java.io.*;
import java.lang.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.control.Label;
import javafx.stage.Stage;

class DirectoryManager {
	Hashtable<String, FileInfo> T = new Hashtable<>();

	void enter(StringBuffer key, FileInfo file)
	{
		T.put(key.toString(), file);
	}

	FileInfo lookup(StringBuffer key)
	{
		return T.get(key.toString());
	}
}


class Disk
{
    int nextFreeSector = 0;
    static final int NUM_SECTORS = 1024;
    StringBuffer sectors[] = new StringBuffer[NUM_SECTORS];
    void write(int sector, StringBuffer data)
    {
        sectors[sector] = new StringBuffer(data.toString());
        try {Thread.sleep(200);} catch (Exception e) {e.printStackTrace();}
        nextFreeSector += 1;
    }
    void read(int sector, StringBuffer data)
    {
        data.setLength(0);
        data.append(sectors[sector].toString());
        try {Thread.sleep(200);} catch (Exception e) {e.printStackTrace();}
    }
}

class FileInfo
{
	int diskNumber;
	int startingSector;
	int fileLength;
}

class Printer {
	StringBuffer filename = new StringBuffer("./outputs/PRINTER");
	String line;
	
	Printer(int num)
	{
		filename.append(Integer.toString(num));
	}

	void print(StringBuffer string)
	{
		try {
			FileWriter temp = new FileWriter(filename.toString(), true);
			BufferedWriter file = new BufferedWriter(temp);
			file.write(string.toString() + '\n');
			file.close();
			Thread.sleep(2750);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class ResourceManager
{
        boolean isFree[];
        ResourceManager(int numberOfItems)
        {
                isFree = new boolean[numberOfItems];
                for (int i=0; i<isFree.length; ++i)
                        isFree[i] = true;
        }
        synchronized int request()
        {
                while (true)
                {
                        for (int i = 0; i < isFree.length; ++i)
                                if ( isFree[i] )
                                {
                                        isFree[i] = false;
                                        return i;
                                }
                        try {this.wait();} catch (Exception e) {e.printStackTrace();} // block until someone releases a Resource
                }
        }
        synchronized void release( int index )
        {
                isFree[index] = true;
                this.notify(); // let a waiting thread run
        }
}

class UserThread extends Thread
{
    StringBuffer filename = new StringBuffer();
    BufferedReader file;
    int diskNo = -1;
    UserThread(int num) 
    {
        String path = String.format("./inputs/USER%s", num);
        try {
            file = new BufferedReader(new FileReader(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean canNextLineBeRead() {
        try {
            String line = file.readLine();
            if (line == null || line == "\n")
            {
                return false;
            }
            filename = new StringBuffer(line);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void run() {
        try {
            while (canNextLineBeRead())
            {
                if (filename.subSequence(0,5).equals(".save"))
                {
                    FileInfo file = new FileInfo();
                    StringBuffer fileline = new StringBuffer(filename.substring(6));
                    file.fileLength = 0;
                    file.diskNumber = Main.DISK_RESOURCE.request();
                    diskNo = file.diskNumber + 1;
                    file.startingSector = Main.DISKS[file.diskNumber].nextFreeSector;
                    canNextLineBeRead();
                    while (filename.subSequence(0,4).equals(".end") ==  false)
                    {
                        Main.DISKS[file.diskNumber].write(file.startingSector + file.fileLength, filename);
                        file.fileLength++;
                        canNextLineBeRead();
                    }
                    Main.DIRECTORY_MANAGER.enter(fileline, file);
                    Main.DISK_RESOURCE.release(file.diskNumber);
                    diskNo = -1;
                } else if(filename.subSequence(0,6).equals(".print")) {
                    PrintJobThread printJ = new PrintJobThread(filename.substring(7));
                    printJ.start();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

class PrintJobThread extends Thread
{
    StringBuffer filename;
    FileInfo file;
    StringBuffer fileline = new StringBuffer();

    PrintJobThread(String s) {
        filename = new StringBuffer(s);
        file = Main.DIRECTORY_MANAGER.lookup(filename);
    }

    public void run() {
        int printerNum = Main.PRINTER_RESOURCE.request();
        for (int i = 0; i < file.fileLength; i++)
        {
            Main.DISKS[file.diskNumber].read(file.startingSector + i, fileline);
            System.out.println(fileline.toString());
            try {
                Main.PRINTERS[printerNum].line = fileline.toString();
                Main.PRINTERS[printerNum].print(fileline);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Main.PRINTER_RESOURCE.release(printerNum);
    }
}

public class Main extends Application
{
	static final int NUMBER_OF_USERS = 4;
	static final int NUMBER_OF_DISKS = 2;
	static final int NUMBER_OF_PRINTERS = 3;
	static final Disk[] DISKS = new Disk[NUMBER_OF_DISKS];
	static final Printer[] PRINTERS = new Printer[NUMBER_OF_PRINTERS];
	static final UserThread[] USERS = new UserThread[NUMBER_OF_USERS];
	static final ResourceManager DISK_RESOURCE = new ResourceManager(NUMBER_OF_DISKS);
	static final ResourceManager PRINTER_RESOURCE = new ResourceManager(NUMBER_OF_PRINTERS);
	static final DirectoryManager DIRECTORY_MANAGER = new DirectoryManager();

    Label user1 = new Label("USER 1");
    Label user2 = new Label("USER 2");
    Label user3 = new Label("USER 3");
    Label user4 = new Label("USER 4");
    Label disk1 = new Label("DISK 1");
    Label disk2 = new Label("DISK 2");
    Label printer1 = new Label("PRINTER 1");
    Label printer2 = new Label("PRINTER 2");
    Label printer3 = new Label("PRINTER 3");

    private void update() {
        int user1disk = USERS[0].diskNo;
        int user2disk = USERS[1].diskNo;
        int user3disk = USERS[2].diskNo;
        int user4disk = USERS[3].diskNo;

        String printer1line = PRINTERS[0].line;
        String printer2line = PRINTERS[1].line;
        String printer3line = PRINTERS[2].line;

        if (user1disk != -1) {
            String text = String.format("USER 1 - Writing to Disk %s", user1disk);
            user1.setText(text);
        } else {
            user1.setText("USER 1 - IDLE");
        }
        if (user2disk != -1) {
            String text = String.format("USER 2 - Writing to Disk %s", user2disk);
            user2.setText(text);
        } else {
            user2.setText("USER 2 - IDLE");
        }
        if (user3disk != -1) {
            String text = String.format("USER 3 - Writing to Disk %s", user3disk);
            user3.setText(text);
        } else {
            user3.setText("USER 3 - IDLE");
        }
        if (user4disk != -1) {
            String text = String.format("USER 1 - Writing to Disk %s", user4disk);
            user4.setText(text);
        } else {
            user4.setText("USER 4 - IDLE");
        }
        if(DISK_RESOURCE.isFree[0]) {
            disk1.setText("DISK 1 - IDLE");
        } else {
            disk1.setText("DISK 1 - RUNNING");
        }
        if(DISK_RESOURCE.isFree[1]) {
            disk2.setText("DISK 2 - IDLE");
        } else {
            disk2.setText("DISK 2 - RUNNING");
        }
        if(PRINTER_RESOURCE.isFree[0]) {
            printer1.setText("PRINTER 1 - IDLE");
        } else {
            String text = String.format("PRINTER 1 - Printing  '%s'", printer1line);
            printer1.setText(text);
        }
        if(PRINTER_RESOURCE.isFree[1]) {
            printer2.setText("PRINTER 2 - IDLE");
        } else {
            String text = String.format("PRINTER 2 - Printing  '%s'", printer2line);
            printer2.setText(text);
        }
        if(PRINTER_RESOURCE.isFree[2]) {
            printer3.setText("PRINTER 3 - IDLE");
        } else {
            String text = String.format("PRINTER 3 - Printing  '%s'", printer3line);
            printer3.setText(text);
        }
    }

	@Override
    public void start(Stage primaryStage) {
    	primaryStage.setTitle("141 OS");

        GridPane layout = new GridPane();
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(33);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(33);
        ColumnConstraints col3 = new ColumnConstraints();
        col3.setPercentWidth(33);
        layout.getColumnConstraints().addAll(col1, col2, col3);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(25);
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(25);
        RowConstraints row3 = new RowConstraints();
        row3.setPercentHeight(25);
        RowConstraints row4 = new RowConstraints();
        row4.setPercentHeight(25);
        layout.getRowConstraints().addAll(row1, row2, row3, row4);

        layout.add(user1, 0, 0);
        layout.add(user2, 0, 1);
        layout.add(user3, 0, 2);
        layout.add(user4, 0, 3);
        layout.add(disk1, 1, 0);
        layout.add(disk2, 1, 1);
        layout.add(printer1, 2, 0);
        layout.add(printer2, 2, 1);
        layout.add(printer3, 2, 2);

        layout.setHgap(10);
        layout.setVgap(10);

    	for(int i = 1; i <= NUMBER_OF_USERS; i++) {
			USERS[i-1] = new UserThread(i);
		}
		for(int i = 1; i <= NUMBER_OF_DISKS; i++) {
			DISKS[i-1] = new Disk();
		}
		for(int i = 1; i <= NUMBER_OF_PRINTERS; i++) {
			PRINTERS[i-1] = new Printer(i);
		}

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Runnable updater = new Runnable() {

                    @Override
                    public void run() {
                        update();
                    }
                };

                while (true) {
                    try {
                        Thread.sleep(1000);
                        Platform.runLater(updater);
                        if(DISK_RESOURCE.isFree[0] && DISK_RESOURCE.isFree[1] && PRINTER_RESOURCE.isFree[0] && PRINTER_RESOURCE.isFree[1] && PRINTER_RESOURCE.isFree[2]) {
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        primaryStage.setScene(new Scene(layout, 800, 800));
        primaryStage.show();

        USERS[0].start();
        USERS[1].start();
        USERS[2].start();
        USERS[3].start();
    }

	public static void main(String[] args) {
		launch(args);
	}
}
