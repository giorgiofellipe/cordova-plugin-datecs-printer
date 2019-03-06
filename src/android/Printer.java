package com.giorgiofellipe.datecsprinter;

import com.datecs.api.biometric.TouchChip;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Printer {
    private static final int MAX_PACKET_SIZE = 2044;
    private static boolean sDebug = false;
    private static final int DEFAULT_TIMEOUT = 1000;
    private static final int CARD_IO_TIME = 11000;
    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;
    public static final int BARCODE_UPCA = 65;
    public static final int BARCODE_UPCE = 66;
    public static final int BARCODE_EAN13 = 67;
    public static final int BARCODE_EAN8 = 68;
    public static final int BARCODE_CODE39 = 69;
    public static final int BARCODE_ITF = 70;
    public static final int BARCODE_CODABAR = 71;
    public static final int BARCODE_CODE93 = 72;
    public static final int BARCODE_CODE128 = 73;
    public static final int BARCODE_PDF417 = 74;
    public static final int BARCODE_CODE128AUTO = 75;
    public static final int BARCODE_EAN128 = 76;
    public static final int HRI_NONE = 0;
    public static final int HRI_ABOVE = 1;
    public static final int HRI_BELOW = 2;
    public static final int HRI_BOTH = 3;
    public static final int PAGE_LEFT = 0;
    public static final int PAGE_BOTTOM = 1;
    public static final int PAGE_RIGHT = 2;
    public static final int PAGE_TOP = 3;
    public static final int FILL_WHITE = 0;
    public static final int FILL_BLACK = 1;
    public static final int FILL_INVERTED = 2;
    public static final int SETTINGS_BLUETOOTH = 5;
    private InputStream mBaseInputStream;
    private OutputStream mBaseOutputStream;
    private IOException mLastError;
    private Printer.Settings mSettings;
    private Printer.ConnectionListener mConnListener;
    private byte[] mDataBuffer;
    private int mDataBufferLen;

    public Printer(OutputStream out) throws IOException {
        if(out == null) {
            throw new NullPointerException("The out is null");
        } else {
            this.mBaseInputStream = null;
            this.mBaseOutputStream = new BufferedOutputStream(out, 2044);
            this.initDefaults();
        }
    }

    public Printer(InputStream in, OutputStream out) throws IOException {
        if(in == null) {
            throw new NullPointerException("The in is null");
        } else if(out == null) {
            throw new NullPointerException("The out is null");
        } else {
            this.mBaseInputStream = in;
            this.mBaseOutputStream = new BufferedOutputStream(out, 2044);
            this.mDataBuffer = new byte[2044];
            this.initDefaults();
            Thread t = new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[2044];

                    try {
                        while(Printer.this.mLastError == null) {
                            int e = Printer.this.read(buffer, 0, buffer.length);
                            synchronized(Printer.this.mDataBuffer) {
                                while(e + Printer.this.mDataBufferLen > Printer.this.mDataBuffer.length) {
                                    byte[] tmp = new byte[Printer.this.mDataBuffer.length + 2044];
                                    System.arraycopy(Printer.this.mDataBuffer, 0, tmp, 0, Printer.this.mDataBufferLen);
                                    Printer.this.mDataBuffer = tmp;
                                }

                                System.arraycopy(buffer, 0, Printer.this.mDataBuffer, Printer.this.mDataBufferLen, e);
                                Printer.this.mDataBufferLen = Printer.this.mDataBufferLen + e;
                            }

                            try {
                                Thread.sleep(10L);
                            } catch (InterruptedException var10) {
                                ;
                            }
                        }
                    } catch (IOException var12) {
                        if(Printer.sDebug) {
                            var12.printStackTrace();
                        }

                        if(Printer.this.mLastError == null) {
                            Printer.this.mLastError = var12;
                        }
                    } finally {
                        Printer.this.raiseDisconnect();
                    }

                }
            });
            t.start();
        }
    }

    public synchronized void close() {
        this.mLastError = new IOException("The object is closed");

        try {
            if(this.mBaseInputStream != null) {
                this.mBaseInputStream.close();
            }
        } catch (IOException var3) {
            ;
        }

        try {
            if(this.mBaseOutputStream != null) {
                this.mBaseOutputStream.close();
            }
        } catch (IOException var2) {
            ;
        }

    }

    public static void setDebug(boolean on) {
        sDebug = on;
    }

    private static final String byteArrayToHexString(byte[] data, int offset, int length) {
        char[] hex = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] buf = new char[length * 3];
        int offs = 0;

        for(int i = 0; i < length; ++i) {
            buf[offs++] = hex[data[offset + i] >> 4 & 15];
            buf[offs++] = hex[data[offset + i] >> 0 & 15];
            buf[offs++] = 32;
        }

        return new String(buf, 0, offs);
    }

    private void debug(String text) {
        if(sDebug) {
            System.out.println(text);
        }

    }

    private void debug(String text, byte[] buffer, int offset, int count) {
        if(sDebug) {
            this.debug(text + byteArrayToHexString(buffer, offset, count) + "(" + count + ")");
        }

    }

    private void initDefaults() {
        this.mSettings = new Printer.Settings();
        this.mSettings.barcodeAlign = 0;
        this.mSettings.barcodeScale = 3;
        this.mSettings.barcodeHeight = 162;
        this.mSettings.barcodeHriFont = 0;
        this.mSettings.barcodeHriCode = 0;
    }

    public synchronized void write(int b) throws IOException {
        this.write(new byte[]{(byte)b});
    }

    public synchronized void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public synchronized void write(byte[] b, int offset, int length) throws IOException {
        this.mBaseOutputStream.write(b, offset, length);
        this.debug("<Printer> >> ", b, offset, length);
    }

    private int read(byte[] buffer, int offset, int length) throws IOException {
        int bytesRead = this.mBaseInputStream.read(buffer, offset, length);
        if(bytesRead < 0) {
            throw new IOException("The end of the stream is reached");
        } else if(bytesRead > length) {
            throw new IOException("Invalid stream result " + bytesRead);
        } else {
            if(bytesRead > 0) {
                this.debug("<Printer> << ", buffer, offset, bytesRead);
            }

            return bytesRead;
        }
    }

    private void output(byte[] buffer) throws IOException {
        this.write(buffer);
        this.mBaseOutputStream.flush();
    }

    private void request(int length, int timeout) throws IOException {
        long endTime = System.currentTimeMillis() + (long)timeout;
        if(this.mDataBuffer == null) {
            throw new IOException("Unable to read from input");
        } else {
            while(this.mDataBufferLen < length) {
                if(this.mLastError != null) {
                    throw this.mLastError;
                }

                if(endTime < System.currentTimeMillis()) {
                    throw new IOException("Timeout");
                }

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException var6) {
                    ;
                }
            }

        }
    }

    private void clear() {
        byte[] var1 = this.mDataBuffer;
        synchronized(this.mDataBuffer) {
            this.mDataBufferLen = 0;
        }
    }

    private void raiseDisconnect() {
        final Printer.ConnectionListener l = this.mConnListener;
        if(l != null) {
            (new Thread(new Runnable() {
                public void run() {
                    l.onDisconnect();
                }
            })).start();
        }

    }

    public void setConnectionListener(Printer.ConnectionListener listener) {
        this.mConnListener = listener;
    }

    public void flush() throws IOException {
        byte[] tmp = new byte[1024];
        this.mBaseOutputStream.write(tmp);
        this.mBaseOutputStream.flush();
    }

    public PrinterInformation getInformation() throws IOException {
        synchronized(this) {
            this.clear();
            this.output(new byte[]{(byte)27, (byte)90});
            byte[] buffer = new byte[32];
            this.request(buffer.length, 1000);
            System.arraycopy(this.mDataBuffer, 0, buffer, 0, buffer.length);
            PrinterInformation pi = new PrinterInformation(buffer);
            return pi;
        }
    }

    public int getVoltage() throws IOException {
        synchronized(this) {
            this.clear();
            this.output(new byte[]{(byte)27, (byte)96});
            byte[] buffer = new byte[2];
            this.request(buffer.length, 1000);
            System.arraycopy(this.mDataBuffer, 0, buffer, 0, buffer.length);
            return ((buffer[0] & 255) - 32) * 100;
        }
    }

    public int getVoltagePercentage() throws IOException {
        int voltage = this.getVoltage();
        boolean percent = false;
        byte percent1;
        if(voltage >= 82) {
            percent1 = 100;
        } else if(voltage == 81) {
            percent1 = 97;
        } else if(voltage == 80) {
            percent1 = 90;
        } else if(voltage == 79) {
            percent1 = 85;
        } else if(voltage == 78) {
            percent1 = 78;
        } else if(voltage == 77) {
            percent1 = 72;
        } else if(voltage == 76) {
            percent1 = 62;
        } else if(voltage == 75) {
            percent1 = 50;
        } else if(voltage == 74) {
            percent1 = 30;
        } else if(voltage == 73) {
            percent1 = 20;
        } else if(voltage == 72) {
            percent1 = 15;
        } else if(voltage == 71) {
            percent1 = 8;
        } else if(voltage == 70) {
            percent1 = 2;
        } else {
            percent1 = 0;
        }

        return percent1;
    }

    public int getTemperature() throws IOException {
        synchronized(this) {
            this.clear();
            this.output(new byte[]{(byte)27, (byte)96});
            byte[] buffer = new byte[2];
            this.request(buffer.length, 1000);
            System.arraycopy(this.mDataBuffer, 0, buffer, 0, buffer.length);
            return (buffer[1] & 255) - 32;
        }
    }
    
    public int getStatus() throws IOException {
        int status = 0;
        try {   
            synchronized(this) {
                this.clear();
                this.output(new byte[]{(byte)16, (byte)4, (byte)1});
                byte[] buffer = new byte[1];
                this.request(buffer.length, 1000);
                System.arraycopy(this.mDataBuffer, 0, buffer, 0, buffer.length);
                status = (buffer[0] & 255) - 18;
            }
        } catch (Exception e) {
            PrinterInformation pi = getInformation();
            //if is a DATECS printer
            if (pi.getModel() >= 0) {
                synchronized(this) {
                    this.clear();
                    this.output(new byte[]{(byte)27, (byte)118});
                    byte[] buffer = new byte[1];
                    this.request(buffer.length, 1000);
                    System.arraycopy(this.mDataBuffer, 0, buffer, 0, buffer.length);
                    status = buffer[0] & 255;
                }
            } else {
                throw new IOException();
            }
        }
        return status;
    }

    public String[] readCard(boolean first, boolean second, boolean third, int timeout) throws IOException {
        if(!first && !second && !third) {
            throw new IllegalArgumentException("No track selected");
        } else if(timeout < 0) {
            throw new IllegalArgumentException("The wait is negative");
        } else {
            synchronized(this) {
                for(int repeat = 0; repeat < (timeout + 11000) / 11000; ++repeat) {
                    int offset = 0;
                    this.clear();
                    this.output(new byte[]{(byte)27, (byte)63, (byte)((first?1:0) | (second?2:0) | (third?4:0))});

                    do {
                        ++offset;
                        this.request(offset, 11000);
                    } while(offset < this.mDataBuffer.length && this.mDataBuffer[offset - 1] != 0);

                    if(offset > 1) {
                        String[] track = new String[3];
                        byte trackIndex = -1;

                        for(int i = 0; i < offset - 1; ++i) {
                            switch(this.mDataBuffer[i] & 255) {
                                case 241:
                                    trackIndex = 0;
                                    track[0] = "";
                                    break;
                                case 242:
                                    trackIndex = 1;
                                    track[1] = "";
                                    break;
                                case 243:
                                    trackIndex = 2;
                                    track[2] = "";
                                    break;
                                default:
                                    if(trackIndex >= 0) {
                                        track[trackIndex] = track[trackIndex] + (char)(this.mDataBuffer[i] & 255);
                                    }
                            }
                        }

                        return track;
                    }
                }
            }

            return new String[3];
        }
    }

    public String readBarcode(int timeout) throws IOException {
        StringBuffer textBuffer = new StringBuffer();
        int offset = 0;
        if(timeout < 0) {
            throw new IllegalArgumentException("The wait is negative");
        } else {
            synchronized(this) {
                boolean consumeFF = false;
                this.clear();
                if(timeout == 0) {
                    this.output(new byte[]{(byte)27, (byte)66, (byte)(timeout + 32)});
                } else {
                    this.output(new byte[]{(byte)27, (byte)66, (byte)(timeout + 32), (byte)27, (byte)66, (byte)32});
                }

                this.request(1, (timeout + 1) * 1000);

                while(this.mDataBuffer[offset] != 0) {
                    if(this.mDataBuffer[offset] == -1) {
                        if(consumeFF) {
                            textBuffer.append('ÿ');
                            textBuffer.append('ÿ');
                            consumeFF = false;
                        } else {
                            consumeFF = true;
                        }
                    } else {
                        if(consumeFF) {
                            this.mDataBuffer[offset] = (byte)(this.mDataBuffer[offset] - 32);
                            consumeFF = false;
                        }

                        textBuffer.append((char)(this.mDataBuffer[offset] & 255));
                    }

                    ++offset;
                    this.request(offset, timeout);
                }
            }

            return textBuffer.length() == 0?null:textBuffer.toString();
        }
    }

    public void reset() throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)64, (byte)27, (byte)50, (byte)27, (byte)73, (byte)0, (byte)27, (byte)33, (byte)0, (byte)29, (byte)76, (byte)0, (byte)0};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void turnOff() throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)43};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void printSelfTest() throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)46};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void selectCodetable(int codetable) throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)117, (byte)codetable};
        if(codetable < 0) {
            throw new IllegalArgumentException("The codetable is negative");
        } else {
            synchronized(this) {
                this.write(buf);
            }
        }
    }

    public void setLineSpace(int lines) throws IOException {
        if(lines >= 0 && lines <= 255) {
            byte[] buf = new byte[]{(byte)27, (byte)51, (byte)(lines & 255)};
            synchronized(this) {
                this.write(buf);
            }
        } else {
            throw new IllegalArgumentException("The lines is out of range");
        }
    }

    public void setAlign(int align) throws IOException {
        switch(align) {
            case 0:
            case 1:
            case 2:
                byte[] buf = new byte[]{(byte)27, (byte)97, (byte)align};
                synchronized(this) {
                    this.write(buf);
                    return;
                }
            default:
                throw new IllegalArgumentException("The align is illegal");
        }
    }

    public void feedPaper(int lines) throws IOException {
        if(lines >= 0 && lines <= 255) {
            byte[] buf = new byte[]{(byte)27, (byte)74, (byte)lines};
            synchronized(this) {
                this.write(buf);
            }
        } else {
            throw new IllegalArgumentException("The lines is out of range");
        }
    }

    public void beep() throws IOException {
        byte[] tmp = new byte[]{(byte)27, (byte)30};
        synchronized(this) {
            this.write(tmp);
        }
    }

    public void melody(String data) throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(27);
        o.write(114);
        o.write(data.getBytes());
        synchronized(this) {
            this.write(o.toByteArray());
        }
    }

    public void printText(byte[] b) throws IOException {
        synchronized(this) {
            this.write(b);
        }
    }

    public void printText(String s) throws IOException {
        if(s == null) {
            throw new NullPointerException("The s is null");
        } else {
            synchronized(this) {
                this.printText(s.getBytes());
            }
        }
    }

    public void printText(String s, String encoding) throws IOException {
        if(s == null) {
            throw new NullPointerException("The s is null");
        } else {
            synchronized(this) {
                this.printText(s.getBytes(encoding));
            }
        }
    }

    private void printTaggedText(byte[] b) throws IOException {
        boolean LEN = true;
        boolean ELE = true;
        boolean END = true;
        boolean DET = true;
        int BREAK = "br".hashCode();
        int SMALL = "s".hashCode();
        int BOLD = "b".hashCode();
        int HIGH = "h".hashCode();
        int WIDE = "w".hashCode();
        int UNDERLINE = "u".hashCode();
        int ITALIC = "i".hashCode();
        int RESET = "reset".hashCode();
        int LEFT = "left".hashCode();
        int CENTER = "center".hashCode();
        int RIGHT = "right".hashCode();
        if(b == null) {
            throw new NullPointerException("The b is null");
        } else {
            int len = b.length;
            byte[] tbuf = new byte[6 + len];
            byte toffs = 0;
            byte mode = 0;
            int pos = 0;
            int var32 = toffs + 1;
            tbuf[toffs] = 27;
            tbuf[var32++] = 33;
            tbuf[var32++] = mode;
            tbuf[var32++] = 27;
            tbuf[var32++] = 73;
            tbuf[var32++] = 0;

            for(int i = 0; i < len; ++i) {
                byte value = b[i];
                tbuf[var32++] = value;
                if(value == 123) {
                    pos = var32;
                } else if(value == 125 && pos >= 1 && var32 - 1 - 6 <= pos) {
                    int index;
                    boolean set;
                    if(tbuf[pos] == 47) {
                        set = false;
                        index = pos + 1;
                    } else {
                        set = true;
                        index = pos;
                    }

                    int tmp = 0;
                    int hashlen = var32 - 1 - index;

                    for(int j = 0; j < hashlen; ++j) {
                        int c = tbuf[index + j] & 255;
                        if(c >= 65 && c <= 90) {
                            c += 32;
                        }

                        tmp = 31 * tmp + c;
                    }

                    if(tmp == BREAK) {
                        var32 = pos - 1;
                        tbuf[var32++] = 10;
                    } else if(tmp == SMALL) {
                        if(set) {
                            mode = (byte)(mode | 1);
                        } else {
                            mode &= -2;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == BOLD) {
                        if(set) {
                            mode = (byte)(mode | 8);
                        } else {
                            mode &= -9;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == HIGH) {
                        if(set) {
                            mode = (byte)(mode | 16);
                        } else {
                            mode &= -17;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == WIDE) {
                        if(set) {
                            mode = (byte)(mode | 32);
                        } else {
                            mode &= -33;
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == UNDERLINE) {
                        if(set) {
                            mode = (byte)(mode | 128);
                        } else {
                            mode = (byte)(mode & -129);
                        }

                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                    } else if(tmp == ITALIC) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 73;
                        tbuf[var32++] = (byte)(set?1:0);
                    } else if(tmp == RESET) {
                        mode = 0;
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 33;
                        tbuf[var32++] = mode;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 73;
                        tbuf[var32++] = 0;
                    } else if(tmp == LEFT) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 97;
                        tbuf[var32++] = 0;
                    } else if(tmp == CENTER) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 97;
                        tbuf[var32++] = 1;
                    } else if(tmp == RIGHT) {
                        var32 = pos - 1;
                        tbuf[var32++] = 27;
                        tbuf[var32++] = 97;
                        tbuf[var32++] = 2;
                    }
                }
            }

            synchronized(this) {
                byte[] var33 = new byte[var32];
                System.arraycopy(tbuf, 0, var33, 0, var33.length);
                this.printText(var33);
            }
        }
    }

    public void printTaggedText(String s) throws IOException {
        if(s == null) {
            throw new NullPointerException("The s is null");
        } else {
            this.printTaggedText(s.getBytes());
        }
    }

    public void printTaggedText(String s, String encoding) throws IOException {
        if(s == null) {
            throw new NullPointerException("The s is null");
        } else {
            this.printTaggedText(s.getBytes(encoding));
        }
    }

    public void printArabicText(int codetable, String text) throws IOException {
        boolean ARABIC_RAW = true;
        boolean ARABIC_1256M = true;
        boolean ARABIC_1256F = true;
        boolean FROM_RIGHT = true;
        boolean FROM_LEFT = true;
        byte[] ArabicAttr = new byte[]{(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)3, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)1, (byte)3, (byte)2, (byte)3, (byte)1, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)1, (byte)3, (byte)2, (byte)0, (byte)1, (byte)0, (byte)1, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0};
        byte[] Arabic1256F = new byte[]{(byte)0, (byte)1, (byte)2, (byte)3, (byte)4, (byte)5, (byte)6, (byte)7, (byte)8, (byte)9, (byte)10, (byte)11, (byte)12, (byte)13, (byte)14, (byte)15, (byte)16, (byte)17, (byte)18, (byte)19, (byte)-80, (byte)-79, (byte)-78, (byte)23, (byte)24, (byte)25, (byte)26, (byte)27, (byte)28, (byte)29, (byte)30, (byte)31};
        byte[] Arabic1256M = new byte[]{(byte)0, (byte)1, (byte)2, (byte)3, (byte)4, (byte)5, (byte)6, (byte)7, (byte)8, (byte)9, (byte)10, (byte)11, (byte)12, (byte)13, (byte)14, (byte)15, (byte)16, (byte)17, (byte)18, (byte)19, (byte)20, (byte)21, (byte)22, (byte)23, (byte)24, (byte)25, (byte)26, (byte)27, (byte)28, (byte)29, (byte)30, (byte)31};
        byte[][] Arabic1256 = new byte[][]{{(byte)-54, (byte)46, (byte)-76, (byte)-49, (byte)-73, (byte)-70, (byte)0, (byte)0, (byte)-67, (byte)-66, (byte)58, (byte)28, (byte)0, (byte)70, (byte)90, (byte)88, (byte)-120, (byte)-74, (byte)-75, (byte)-71, (byte)-72, (byte)-68, (byte)0, (byte)0, (byte)-124, (byte)-63, (byte)88, (byte)30, (byte)0, (byte)0, (byte)0, (byte)-106, (byte)0, (byte)12, (byte)-53, (byte)-52, (byte)-51, (byte)-50, (byte)-48, (byte)-47, (byte)0, (byte)-62, (byte)-96, (byte)-56, (byte)0, (byte)13, (byte)-61, (byte)0, (byte)-65, (byte)-59, (byte)-64, (byte)0, (byte)0, (byte)0, (byte)-46, (byte)-69, (byte)0, (byte)0, (byte)27, (byte)-55, (byte)0, (byte)0, (byte)0, (byte)31, (byte)-96, (byte)41, (byte)33, (byte)35, (byte)-98, (byte)37, (byte)-86, (byte)39, (byte)42, (byte)-43, (byte)50, (byte)54, (byte)62, (byte)66, (byte)74, (byte)78, (byte)80, (byte)84, (byte)86, (byte)92, (byte)96, (byte)100, (byte)104, (byte)-57, (byte)108, (byte)112, (byte)116, (byte)120, (byte)32, (byte)124, (byte)-128, (byte)-82, (byte)0, (byte)-116, (byte)0, (byte)-110, (byte)-104, (byte)-96, (byte)-100, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-92, (byte)-90, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-58, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-41}, {(byte)-54, (byte)47, (byte)-76, (byte)-49, (byte)-73, (byte)-70, (byte)0, (byte)0, (byte)-67, (byte)-66, (byte)59, (byte)28, (byte)0, (byte)71, (byte)91, (byte)89, (byte)-119, (byte)-74, (byte)-75, (byte)-71, (byte)-72, (byte)-68, (byte)0, (byte)0, (byte)-123, (byte)-63, (byte)89, (byte)30, (byte)0, (byte)0, (byte)0, (byte)-105, (byte)0, (byte)12, (byte)-53, (byte)-52, (byte)-51, (byte)-50, (byte)-48, (byte)-47, (byte)0, (byte)-62, (byte)-95, (byte)-56, (byte)0, (byte)13, (byte)-61, (byte)0, (byte)-65, (byte)-59, (byte)-64, (byte)0, (byte)0, (byte)0, (byte)-46, (byte)-69, (byte)0, (byte)0, (byte)27, (byte)-55, (byte)0, (byte)0, (byte)0, (byte)31, (byte)-46, (byte)41, (byte)34, (byte)36, (byte)-97, (byte)38, (byte)-85, (byte)40, (byte)43, (byte)-42, (byte)51, (byte)55, (byte)63, (byte)67, (byte)75, (byte)79, (byte)81, (byte)85, (byte)87, (byte)93, (byte)97, (byte)101, (byte)105, (byte)-57, (byte)109, (byte)113, (byte)117, (byte)121, (byte)32, (byte)125, (byte)-127, (byte)-81, (byte)0, (byte)-115, (byte)0, (byte)-109, (byte)-103, (byte)-95, (byte)-99, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-91, (byte)-89, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-58, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-40}, {(byte)-54, (byte)49, (byte)-76, (byte)-49, (byte)-73, (byte)-70, (byte)0, (byte)0, (byte)-67, (byte)-66, (byte)61, (byte)28, (byte)0, (byte)73, (byte)90, (byte)88, (byte)-117, (byte)-74, (byte)-75, (byte)-71, (byte)-72, (byte)-68, (byte)0, (byte)0, (byte)-121, (byte)-63, (byte)88, (byte)30, (byte)0, (byte)0, (byte)0, (byte)-106, (byte)0, (byte)12, (byte)-53, (byte)-52, (byte)-51, (byte)-50, (byte)-48, (byte)-47, (byte)0, (byte)-62, (byte)-93, (byte)-56, (byte)0, (byte)13, (byte)-61, (byte)0, (byte)-65, (byte)-59, (byte)-64, (byte)0, (byte)0, (byte)0, (byte)-46, (byte)-69, (byte)0, (byte)0, (byte)27, (byte)-55, (byte)0, (byte)0, (byte)0, (byte)31, (byte)-44, (byte)41, (byte)33, (byte)35, (byte)-98, (byte)37, (byte)-83, (byte)39, (byte)45, (byte)-43, (byte)53, (byte)57, (byte)65, (byte)69, (byte)77, (byte)78, (byte)80, (byte)84, (byte)86, (byte)95, (byte)99, (byte)103, (byte)107, (byte)-57, (byte)111, (byte)115, (byte)119, (byte)123, (byte)32, (byte)127, (byte)-125, (byte)-121, (byte)0, (byte)-111, (byte)0, (byte)-107, (byte)-101, (byte)-93, (byte)-100, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-92, (byte)-87, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-58, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-41}, {(byte)-54, (byte)48, (byte)-76, (byte)-49, (byte)-73, (byte)-70, (byte)0, (byte)0, (byte)-67, (byte)-66, (byte)60, (byte)28, (byte)0, (byte)72, (byte)91, (byte)89, (byte)-118, (byte)-74, (byte)-75, (byte)-71, (byte)-72, (byte)-68, (byte)0, (byte)0, (byte)-122, (byte)-63, (byte)89, (byte)30, (byte)0, (byte)0, (byte)0, (byte)-105, (byte)0, (byte)12, (byte)-53, (byte)-52, (byte)-51, (byte)-50, (byte)-48, (byte)-47, (byte)0, (byte)-62, (byte)-94, (byte)-56, (byte)0, (byte)13, (byte)-61, (byte)0, (byte)-65, (byte)-59, (byte)-64, (byte)0, (byte)0, (byte)0, (byte)-46, (byte)-69, (byte)0, (byte)0, (byte)27, (byte)-55, (byte)0, (byte)0, (byte)0, (byte)31, (byte)-45, (byte)41, (byte)34, (byte)36, (byte)-97, (byte)38, (byte)-84, (byte)40, (byte)44, (byte)-43, (byte)52, (byte)56, (byte)64, (byte)68, (byte)76, (byte)79, (byte)81, (byte)85, (byte)87, (byte)94, (byte)98, (byte)102, (byte)106, (byte)-57, (byte)110, (byte)114, (byte)118, (byte)122, (byte)32, (byte)126, (byte)-126, (byte)-122, (byte)0, (byte)-112, (byte)0, (byte)-108, (byte)-102, (byte)-94, (byte)-99, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-92, (byte)-88, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-58, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)-41}};
        byte[] input = text.getBytes("Cp1256");
        boolean found = false;

        for(int output = 0; output < input.length; ++output) {
            if(input[output] == -20) {
                found = true;
                break;
            }
        }

        if(found) {
            ByteArrayOutputStream var23 = new ByteArrayOutputStream();
            int LastAttr = 0;
            boolean NextAttr = false;
            boolean ArabAttr = false;

            for(int i = 0; i < input.length; ++i) {
                int ThisByte = input[i] & 255;
                if(ThisByte <= 32) {
                    LastAttr = 0;
                } else {
                    int var25 = 0;
                    int var24 = 0;
                    int NextByte = input.length - 1 == i?0:input[i] & 255;
                    if(NextByte >= 128) {
                        NextByte = Arabic1256[1][NextByte - 128] & 255;
                        var24 = ArabicAttr[NextByte] & 255;
                    }

                    if((var24 & 1) != 0) {
                        var25 |= 2;
                    }

                    if((LastAttr & 2) != 0) {
                        var25 |= 1;
                    }

                    if(ThisByte < 64) {
                        if(codetable == 23) {
                            ThisByte = (Arabic1256M[ThisByte - 32] & 255) + 32;
                        } else {
                            ThisByte = (Arabic1256F[ThisByte - 32] & 255) + 32;
                        }
                    } else {
                        ThisByte = (Arabic1256[var25][ThisByte - 128] & 255) + 32;
                        if(codetable == 24 && (ThisByte == 198 || ThisByte == 199)) {
                            ThisByte -= 2;
                        }

                        var25 = ArabicAttr[ThisByte - 32] & 255;
                    }

                    LastAttr = var25;
                }

                var23.write(ThisByte);
            }

            input = var23.toByteArray();
            synchronized(this) {
                this.selectCodetable(21);
                this.write(input);
                this.selectCodetable(codetable);
            }
        } else {
            synchronized(this) {
                this.selectCodetable(codetable);
                this.write(input);
            }
        }

    }

    private static void convertARGBToGrayscale(int[] argb, int width, int height) {
        int pixels = width * height;

        for(int i = 0; i < pixels; ++i) {
            int r = argb[i] >> 16 & 255;
            int g = argb[i] >> 8 & 255;
            int b = argb[i] & 255;
            int color = r * 19 + g * 38 + b * 7 >> 6 & 255;
            argb[i] = color;
        }

    }

    private static void ditherImageByFloydSteinberg(int[] grayscale, int width, int height) {
        int stopXM1 = width - 1;
        int stopYM1 = height - 1;
        int[] coef = new int[]{3, 5, 1};
        int y = 0;

        for(int offs = 0; y < height; ++y) {
            for(int x = 0; x < width; ++offs) {
                int v = grayscale[offs];
                int error;
                if(v < 128) {
                    grayscale[offs] = 0;
                    error = v;
                } else {
                    grayscale[offs] = 255;
                    error = v - 255;
                }

                int ed;
                if(x != stopXM1) {
                    ed = grayscale[offs + 1] + error * 7 / 16;
                    ed = ed < 0?0:(ed > 255?255:ed);
                    grayscale[offs + 1] = ed;
                }

                if(y != stopYM1) {
                    int i = -1;

                    for(int j = 0; i <= 1; ++j) {
                        if(x + i >= 0 && x + i < width) {
                            ed = grayscale[offs + width + i] + error * coef[j] / 16;
                            ed = ed < 0?0:(ed > 255?255:ed);
                            grayscale[offs + width + i] = ed;
                        }

                        ++i;
                    }
                }

                ++x;
            }
        }

    }

    private int cropImage(int[] grayscale, int width, int height) {
        int length = width * height;

        int offset;
        for(offset = 0; offset < length && grayscale[length - 1 - offset] >= 128; ++offset) {
            ;
        }

        int newHeight = height - offset / width;
        return newHeight;
    }

    public void printImage(int[] argb, int width, int height, int align, boolean dither, boolean crop) throws IOException {
        Object buf = null;
        boolean bufOffs = false;
        if(argb == null) {
            throw new NullPointerException("The argb is null");
        } else if(align >= 0 && align <= 2) {
            if(width >= 1 && height >= 1) {
                convertARGBToGrayscale(argb, width, height);
                if(dither) {
                    ditherImageByFloydSteinberg(argb, width, height);
                }

                if(crop) {
                    height = this.cropImage(argb, width, height);
                }

                byte[] var14 = new byte[width * 3 + 9];
                synchronized(this) {
                    byte var15 = 0;
                    int var16 = var15 + 1;
                    var14[var15] = 27;
                    var14[var16++] = 51;
                    var14[var16++] = 24;
                    this.write(var14, 0, var16);
                    var15 = 0;
                    var16 = var15 + 1;
                    var14[var15] = 27;
                    var14[var16++] = 97;
                    var14[var16++] = (byte)align;
                    var14[var16++] = 27;
                    var14[var16++] = 42;
                    var14[var16++] = 33;
                    var14[var16++] = (byte)(width % 256);
                    var14[var16++] = (byte)(width / 256);
                    var14[var14.length - 1] = 10;
                    int j = 0;

                    for(int offs = 0; j < height; ++j) {
                        int i;
                        if(j > 0 && j % 24 == 0) {
                            this.write(var14);

                            for(i = var16; i < var14.length - 1; ++i) {
                                var14[i] = 0;
                            }
                        }

                        for(i = 0; i < width; ++offs) {
                            var14[var16 + i * 3 + j % 24 / 8] |= (byte)((argb[offs] < 128?1:0) << 7 - j % 8);
                            ++i;
                        }
                    }

                    this.write(var14);
                }
            } else {
                throw new IllegalArgumentException("The size of image is illegal");
            }
        } else {
            throw new IllegalArgumentException("The align is illegal");
        }
    }

    public void printImage(int[] argb, int width, int height, int align, boolean dither) throws IOException {
        this.printImage(argb, width, height, align, dither, false);
    }

    private static int compressRLE(byte[] src, int srcOffs, byte[] dst, int dstOffs, int length) {
        int offset = 0;

        int dstLen;
        int count;
        for(dstLen = 0; offset < length; offset += count) {
            count = 1;
            int currValue = src[srcOffs + offset] & 255;

            for(int i = offset + 1; i < length && count < 64; ++i) {
                int nextValue = src[srcOffs + i] & 255;
                if(currValue != nextValue) {
                    break;
                }

                ++count;
            }

            if(count > 1) {
                dst[dstOffs + dstLen] = (byte)(192 | count);
                ++dstLen;
                dst[dstOffs + dstLen] = (byte)currValue;
                ++dstLen;
            } else if((currValue & 192) == 192) {
                dst[dstOffs + dstLen] = -63;
                ++dstLen;
                dst[dstOffs + dstLen] = (byte)currValue;
                ++dstLen;
            } else {
                dst[dstOffs + dstLen] = (byte)currValue;
                ++dstLen;
            }
        }

        return dstLen;
    }

    public void printCompressedImage(int[] argb, int width, int height, int align, boolean dither, boolean crop) throws IOException {
        if(argb == null) {
            throw new NullPointerException("The argb is null");
        } else if(align >= 0 && align <= 2) {
            if(width >= 1 && height >= 1) {
                convertARGBToGrayscale(argb, width, height);
                if(dither) {
                    ditherImageByFloydSteinberg(argb, width, height);
                }

                if(crop) {
                    height = this.cropImage(argb, width, height);
                }

                synchronized(this) {
                    int horzSizeInBytes = (width + 7) / 8;
                    byte[] buffer = new byte[horzSizeInBytes * 24 + 7];
                    byte[] packed = new byte[horzSizeInBytes * 24 * 2];
                    boolean cmdOffset = false;
                    byte var17 = 0;
                    int var18 = var17 + 1;
                    buffer[var17] = 27;
                    buffer[var18++] = 51;
                    buffer[var18++] = 24;
                    this.write(buffer, 0, var18);
                    var17 = 0;
                    var18 = var17 + 1;
                    buffer[var17] = 27;
                    buffer[var18++] = 97;
                    buffer[var18++] = (byte)align;
                    buffer[var18++] = 27;
                    buffer[var18++] = 42;
                    buffer[var18++] = 17;
                    buffer[var18++] = (byte)horzSizeInBytes;
                    int length = 0;

                    for(int offset = 0; length < height; ++length) {
                        int i;
                        if(length > 0 && length % 24 == 0) {
                            i = compressRLE(buffer, var18, packed, 0, buffer.length - var18);
                            this.write(buffer, 0, var18);
                            this.write(packed, 0, i);
                            this.write(10);

                            for(int i1 = var18; i1 < buffer.length; ++i1) {
                                buffer[i1] = 0;
                            }
                        }

                        for(i = 0; i < width; ++offset) {
                            buffer[var18 + i / 8 + length % 24 * horzSizeInBytes] |= (byte)((argb[offset] < 128?1:0) << i % 8);
                            ++i;
                        }
                    }

                    length = compressRLE(buffer, var18, packed, 0, buffer.length - var18);
                    this.write(buffer, 0, var18);
                    this.write(packed, 0, length);
                    this.write(10);
                }
            } else {
                throw new IllegalArgumentException("The size of image is illegal");
            }
        } else {
            throw new IllegalArgumentException("The align is illegal");
        }
    }

    public void printCompressedImage(int[] argb, int width, int height, int align, boolean dither) throws IOException {
        this.printCompressedImage(argb, width, height, align, dither, false);
    }

    public void printLogo(boolean wide, boolean high) throws IOException {
        byte mode = 0;
        if(wide) {
            ++mode;
        }

        if(high) {
            mode = (byte)(mode + 2);
        }

        byte[] buf = new byte[]{(byte)29, (byte)47, mode};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void printLogo() throws IOException {
        byte[] buf = new byte[]{(byte)29, (byte)47, (byte)0};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void setBarcode(int align, boolean small, int scale, int hri, int height) {
        if(align < 0) {
            throw new IllegalArgumentException("The align is illegal");
        } else if(scale >= 2 && scale <= 4) {
            if(hri < 0) {
                throw new IllegalArgumentException("The hri is negative");
            } else if(height >= 1 && height <= 255) {
                this.mSettings.barcodeAlign = align;
                this.mSettings.barcodeHriFont = small?1:0;
                this.mSettings.barcodeScale = scale;
                this.mSettings.barcodeHriCode = hri;
                this.mSettings.barcodeHeight = height;
            } else {
                throw new IllegalArgumentException("The height is illegal");
            }
        } else {
            throw new IllegalArgumentException("The scale is illegal");
        }
    }

    public void printBarcode(int type, byte[] data) throws IOException {
        if(data == null) {
            throw new NullPointerException("The data is null.");
        } else {
            byte[] buf = new byte[21 + data.length];
            byte offs = 0;
            int var7 = offs + 1;
            buf[offs] = 27;
            buf[var7++] = 97;
            buf[var7++] = (byte)this.mSettings.barcodeAlign;
            buf[var7++] = 29;
            buf[var7++] = 119;
            buf[var7++] = (byte)this.mSettings.barcodeScale;
            buf[var7++] = 29;
            buf[var7++] = 104;
            buf[var7++] = (byte)this.mSettings.barcodeHeight;
            buf[var7++] = 29;
            buf[var7++] = 72;
            buf[var7++] = (byte)this.mSettings.barcodeHriCode;
            buf[var7++] = 29;
            buf[var7++] = 102;
            buf[var7++] = (byte)this.mSettings.barcodeHriFont;
            switch(type) {
                case 65:
                    if(data.length != 11) {
                        throw new IllegalArgumentException("The length of UPCA barcode data must be 11 symbols");
                    }
                    break;
                case 66:
                    if(data.length != 11) {
                        throw new IllegalArgumentException("The length of UPCE barcode data must be 11 symbols");
                    }
                    break;
                case 67:
                    if(data.length != 12) {
                        throw new IllegalArgumentException("The length of EAN13 barcode data must be 12 symbols");
                    }
                    break;
                case 68:
                    if(data.length != 7) {
                        throw new IllegalArgumentException("The length of EAN8 barcode data must be 7 symbols");
                    }
                    break;
                case 69:
                case 70:
                case 71:
                case 72:
                case 73:
                case 75:
                case 76:
                    if(data.length < 1 || data.length > 255) {
                        throw new IllegalArgumentException("The length of barcode data must be between 1 and 255 symbols");
                    }
                    break;
                case 74:
                    if(data.length >= 1 && data.length <= 1000) {
                        break;
                    }

                    throw new IllegalArgumentException("The length of PDF417 barcode data must be between 1 and 1000 symbols");
                default:
                    throw new IllegalArgumentException("Invalid barcode type");
            }

            buf[var7++] = 29;
            buf[var7++] = 107;
            buf[var7++] = (byte)type;
            if(type == 73 && data[0] != 123) {
                buf[var7++] = (byte)(data.length + 2);
                buf[var7++] = 123;
                buf[var7++] = 66;
            } else if(type == 74) {
                buf[var7++] = 0;
                buf[var7++] = (byte)(data.length & 255);
                buf[var7++] = (byte)(data.length >> 8 & 255);
            } else {
                buf[var7++] = (byte)data.length;
            }

            for(int i = 0; i < data.length; ++i) {
                buf[var7++] = data[i];
            }

            synchronized(this) {
                this.write(buf, 0, var7);
            }
        }
    }

    public void printBarcode(int type, String data) throws IOException {
        if(data == null) {
            throw new NullPointerException("The data is null");
        } else {
            this.printBarcode(type, data.getBytes());
        }
    }

    public void printQRCode(int size, int eccLv, byte[] data) throws IOException {
        byte[] buf = new byte[10 + data.length];
        byte offs = 0;
        int var7 = offs + 1;
        buf[offs] = 27;
        buf[var7++] = 97;
        buf[var7++] = (byte)this.mSettings.barcodeAlign;
        buf[var7++] = 29;
        buf[var7++] = 81;
        buf[var7++] = 6;
        buf[var7++] = (byte)size;
        buf[var7++] = (byte)eccLv;
        buf[var7++] = (byte)data.length;
        buf[var7++] = (byte)(data.length >> 8);

        for(int i = 0; i < data.length; ++i) {
            buf[var7++] = data[i];
        }

        this.write(buf);
    }

    public void printQRCode(int size, int eccLv, String data) throws IOException {
        if(data == null) {
            throw new NullPointerException("The data is null");
        } else {
            this.printQRCode(size, eccLv, data.getBytes());
        }
    }

    public void calibrateBMMSensor() throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)67, (byte)65, (byte)76, (byte)3};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void feedLabel() throws IOException {
        byte[] buf = new byte[]{(byte)12};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void selectPageMode() throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)76, (byte)24};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void selectStandardMode() throws IOException {
        byte[] buf = new byte[]{(byte)29, (byte)85};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void printPage() throws IOException {
        byte[] buf = new byte[]{(byte)29, (byte)90};
        synchronized(this) {
            this.write(buf);
        }
    }

    public void setPageRegion(int x, int y, int width, int height, int direction) throws IOException {
        if(x >= 0 && y >= 0 && width >= 0 && height >= 0 && direction >= 0 && direction <= 3) {
            byte[] buf = new byte[]{(byte)27, (byte)87, (byte)(x & 255), (byte)(x >> 8 & 255), (byte)(y & 255), (byte)(y >> 8 & 255), (byte)(width & 255), (byte)(width >> 8 & 255), (byte)(height & 255), (byte)(height >> 8 & 255), (byte)29, (byte)84, (byte)(direction & 255)};
            synchronized(this) {
                this.write(buf);
            }
        } else {
            throw new IllegalArgumentException("The illegal parameter");
        }
    }

    public void setPageXY(int x, int y) throws IOException {
        if(x >= 0 && y >= 0) {
            byte[] buf = new byte[]{(byte)27, (byte)36, (byte)(x & 255), (byte)(x >> 8 & 255), (byte)29, (byte)36, (byte)(y & 255), (byte)(y >> 8 & 255)};
            synchronized(this) {
                this.write(buf);
            }
        } else {
            throw new IllegalArgumentException("The illegal parameter");
        }
    }

    public void drawPageRectangle(int x, int y, int width, int height, int fillMode) throws IOException {
        if(x >= 0 && y >= 0 && width >= 0 && height >= 0 && fillMode >= 0 && fillMode <= 2) {
            byte[] buf = new byte[]{(byte)29, (byte)82, (byte)(x & 255), (byte)(x >> 8 & 255), (byte)(y & 255), (byte)(y >> 8 & 255), (byte)(width & 255), (byte)(width >> 8 & 255), (byte)(height & 255), (byte)(height >> 8 & 255), (byte)(fillMode & 255)};
            synchronized(this) {
                this.write(buf);
            }
        } else {
            throw new IllegalArgumentException("The illegal parameter");
        }
    }

    public void drawPageFrame(int x, int y, int width, int height, int fillMode, int thickness) throws IOException {
        if(x >= 0 && y >= 0 && width >= 0 && height >= 0 && fillMode >= 0 && fillMode <= 2 && thickness >= 0) {
            byte[] buf = new byte[]{(byte)29, (byte)88, (byte)(x & 255), (byte)(x >> 8 & 255), (byte)(y & 255), (byte)(y >> 8 & 255), (byte)(width & 255), (byte)(width >> 8 & 255), (byte)(height & 255), (byte)(height >> 8 & 255), (byte)(fillMode & 255), (byte)(thickness & 255)};
            synchronized(this) {
                this.write(buf);
            }
        } else {
            throw new IllegalArgumentException("The illegal parameter");
        }
    }

    public String[] getSettings(int type) throws IOException {
        byte[] buffer = new byte[2044];
        int offset = 0;
        synchronized(this) {
            this.clear();
            this.output(new byte[]{(byte)27, (byte)115, (byte)type});

            while(true) {
                ++offset;
                this.request(offset, 1000);
                if(buffer[offset - 1] == 0) {
                    break;
                }
            }
        }

        StringBuffer sb = new StringBuffer();

        for(int i = 0; i < offset; ++i) {
            if((buffer[i] & 255) > 32) {
                sb.append((char)(buffer[i] & 255));
            } else if((buffer[i] & 255) > 0) {
                sb.append('\n');
            }
        }

        return sb.toString().substring(1).split("\n");
    }

    public void setIntensity(int level) throws IOException {
        byte[] buf = new byte[]{(byte)27, (byte)89, (byte)level};
        synchronized(this) {
            this.write(buf);
        }
    }

    public TouchChip getTouchChip() {
        TouchChip instance = new TouchChip() {
            protected byte[] transmit(byte[] input) throws IOException {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                buffer.write(27);
                buffer.write(39);

                int offset;
                for(int result = 0; result < input.length; ++result) {
                    offset = input[result] & 255;
                    if(offset < 32) {
                        buffer.write(16);
                        buffer.write(offset + 32);
                    } else {
                        buffer.write(offset);
                    }
                }

                Printer var8 = Printer.this;
                synchronized(Printer.this) {
                    Printer.this.clear();
                    Printer.this.output(buffer.toByteArray());
                    offset = 0;

                    do {
                        ++offset;
                        Printer.this.request(offset, 31000);
                    } while(offset < Printer.this.mDataBuffer.length - 1 && Printer.this.mDataBuffer[offset - 1] != 10);

                    buffer.reset();

                    for(int i = 0; i < offset - 2; ++i) {
                        int v = Printer.this.mDataBuffer[i] & 255;
                        if(v == 16) {
                            byte[] var10000 = Printer.this.mDataBuffer;
                            ++i;
                            v = (var10000[i] & 255) - 32;
                        }

                        buffer.write(v);
                    }
                }

                byte[] var9 = buffer.toByteArray();
                return var9;
            }
        };
        return instance;
    }

    public void journalFormat() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(29);
        o.write(35);
        o.write(37);
        o.write(38);
        o.write(42);
        o.write(69);
        synchronized(this) {
            this.clear();
            this.output(o.toByteArray());
            this.request(3, 3000);
        }
    }

    public void journalWrite(byte[] data) throws IOException {
        if(data.length > 400) {
            throw new IllegalArgumentException("The parameter \'text\' is out of limits");
        } else {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.write(29);
            o.write(35);
            o.write(37);
            o.write(38);
            o.write(42);
            o.write(119);
            o.write(data.length);
            o.write(data.length >> 8);
            o.write(data);
            synchronized(this) {
                this.clear();
                this.output(o.toByteArray());
                this.request(3, 1000);
            }
        }
    }

    public void journalWrite(String text, String charset) throws IOException {
        byte[] data = text.getBytes(charset);
        this.journalWrite(data);
    }

    public void journalPrint() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(29);
        o.write(35);
        o.write(37);
        o.write(38);
        o.write(42);
        o.write(80);
        synchronized(this) {
            this.output(o.toByteArray());
        }
    }

    private byte[] journalRead(boolean first) throws IOException {
        byte[] buffer = new byte[]{(byte)29, (byte)35, (byte)37, (byte)38, (byte)42, (byte)(first?70:78)};
        synchronized(this) {
            this.clear();
            this.output(buffer);
            int cnt = 1;
            boolean len = false;

            int var8;
            do {
                do {
                    ++cnt;
                    this.request(cnt, 1000);
                    var8 = (this.mDataBuffer[1] & 255) + ((this.mDataBuffer[2] & 255) << 8);
                } while(cnt < 3);
            } while(var8 != cnt - 3);

            if(this.mDataBuffer[0] == 70) {
                return null;
            } else {
                byte[] tmp = new byte[var8];
                System.arraycopy(this.mDataBuffer, 3, tmp, 0, tmp.length);
                return tmp;
            }
        }
    }

    public byte[] journalRead() throws IOException {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        synchronized(this) {
            byte[] res = this.journalRead(true);
            if(res != null) {
                o.write(res);

                while((res = this.journalRead(false)) != null) {
                    o.write(res);
                }
            }
        }

        return o.toByteArray();
    }

    public String journalRead(String charset) throws IOException {
        byte[] data = this.journalRead();
        return new String(data, 0, data.length, charset);
    }

    public interface ConnectionListener {
        void onDisconnect();
    }

    private class Settings {
        public int barcodeAlign;
        public int barcodeScale;
        public int barcodeHeight;
        public int barcodeHriFont;
        public int barcodeHriCode;

        private Settings() {
        }
    }
}
