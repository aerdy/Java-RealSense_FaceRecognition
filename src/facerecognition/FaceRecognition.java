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

public class FaceRecognition {
    static int cWidth  = 640;
    static int cHeight = 480;
    static int dWidth, dHeight;
    static boolean exit = false;
    
    public static void main(String s[]) throws java.io.IOException {
        PXCMSenseManager senseMgr = PXCMSenseManager.CreateInstance();

        if (senseMgr == null) {
            System.out.println("Failed to create a sense manager instance.");
            return;
        }

        pxcmStatus sts = senseMgr.EnableFace(null);
        PXCMFaceModule faceModule = senseMgr.QueryFace();

        if (sts.isError() || faceModule == null) {
            System.out.println("Failed to initialize face module.");
            return;
        }

        // Retrieve the input requirements
        sts = pxcmStatus.PXCM_STATUS_DATA_UNAVAILABLE;
        PXCMFaceConfiguration faceConfig = faceModule.CreateActiveConfiguration();
        PXCMFaceConfiguration.RecognitionConfiguration rcfg = faceConfig.QueryRecognition();
        rcfg.Enable();
        // Create a recognition database
//        PXCMFaceConfiguration.RecognitionConfiguration.RecognitionStorageDesc desc=new PXCMFaceConfiguration.RecognitionConfiguration.RecognitionStorageDesc();
//        desc.maxUsers=10;
//        rcfg.CreateStorage("MyDB", desc);
//        rcfg.UseStorage("MyDB");
//
//        // Set the registeration mode
//        rcfg.SetRegistrationMode(PXCMFaceConfiguration.RecognitionConfiguration.RecognitionRegistrationMode.REGISTRATION_MODE_CONTINUOUS);


        faceConfig.ApplyChanges();

        sts = senseMgr.Init();

        if (sts.isError()) {
            System.out.println("Init failed: " + sts);
            return;
        }

        PXCMCapture.Device dev = senseMgr.QueryCaptureManager().QueryDevice();
        PXCMCapture.DeviceInfo info = new PXCMCapture.DeviceInfo();
        dev.QueryDeviceInfo(info);
        System.out.println("Using Camera: " + info.name);

        PXCMFaceData faceData = faceModule.CreateOutput();
        for (int nframes = 0; nframes < 30000; nframes++) {
            senseMgr.AcquireFrame(true);
            PXCMCapture.Sample sample = senseMgr.QueryFaceSample();

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
                int uid = rdata.QueryUserID();
                System.out.println("data" + uid);
                if (uid >= 0) {
                    System.out.println("data" + uid);
                }
            }

            //faceData.close();
            senseMgr.ReleaseFrame();
        }
        faceData.close();
        senseMgr.Close();
        System.exit(0);
    }
    
    
}
