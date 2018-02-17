/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.circuit.zk2;

import com.zkteco.biometric.FingerprintSensorErrorCode;
import com.zkteco.biometric.FingerprintSensorEx;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;

/**
 *
 * @author Marvin
 */
public class ZKMain extends JFrame{
	JButton btnOpen = null;
	JButton btnEnroll = null;
	JButton btnVerify = null;
	JButton btnIdentify = null;
	JButton btnRegImg = null;
	JButton btnIdentImg = null;
	JButton btnClose = null;
	JButton btnImg = null;
	JRadioButton radioISO = null;
	JRadioButton radioANSI = null;
	
	
	private JTextArea textArea;
	
	//the width of fingerprint image
	int fpWidth = 0;
	//the height of fingerprint image
	int fpHeight = 0;
	//for verify test
	private byte[] lastRegTemp = new byte[2048];
	//the length of lastRegTemp
	private int cbRegTemp = 0;
	//pre-register template
	private byte[][] regtemparray = new byte[3][2048];
	//Register
	private boolean bRegister = false;
	//Identify
	private boolean bIdentify = true;
	//finger id
	private int iFid = 1;
	
	private int nFakeFunOn = 1;
	//must be 3
	static final int enroll_cnt = 3;
	//the index of pre-register function
	private int enroll_idx = 0;
	
	private byte[] imgbuf = null;
	private byte[] template = new byte[2048];
	private int[] templateLen = new int[1];
	
	
	private boolean mbStop = true;
	private long mhDevice = 0;
	private long mhDB = 0;
	private WorkThread workThread = null;
	
