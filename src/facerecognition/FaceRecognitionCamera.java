package facerecognition;

/**
 * *****************************************************************************
 *
 * INTEL CORPORATION PROPRIETARY INFORMATION This software is supplied under the
 * terms of a license agreement or nondisclosure agreement with Intel
 * Corporation and may not be copied or disclosed except in accordance with the
 * terms of that agreement Copyright(c) 2014 Intel Corporation. All Rights
 * Reserved.
 *
 ******************************************************************************
 */
import intel.rssdk.*;
import java.lang.System.*;
import java.util.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.*;
import java.io.File;

public class FaceRecognitionCamera {

    static {
        try {
            System.load("C:/Windows/System/libpxcclr.jni64.dll");

        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native code library failed to load.\n" + e);
            System.exit(1);
        }
    }
    static int cWidth = 640;
    static int cHeight = 480;
    static int dWidth, dHeight;
    static boolean exit = false;
    public String DatabaseFilename = "database.bin";
    static PXCMFaceData faceData;
    private static int faceRectangleHeight;
    private static int faceRectangleWidth;
    private static int faceRectangleX;
    private static int faceRectangleY;
    private static int iddata = 10;
    private static PXCMFaceConfiguration.RecognitionConfiguration.RecognitionStorageDesc desc;

    private static void PrintConnectedDevices() {
        PXCMSession session = PXCMSession.CreateInstance();
        PXCMSession.ImplDesc desc = new PXCMSession.ImplDesc();
        PXCMSession.ImplDesc outDesc = new PXCMSession.ImplDesc();
        desc.group = EnumSet.of(PXCMSession.ImplGroup.IMPL_GROUP_SENSOR);
        desc.subgroup = EnumSet.of(PXCMSession.ImplSubgroup.IMPL_SUBGROUP_VIDEO_CAPTURE);

        int numDevices = 0;
        for (int i = 0;; i++) {
            if (session.QueryImpl(desc, i, outDesc).isError()) {
                break;
            }

            PXCMCapture capture = new PXCMCapture();
            if (session.CreateImpl(outDesc, capture).isError()) {
                continue;
            }

            for (int j = 0;; j++) {
                PXCMCapture.DeviceInfo info = new PXCMCapture.DeviceInfo();
                if (capture.QueryDeviceInfo(j, info).isError()) {
                    break;
                }

                System.out.println(info.name);
                numDevices++;
            }
        }

        System.out.println("Found " + numDevices + " devices");
    }

    public static void main(String s[]) {
        PrintConnectedDevices();

        PXCMSenseManager senseMgr = PXCMSenseManager.CreateInstance();
        senseMgr.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_COLOR, 640, 480, 30);

        // Enable the face module
        senseMgr.EnableFace(null);
        pxcmStatus sts = senseMgr.EnableFace(null);
        sts = senseMgr.EnableStream(PXCMCapture.StreamType.STREAM_TYPE_DEPTH);
        PXCMFaceModule faceModule = senseMgr.QueryFace();

        PXCMFaceConfiguration faceConfig = faceModule.CreateActiveConfiguration();
        faceConfig.SetTrackingMode(PXCMFaceConfiguration.TrackingModeType.FACE_MODE_COLOR_PLUS_DEPTH);

        PXCMFaceConfiguration.RecognitionConfiguration rcfg = faceConfig.QueryRecognition();
        rcfg.Enable();
        // Create a recognition database
        desc = new PXCMFaceConfiguration.RecognitionConfiguration.RecognitionStorageDesc();
        desc.maxUsers = 10;
        //rcfg.CreateStorage("MyDB.bin", desc);
        //rcfg.UseStorage("MyDB.bin");

        // Set the registeration mode
        rcfg.SetRegistrationMode(PXCMFaceConfiguration.RecognitionConfiguration.RecognitionRegistrationMode.REGISTRATION_MODE_CONTINUOUS);

        faceConfig.ApplyChanges();

        sts = senseMgr.Init();

        if (sts.isError()) {
            System.out.println("Init failed: " + sts);
            return;
        }

