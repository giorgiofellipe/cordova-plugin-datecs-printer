package com.giorgiofellipe.datecsprinter;

public class PrinterInformation {
    public static final int PRINTER_UNKNOWN = -1;
    public static final int PRINTER_CMP10 = 0;
    public static final int PRINTER_DPP350 = 1;
    public static final int PRINTER_DPP250 = 2;
    public static final int PRINTER_PP60 = 3;
    public static final int PRINTER_EP55 = 4;
    public static final int PRINTER_DPP450 = 5;
    public static final int PRINTER_EP60 = 6;
    public static final int PRINTER_EP300 = 7;
    public static final int PRINTER_DPP500 = 8;
    private byte[] mData;
    private int mFwVersion;
    private boolean mIrdaAvailable;
    private boolean mMsrAvailable;
    private boolean mMsr3Track;
    private boolean mFahrenheit;
    private boolean mFirmwareUpdateSupported;
    private boolean mBlackmarkSupported;
    private boolean mBlackmarkEnabled;
    private boolean mBarcodeAvailable;
    private boolean mPageSupported;
    private boolean mUsbAEnabled;
    private int mPrinter;
    private String mPrinterName;
    private int mMaxPageWidth;
    private int mMaxPageHeight;
    private int mFeedLines;

    PrinterInformation(byte[] data) {
        if(data == null) {
            throw new NullPointerException("The data is null");
        } else if(data.length != 32) {
            throw new IllegalArgumentException("The data.length!=32");
        } else {
            this.mData = new byte[data.length];
            System.arraycopy(data, 0, this.mData, 0, this.mData.length);
            this.mFwVersion = ((data[22] & 255) - 48) * 100 + ((data[23] & 255) - 48) * 10 + ((data[24] & 255) - 48);
            this.mIrdaAvailable = (data[27] & 1) != 0;
            this.mMsrAvailable = (data[27] & 2) != 0;
            this.mMsr3Track = (data[27] & 4) != 0;
            this.mFahrenheit = (data[27] & 32) != 0;
            this.mFirmwareUpdateSupported = (data[28] & 1) != 0;
            this.mBlackmarkSupported = (data[28] & 4) != 0;
            this.mBlackmarkEnabled = false;
            this.mBarcodeAvailable = (data[28] & 8) != 0;
            this.mUsbAEnabled = (data[28] & 16) != 0;
            this.mPrinterName = (new String(data, 0, 22)).trim();
            this.mPrinter = -1;
            this.mPageSupported = false;
            this.mMaxPageWidth = 384;
            this.mMaxPageWidth = 0;
            this.mFeedLines = 90;
            if(this.mPrinterName.endsWith("IR Mobile Printer") || this.mPrinterName.endsWith("IR Mobile Printer") || this.mPrinterName.endsWith("DATECS Ltd.") || this.mPrinterName.endsWith("CMP-10BT") || this.mPrinterName.endsWith("CMP-10") || this.mPrinterName.endsWith("CMP-10 Bluetooth") || this.mPrinterName.endsWith("CITIZEN SYSTEMS")) {
                this.mPrinter = 0;
                this.mMaxPageWidth = 384;
                this.mFeedLines = 90;
            }

            if(this.mPrinterName.endsWith("DPP-350") || this.mPrinterName.endsWith("DPP350") || this.mPrinterName.endsWith("BLM-80") || this.mPrinterName.endsWith("BLM-80C") || this.mPrinterName.endsWith("DPP-350C")) {
                this.mPrinter = 1;
                if((data[30] & 64) != 0) {
                    this.mMaxPageWidth = 408;
                } else {
                    this.mMaxPageWidth = 576;
                }

                this.mFeedLines = 90;
                if(this.mBlackmarkSupported) {
                    this.mBlackmarkEnabled = (data[30] & 32) != 0;
                }

                if(this.mFwVersion >= 140) {
                    this.mPageSupported = (data[28] & 64) != 0;
                }

                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.endsWith("DPP-250") || this.mPrinterName.endsWith("SM1-21") || this.mPrinterName.endsWith("SM1-22") || this.mPrinterName.endsWith("DPP-255") || this.mPrinterName.endsWith("DPP-250C")) {
                this.mPrinter = 2;
                this.mMaxPageWidth = 384;
                this.mFeedLines = 110;
                if(this.mBlackmarkSupported) {
                    this.mBlackmarkSupported = (data[30] & 32) != 0;
                }

                this.mPageSupported = (data[28] & 64) != 0;
                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.endsWith("PP60") || this.mPrinterName.endsWith("PP-60")) {
                this.mPrinter = 3;
                this.mMaxPageWidth = 384;
                this.mFeedLines = 110;
                if(this.mBlackmarkSupported) {
                    this.mBlackmarkEnabled = (data[30] & 32) != 0;
                }

                this.mPageSupported = (data[28] & 64) != 0;
                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.endsWith("EP55") || this.mPrinterName.endsWith("EP-55")) {
                this.mPrinter = 4;
                this.mMaxPageWidth = 384;
                this.mFeedLines = 110;
                if(this.mBlackmarkSupported) {
                    this.mBlackmarkEnabled = (data[30] & 32) != 0;
                }

                this.mPageSupported = (data[28] & 64) != 0;
                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.endsWith("DPP450") || this.mPrinterName.endsWith("DPP-450") || this.mPrinterName.endsWith("SM2-41")) {
                this.mPrinter = 5;
                this.mMaxPageWidth = 832;
                this.mFeedLines = 110;
                this.mPageSupported = (data[28] & 64) != 0;
                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.endsWith("EP60H") || this.mPrinterName.endsWith("EP-60H")) {
                this.mPrinter = 6;
                this.mMaxPageWidth = 832;
                this.mFeedLines = 110;
                if(this.mBlackmarkSupported) {
                    this.mBlackmarkEnabled = (data[30] & 32) != 0;
                }

                this.mPageSupported = (data[28] & 64) != 0;
                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.contains("EP300")) {
                this.mPrinter = 7;
                this.mMaxPageWidth = 576;
                this.mFeedLines = 110;
                this.mPageSupported = false;
                this.mMaxPageHeight = 2432;
            }

            if(this.mPrinterName.contains("IDPA-500")) {
                this.mPrinter = 8;
                this.mMaxPageWidth = 1584;
                this.mFeedLines = 200;
                this.mPageSupported = true;
                this.mMaxPageHeight = 2432;
            }

        }
    }