	public void launchFrame(){
		this.setLayout (null);
		btnOpen = new JButton("Open");  
		this.add(btnOpen);  
		int nRsize = 20;
		btnOpen.setBounds(30, 10 + nRsize, 100, 30);
		
		btnEnroll = new JButton("Enroll");  
		this.add(btnEnroll);  
		btnEnroll.setBounds(30, 60 + nRsize, 100, 30);
		
		btnVerify = new JButton("Verify");  
		this.add(btnVerify);  
		btnVerify.setBounds(30, 110 + nRsize, 100, 30);
		
		btnIdentify = new JButton("Identify");  
		this.add(btnIdentify);  
		btnIdentify.setBounds(30, 160 + nRsize, 100, 30);
		
		btnRegImg = new JButton("Register By Image");  
		this.add(btnRegImg);  
		btnRegImg.setBounds(15, 210 + nRsize, 120, 30);
		
		btnIdentImg = new JButton("Verify By Image");  
		this.add(btnIdentImg);  
		btnIdentImg.setBounds(15, 260 + nRsize, 120, 30);
		
		
		btnClose = new JButton("Close");  
		this.add(btnClose);  
		btnClose.setBounds(30, 310 + nRsize, 100, 30);
		
		
		//For ISO/Ansi
		radioANSI = new JRadioButton("ANSI", true);// åˆ›å»ºå�•é€‰æŒ‰é’®
		this.add(radioANSI);  
		radioANSI.setBounds(30, 360 + nRsize, 60, 30);
		
		radioISO = new JRadioButton("ISO");// åˆ›å»ºå�•é€‰æŒ‰é’®
		this.add(radioISO);  
		radioISO.setBounds(120, 360 + nRsize, 60, 30);
        
        ButtonGroup group = new ButtonGroup();
        group = new ButtonGroup();
        group.add(radioANSI);
        group.add(radioISO);
      //For End
        
		btnImg = new JButton();
		btnImg.setBounds(150, 5, 256, 300);
		btnImg.setDefaultCapable(false);
		this.add(btnImg); 
		
		textArea = new JTextArea();
		this.add(textArea);  
		textArea.setBounds(10, 440, 480, 100);
		
		
		this.setSize(520, 580);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setTitle("ZKFinger Demo");
		this.setResizable(false);
		
		btnOpen.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				if (0 != mhDevice)
				{
					//already inited
					textArea.setText("Please close device first!");
					return;
				}
				int ret = FingerprintSensorErrorCode.ZKFP_ERR_OK;
				//Initialize
				cbRegTemp = 0;
				bRegister = false;
				bIdentify = false;
				iFid = 1;
				enroll_idx = 0;
				if (FingerprintSensorErrorCode.ZKFP_ERR_OK != FingerprintSensorEx.Init())
				{
					textArea.setText("Init failed!");
					return;
				}
				ret = FingerprintSensorEx.GetDeviceCount();
				if (ret < 0)
				{
					textArea.setText("No devices connected!");
					FreeSensor();
					return;
				}
				if (0 == (mhDevice = FingerprintSensorEx.OpenDevice(0)))
				{
					textArea.setText("Open device fail, ret = " + ret + "!");
					FreeSensor();
					return;
				}
				if (0 == (mhDB = FingerprintSensorEx.DBInit()))
				{
					textArea.setText("Init DB fail, ret = " + ret + "!");
					FreeSensor();
					return;
				}
				
				//For ISO/Ansi
				int nFmt = 0;	//Ansi
				if (radioISO.isSelected())
				{
					nFmt = 1;	//ISO
				}
				FingerprintSensorEx.DBSetParameter(mhDB,  5010, nFmt);				
				//For ISO/Ansi End
				
				//set fakefun off
				//FingerprintSensorEx.SetParameter(mhDevice, 2002, changeByte(nFakeFunOn), 4);
				
				byte[] paramValue = new byte[4];
				int[] size = new int[1];
				//GetFakeOn
				//size[0] = 4;
				//FingerprintSensorEx.GetParameters(mhDevice, 2002, paramValue, size);
				//nFakeFunOn = byteArrayToInt(paramValue);
				
				size[0] = 4;
				FingerprintSensorEx.GetParameters(mhDevice, 1, paramValue, size);
				fpWidth = byteArrayToInt(paramValue);
				size[0] = 4;
				FingerprintSensorEx.GetParameters(mhDevice, 2, paramValue, size);
				fpHeight = byteArrayToInt(paramValue);
				//width = fingerprintSensor.getImageWidth();
				//height = fingerprintSensor.getImageHeight();
				imgbuf = new byte[fpWidth*fpHeight];
				btnImg.resize(fpWidth, fpHeight);
				mbStop = false;
				workThread = new WorkThread();
			    workThread.start();// çº¿ç¨‹å�¯åŠ¨
	            textArea.setText("Open succ!");
			}
		});
		
		
		
		btnClose.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				FreeSensor();
				
				textArea.setText("Close succ!");
			}
		});
		
		btnEnroll.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == mhDevice)
				{
					textArea.setText("Please Open device first!");
					return;
				}
				if(!bRegister)
				{
					enroll_idx = 0;
					bRegister = true;
					textArea.setText("Please your finger 3 times!");
				}
			}
			});
		
		btnVerify.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == mhDevice)
				{
					textArea.setText("Please Open device first!");
					return;
				}
				if(bRegister)
				{
					enroll_idx = 0;
					bRegister = false;
				}
				if(bIdentify)
				{
					bIdentify = false;
				}
			}
			});
		
		btnIdentify.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == mhDevice)
				{
					textArea.setText("Please Open device first!");
					return;
				}
				if(bRegister)
				{
					enroll_idx = 0;
					bRegister = false;
				}
				if(!bIdentify)
				{
					bIdentify = true;
				}
			}
			});
		
		
		btnRegImg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 == mhDB)
				{
					textArea.setText("Please open device first!");
				}
				String path = "d:\\test\\fingerprint.bmp";
				byte[] fpTemplate = new byte[2048];
				int[] sizeFPTemp = new int[1];
				sizeFPTemp[0] = 2048;
				int ret = FingerprintSensorEx.ExtractFromImage( mhDB, path, 500, fpTemplate, sizeFPTemp);
				if (0 == ret)
				{
					ret = FingerprintSensorEx.DBAdd( mhDB, iFid, fpTemplate);
					if (0 == ret)
					{
						//String base64 = fingerprintSensor.BlobToBase64(fpTemplate, sizeFPTemp[0]);		
						iFid++;
                    	cbRegTemp = sizeFPTemp[0];
                        System.arraycopy(fpTemplate, 0, lastRegTemp, 0, cbRegTemp);
                        //Base64 Template
                        //String strBase64 = Base64.encodeToString(regTemp, 0, ret, Base64.NO_WRAP);
                        textArea.setText("enroll succ");
					}
					else
					{
						textArea.setText("DBAdd fail, ret=" + ret);
					}
				}
				else
				{
					textArea.setText("ExtractFromImage fail, ret=" + ret);
				}
			}
			});
		
		
		btnIdentImg.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if(0 ==  mhDB)
				{
					textArea.setText("Please open device first!");
				}
				String path = "d:\\test\\fingerprint.bmp";
				byte[] fpTemplate = new byte[2048];
				int[] sizeFPTemp = new int[1];
				sizeFPTemp[0] = 2048;
				int ret = FingerprintSensorEx.ExtractFromImage(mhDB, path, 500, fpTemplate, sizeFPTemp);
				if (0 == ret)
				{
					if (bIdentify)
					{
						int[] fid = new int[1];
						int[] score = new int [1];
						ret = FingerprintSensorEx.DBIdentify(mhDB, fpTemplate, fid, score);
                        if (ret == 0)
                        {
                        	textArea.setText("Identify succ, fid=" + fid[0] + ",score=" + score[0]);
                        }
                        else
                        {
                        	textArea.setText("Identify fail, errcode=" + ret);
                        }
                            
					}
					else
					{
						if(cbRegTemp <= 0)
						{
							textArea.setText("Please register first!");
						}
						else
						{
							ret = FingerprintSensorEx.DBMatch(mhDB, lastRegTemp, fpTemplate);
							if(ret > 0)
							{
								textArea.setText("Verify succ, score=" + ret);
							}
							else
							{
								textArea.setText("Verify fail, ret=" + ret);
							}
						}
					}
				}
				else
				{
					textArea.setText("ExtractFromImage fail, ret=" + ret);
				}
			}
		});
	
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent e) {
                // TODO Auto-generated method stub
            	FreeSensor();
            }
		});
	}
	
	private void FreeSensor()
	{
		mbStop = true;
		try {		//wait for thread stopping
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (0 != mhDB)
		{
			FingerprintSensorEx.DBFree(mhDB);
			mhDB = 0;
		}
		if (0 != mhDevice)
		{
			FingerprintSensorEx.CloseDevice(mhDevice);
			mhDevice = 0;
		}
		FingerprintSensorEx.Terminate();
	}
	
	public static void writeBitmap(byte[] imageBuf, int nWidth, int nHeight,
			String path) throws IOException {
		java.io.FileOutputStream fos = new java.io.FileOutputStream(path);
		java.io.DataOutputStream dos = new java.io.DataOutputStream(fos);

		int w = (((nWidth+3)/4)*4);
		int bfType = 0x424d; // ä½�å›¾æ–‡ä»¶ç±»åž‹ï¼ˆ0â€”1å­—èŠ‚ï¼‰
		int bfSize = 54 + 1024 + w * nHeight;// bmpæ–‡ä»¶çš„å¤§å°�ï¼ˆ2â€”5å­—èŠ‚ï¼‰
		int bfReserved1 = 0;// ä½�å›¾æ–‡ä»¶ä¿�ç•™å­—ï¼Œå¿…é¡»ä¸º0ï¼ˆ6-7å­—èŠ‚ï¼‰
		int bfReserved2 = 0;// ä½�å›¾æ–‡ä»¶ä¿�ç•™å­—ï¼Œå¿…é¡»ä¸º0ï¼ˆ8-9å­—èŠ‚ï¼‰
		int bfOffBits = 54 + 1024;// æ–‡ä»¶å¤´å¼€å§‹åˆ°ä½�å›¾å®žé™…æ•°æ�®ä¹‹é—´çš„å­—èŠ‚çš„å��ç§»é‡�ï¼ˆ10-13å­—èŠ‚ï¼‰

		dos.writeShort(bfType); // è¾“å…¥ä½�å›¾æ–‡ä»¶ç±»åž‹'BM'
		dos.write(changeByte(bfSize), 0, 4); // è¾“å…¥ä½�å›¾æ–‡ä»¶å¤§å°�
		dos.write(changeByte(bfReserved1), 0, 2);// è¾“å…¥ä½�å›¾æ–‡ä»¶ä¿�ç•™å­—
		dos.write(changeByte(bfReserved2), 0, 2);// è¾“å…¥ä½�å›¾æ–‡ä»¶ä¿�ç•™å­—
		dos.write(changeByte(bfOffBits), 0, 4);// è¾“å…¥ä½�å›¾æ–‡ä»¶å��ç§»é‡�

		int biSize = 40;// ä¿¡æ�¯å¤´æ‰€éœ€çš„å­—èŠ‚æ•°ï¼ˆ14-17å­—èŠ‚ï¼‰
		int biWidth = nWidth;// ä½�å›¾çš„å®½ï¼ˆ18-21å­—èŠ‚ï¼‰
		int biHeight = nHeight;// ä½�å›¾çš„é«˜ï¼ˆ22-25å­—èŠ‚ï¼‰
		int biPlanes = 1; // ç›®æ ‡è®¾å¤‡çš„çº§åˆ«ï¼Œå¿…é¡»æ˜¯1ï¼ˆ26-27å­—èŠ‚ï¼‰
		int biBitcount = 8;// æ¯�ä¸ªåƒ�ç´ æ‰€éœ€çš„ä½�æ•°ï¼ˆ28-29å­—èŠ‚ï¼‰ï¼Œå¿…é¡»æ˜¯1ä½�ï¼ˆå�Œè‰²ï¼‰ã€�4ä½�ï¼ˆ16è‰²ï¼‰ã€�8ä½�ï¼ˆ256è‰²ï¼‰æˆ–è€…24ä½�ï¼ˆçœŸå½©è‰²ï¼‰ä¹‹ä¸€ã€‚
		int biCompression = 0;// ä½�å›¾åŽ‹ç¼©ç±»åž‹ï¼Œå¿…é¡»æ˜¯0ï¼ˆä¸�åŽ‹ç¼©ï¼‰ï¼ˆ30-33å­—èŠ‚ï¼‰ã€�1ï¼ˆBI_RLEBåŽ‹ç¼©ç±»åž‹ï¼‰æˆ–2ï¼ˆBI_RLE4åŽ‹ç¼©ç±»åž‹ï¼‰ä¹‹ä¸€ã€‚
		int biSizeImage = w * nHeight;// å®žé™…ä½�å›¾å›¾åƒ�çš„å¤§å°�ï¼Œå�³æ•´ä¸ªå®žé™…ç»˜åˆ¶çš„å›¾åƒ�å¤§å°�ï¼ˆ34-37å­—èŠ‚ï¼‰
		int biXPelsPerMeter = 0;// ä½�å›¾æ°´å¹³åˆ†è¾¨çŽ‡ï¼Œæ¯�ç±³åƒ�ç´ æ•°ï¼ˆ38-41å­—èŠ‚ï¼‰è¿™ä¸ªæ•°æ˜¯ç³»ç»Ÿé»˜è®¤å€¼
		int biYPelsPerMeter = 0;// ä½�å›¾åž‚ç›´åˆ†è¾¨çŽ‡ï¼Œæ¯�ç±³åƒ�ç´ æ•°ï¼ˆ42-45å­—èŠ‚ï¼‰è¿™ä¸ªæ•°æ˜¯ç³»ç»Ÿé»˜è®¤å€¼
		int biClrUsed = 0;// ä½�å›¾å®žé™…ä½¿ç”¨çš„é¢œè‰²è¡¨ä¸­çš„é¢œè‰²æ•°ï¼ˆ46-49å­—èŠ‚ï¼‰ï¼Œå¦‚æžœä¸º0çš„è¯�ï¼Œè¯´æ˜Žå…¨éƒ¨ä½¿ç”¨äº†
		int biClrImportant = 0;// ä½�å›¾æ˜¾ç¤ºè¿‡ç¨‹ä¸­é‡�è¦�çš„é¢œè‰²æ•°(50-53å­—èŠ‚)ï¼Œå¦‚æžœä¸º0çš„è¯�ï¼Œè¯´æ˜Žå…¨éƒ¨é‡�è¦�

		dos.write(changeByte(biSize), 0, 4);// è¾“å…¥ä¿¡æ�¯å¤´æ•°æ�®çš„æ€»å­—èŠ‚æ•°
		dos.write(changeByte(biWidth), 0, 4);// è¾“å…¥ä½�å›¾çš„å®½
		dos.write(changeByte(biHeight), 0, 4);// è¾“å…¥ä½�å›¾çš„é«˜
		dos.write(changeByte(biPlanes), 0, 2);// è¾“å…¥ä½�å›¾çš„ç›®æ ‡è®¾å¤‡çº§åˆ«
		dos.write(changeByte(biBitcount), 0, 2);// è¾“å…¥æ¯�ä¸ªåƒ�ç´ å� æ�®çš„å­—èŠ‚æ•°
		dos.write(changeByte(biCompression), 0, 4);// è¾“å…¥ä½�å›¾çš„åŽ‹ç¼©ç±»åž‹
		dos.write(changeByte(biSizeImage), 0, 4);// è¾“å…¥ä½�å›¾çš„å®žé™…å¤§å°�
		dos.write(changeByte(biXPelsPerMeter), 0, 4);// è¾“å…¥ä½�å›¾çš„æ°´å¹³åˆ†è¾¨çŽ‡
		dos.write(changeByte(biYPelsPerMeter), 0, 4);// è¾“å…¥ä½�å›¾çš„åž‚ç›´åˆ†è¾¨çŽ‡
		dos.write(changeByte(biClrUsed), 0, 4);// è¾“å…¥ä½�å›¾ä½¿ç”¨çš„æ€»é¢œè‰²æ•°
		dos.write(changeByte(biClrImportant), 0, 4);// è¾“å…¥ä½�å›¾ä½¿ç”¨è¿‡ç¨‹ä¸­é‡�è¦�çš„é¢œè‰²æ•°

		for (int i = 0; i < 256; i++) {
			dos.writeByte(i);
			dos.writeByte(i);
			dos.writeByte(i);
			dos.writeByte(0);
		}

		byte[] filter = null;
		if (w > nWidth)
		{
			filter = new byte[w-nWidth];
		}
		
		for(int i=0;i<nHeight;i++)
		{
			dos.write(imageBuf, (nHeight-1-i)*nWidth, nWidth);
			if (w > nWidth)
				dos.write(filter, 0, w-nWidth);
		}
		dos.flush();
		dos.close();
		fos.close();
	}

	public static byte[] changeByte(int data) {
		return intToByteArray(data);
	}
	
	public static byte[] intToByteArray (final int number) {
		byte[] abyte = new byte[4];  
	    // "&" ä¸Žï¼ˆANDï¼‰ï¼Œå¯¹ä¸¤ä¸ªæ•´åž‹æ“�ä½œæ•°ä¸­å¯¹åº”ä½�æ‰§è¡Œå¸ƒå°”ä»£æ•°ï¼Œä¸¤ä¸ªä½�éƒ½ä¸º1æ—¶è¾“å‡º1ï¼Œå�¦åˆ™0ã€‚  
	    abyte[0] = (byte) (0xff & number);  
	    // ">>"å�³ç§»ä½�ï¼Œè‹¥ä¸ºæ­£æ•°åˆ™é«˜ä½�è¡¥0ï¼Œè‹¥ä¸ºè´Ÿæ•°åˆ™é«˜ä½�è¡¥1  
	    abyte[1] = (byte) ((0xff00 & number) >> 8);  
	    abyte[2] = (byte) ((0xff0000 & number) >> 16);  
	    abyte[3] = (byte) ((0xff000000 & number) >> 24);  
	    return abyte; 
	}	 
		 
		public static int byteArrayToInt(byte[] bytes) {
			int number = bytes[0] & 0xFF;  
		    // "|="æŒ‰ä½�æˆ–èµ‹å€¼ã€‚  
		    number |= ((bytes[1] << 8) & 0xFF00);  
		    number |= ((bytes[2] << 16) & 0xFF0000);  
		    number |= ((bytes[3] << 24) & 0xFF000000);  
		    return number;  
		 }
	
		private class WorkThread extends Thread {
	        @Override
	        public void run() {
	            super.run();
	            int ret = 0;
	            while (!mbStop) {
	            	templateLen[0] = 2048;
	            	if (0 == (ret = FingerprintSensorEx.AcquireFingerprint(mhDevice, imgbuf, template, templateLen)))
	            	{
	            		if (nFakeFunOn == 1)
                    	{
                    		byte[] paramValue = new byte[4];
            				int[] size = new int[1];
            				size[0] = 4;
            				int nFakeStatus = 0;
            				//GetFakeStatus
            				ret = FingerprintSensorEx.GetParameters(mhDevice, 2004, paramValue, size);
            				nFakeStatus = byteArrayToInt(paramValue);
            				System.out.println("ret = "+ ret +",nFakeStatus=" + nFakeStatus);
            				if (0 == ret && (byte)(nFakeStatus & 31) != 31)
            				{
            					textArea.setText("Is a fake-finer?");
            					return;
            				}
                    	}
                    	OnCatpureOK(imgbuf);
                    	OnExtractOK(template, templateLen[0]);
	            	}
	                try {
	                    Thread.sleep(500);
	                } catch (InterruptedException e) {
	                    e.printStackTrace();
	                }

	            }
	        }

			private void runOnUiThread(Runnable runnable) {
				// TODO Auto-generated method stub
				
			}
	    }
		
		private void OnCatpureOK(byte[] imgBuf)
		{
			try {
				writeBitmap(imgBuf, fpWidth, fpHeight, "fingerprint.bmp");
				btnImg.setIcon(new ImageIcon(ImageIO.read(new File("fingerprint.bmp"))));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		private void OnExtractOK(byte[] template, int len)
		{
			if(bRegister)
			{
				int[] fid = new int[1];
				int[] score = new int [1];
                int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
                if (ret == 0)
                {
                    textArea.setText("the finger already enroll by " + fid[0] + ",cancel enroll");
                    bRegister = false;
                    enroll_idx = 0;
                    return;
                }
                if (enroll_idx > 0 && FingerprintSensorEx.DBMatch(mhDB, regtemparray[enroll_idx-1], template) <= 0)
                {
                	textArea.setText("please press the same finger 3 times for the enrollment");
                    return;
                }
                System.arraycopy(template, 0, regtemparray[enroll_idx], 0, 2048);
                enroll_idx++;
                if (enroll_idx == 3) {
                	int[] _retLen = new int[1];
                    _retLen[0] = 2048;
                    byte[] regTemp = new byte[_retLen[0]];
                    
                    if (0 == (ret = FingerprintSensorEx.DBMerge(mhDB, regtemparray[0], regtemparray[1], regtemparray[2], regTemp, _retLen)) &&
                    		0 == (ret = FingerprintSensorEx.DBAdd(mhDB, iFid, regTemp))) {
                    	iFid++;
                    	cbRegTemp = _retLen[0];
                        System.arraycopy(regTemp, 0, lastRegTemp, 0, cbRegTemp);
                        //Base64 Template
                        textArea.setText("enroll succ");
                    } else {
                    	textArea.setText("enroll fail, error code=" + ret);
                    }
                    bRegister = false;
                } else {
                	textArea.setText("You need to press the " + (3 - enroll_idx) + " times fingerprint");
                }
			}
			else
			{
				if (bIdentify)
				{
					int[] fid = new int[1];
					int[] score = new int [1];
					int ret = FingerprintSensorEx.DBIdentify(mhDB, template, fid, score);
                    if (ret == 0)
                    {
                    	textArea.setText("Identify succ, fid=" + fid[0] + ",score=" + score[0]);
                    }
                    else
                    {
                    	textArea.setText("Identify fail, errcode=" + ret);
                    }
                        
				}
				else
				{
					if(cbRegTemp <= 0)
					{
						textArea.setText("Please register first!");
					}
					else
					{
						int ret = FingerprintSensorEx.DBMatch(mhDB, lastRegTemp, template);
						if(ret > 0)
						{
							textArea.setText("Verify succ, score=" + ret);
						}
						else
						{
							textArea.setText("Verify fail, ret=" + ret);
						}
					}
				}
			}
		}
		
		public static void main(String[] args) {
			new ZKMain().launchFrame();
		}
    
}
