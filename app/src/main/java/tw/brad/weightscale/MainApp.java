package tw.brad.weightscale;

import android.app.Application;

public class MainApp extends Application {
    public static final int BLEService_MODE_SCAN_START = 0;
    public static final int BLEService_MODE_SCAN_A_DEVICE = 1;
    public static final int BLEService_MODE_SCAN_STOP = 2;
    public static final int BLEService_MODE_SCAN_CALCEL = 3;

    public static final int BLEService_CMD_SCAN = 4;
    public static final int BLEService_CMD_CONNECT = 5;
    public static final int BLEService_CMD_DISCONNECT = 6;
}