    public byte[] getData() {
        byte[] b = new byte[this.mData.length];
        System.arraycopy(this.mData, 0, b, 0, b.length);
        return b;
    }

    public int getFirmwareVersion() {
        return this.mFwVersion;
    }

    public String getFirmwareVersionString() {
        return this.mFwVersion / 100 + "." + this.mFwVersion % 100;
    }

    public boolean isIrdaAvailable() {
        return this.mIrdaAvailable;
    }

    public boolean isMsrAvailable() {
        return this.mMsrAvailable;
    }

    public boolean isMsr3Tack() {
        return this.mMsr3Track;
    }

    public boolean isFahrenheit() {
        return this.mFahrenheit;
    }

    public boolean isFirmwareUpdateSupported() {
        return this.mFirmwareUpdateSupported;
    }

    public boolean isBlackmarkSupported() {
        return this.mBlackmarkSupported;
    }

    public boolean isBlackmarkEnabled() {
        return this.mBlackmarkEnabled;
    }

    public boolean isBarcodeAvailable() {
        return this.mBarcodeAvailable;
    }

    public boolean isPageSupported() {
        return this.mPageSupported;
    }

    public boolean isUsbAEnabled() {
        return this.mUsbAEnabled;
    }

    public int getModel() {
        return this.mPrinter;
    }

    public String getName() {
        return this.mPrinterName;
    }

    public int getMaxPageWidth() {
        return this.mMaxPageWidth;
    }

    public int getMaxPageHeight() {
        return this.mMaxPageHeight;
    }

    public int getFeedLines() {
        return this.mFeedLines;
    }
}