        PXCMCapture.Device device = senseMgr.QueryCaptureManager().QueryDevice();
        PXCMCapture.Device.StreamProfileSet profiles = new PXCMCapture.Device.StreamProfileSet();
        device.QueryStreamProfileSet(profiles);

        dWidth = profiles.depth.imageInfo.width;
        dHeight = profiles.depth.imageInfo.height;

        Listener listener = new Listener();

        FaceRecognitionCamera c_raw = new FaceRecognitionCamera();
        DrawFrame c_df = new DrawFrame(cWidth, cHeight);
        JFrame cframe = new JFrame("Intel(R) RealSense(TM) SDK - Color Stream");
        cframe.addWindowListener(listener);
        cframe.setSize(cWidth, cHeight);
        cframe.add(c_df);
        cframe.setVisible(true);

//        CameraViewer d_raw = new CameraViewer(); 
//		DrawFrame d_df=new DrawFrame(dWidth, dHeight);      
//        JFrame dframe= new JFrame("Intel(R) RealSense(TM) SDK - Depth Stream"); 
//		dframe.addWindowListener(listener);
//		dframe.setSize(dWidth, dHeight); 
//        dframe.add(d_df);
//        dframe.setVisible(true); 
        if (sts == pxcmStatus.PXCM_STATUS_NO_ERROR) {
            faceData = faceModule.CreateOutput();
            for (int nframes = 0; nframes < 30000; nframes++) {
                senseMgr.AcquireFrame(true);
                PXCMCapture.Sample sample = senseMgr.QueryFaceSample();
                if (sample.color != null) {
                    PXCMImage.ImageData cData = new PXCMImage.ImageData();
                    sts = sample.color.AcquireAccess(PXCMImage.Access.ACCESS_READ, PXCMImage.PixelFormat.PIXEL_FORMAT_RGB32, cData);
                    if (sts.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                        System.out.println("Failed to AcquireAccess of color image data");
                        System.exit(3);
                    }

                    int cBuff[] = new int[cData.pitches[0] / 4 * cHeight];

                    cData.ToIntArray(0, cBuff);
                    c_df.image.setRGB(0, 0, cWidth, cHeight, cBuff, 0, cData.pitches[0] / 4);
                    c_df.repaint();
                    sts = sample.color.ReleaseAccess(cData);

                    if (sts.compareTo(pxcmStatus.PXCM_STATUS_NO_ERROR) < 0) {
                        System.out.println("Failed to ReleaseAccess of color image data");
                        System.exit(3);
                    }
                }

                //faceData = faceModule.CreateOutput();
                faceData.Update();

                // Read and print data 
                for (int fidx = 0;; fidx++) {
                    PXCMFaceData.Face face = faceData.QueryFaceByIndex(fidx);
                    if (face == null) {
                        break;
                    }

                    PXCMFaceData.RecognitionData rdata = face.QueryRecognition();
                    // recognize the current face?
                    if (iddata == rdata.QueryUserID()) {
                        int uid = rdata.QueryUserID();
                        rdata.UnregisterUser();
                        System.out.println("data if" + String.valueOf(uid));
                    } else {
                        rdata.RegisterUser();
                        iddata = rdata.QueryUserID();
                        System.out.println("data else " + rdata.QueryUserID());
                    }

                    PXCMFaceData.RecognitionModuleData rmd = faceData.QueryRecognitionModule();

                    int nbytes = rmd.QueryDatabaseSize();

                    byte[] buffer = new byte[nbytes];
                    System.out.println("data 2" + buffer);
                    // retrieve the database buffer

                    rmd.QueryDatabaseBuffer(buffer);

                }

                senseMgr.ReleaseFrame();
            }
            senseMgr.Close();
            System.out.println("Done streaming");

            faceData.close();
            senseMgr.Close();
            System.exit(0);
            cframe.dispose();

        } else {
            System.out.println("Failed to initialize");
        }
    }

}

class Listener extends WindowAdapter {

    public boolean exit = false;

    @Override
    public void windowClosing(WindowEvent e) {
        exit = true;
    }

}

class DrawFrame extends Component {

    public BufferedImage image;

    public DrawFrame(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    public void paint(Graphics g) {
        ((Graphics2D) g).drawImage(image, 0, 0, null);
    }
}
