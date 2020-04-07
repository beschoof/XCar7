package com.example.mqttlib;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

@SuppressLint("NewApi")
public class UsbConnection12 extends Connection {

//	private static final String TAG = "UsbConnection12";

   private FileInputStream mFileInputStream;
   private FileOutputStream mFileOutputStream;

   private ParcelFileDescriptor mParcelFileDescriptor;

   UsbAccessory mUsbAccessory;

   Context mContext;

   public UsbConnection12(Activity activity, UsbManager manager) {
      mContext = activity;

      UsbAccessory[] accessories = manager.getAccessoryList();

      UsbAccessory accessory = (accessories == null ? null : accessories[0]);
      if (accessory != null) {
         mUsbAccessory = accessory;
         if (manager.hasPermission(accessory)) {
            mParcelFileDescriptor = manager.openAccessory(accessory);

            if (mParcelFileDescriptor != null) {
               FileDescriptor mFileDescriptor = mParcelFileDescriptor.getFileDescriptor();
               mFileInputStream = new FileInputStream(mFileDescriptor);
               mFileOutputStream = new FileOutputStream(mFileDescriptor);
            } else {
               Toast.makeText(mContext, "Failed to open accessory", Toast.LENGTH_SHORT).show();
               if (mContext instanceof Activity)
                  ((Activity) mContext).finish();
               else if (mContext instanceof Service)
                  ((Service) mContext).stopSelf();
            }

         } else {
         }
      } else {
      }

      IntentFilter mIntentFilter = new IntentFilter();
      mIntentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
      mContext.registerReceiver(mBroadcastReceiver, mIntentFilter);
   }

   public UsbConnection12(Service service, UsbManager manager) {
      mContext = service;

      UsbAccessory[] accessories = manager.getAccessoryList();
      UsbAccessory accessory = (accessories == null ? null : accessories[0]);
      if (accessory != null) {
         mUsbAccessory = accessory;
         if (manager.hasPermission(accessory)) {
            mParcelFileDescriptor = manager.openAccessory(accessory);

            if (mParcelFileDescriptor != null) {
               FileDescriptor mFileDescriptor = mParcelFileDescriptor.getFileDescriptor();
               mFileInputStream = new FileInputStream(mFileDescriptor);
               mFileOutputStream = new FileOutputStream(mFileDescriptor);
            }

         } else {
         }
      } else {
      }

      IntentFilter mIntentFilter = new IntentFilter();
      mIntentFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
      service.registerReceiver(mBroadcastReceiver, mIntentFilter);
   }

   @Override
   public InputStream getInputStream() throws IOException {
      return mFileInputStream;
   }

   @Override
   public OutputStream getOutputStream() throws IOException {
      return mFileOutputStream;
   }

   @Override
   public void close() throws IOException {
      if (mParcelFileDescriptor != null) {
         mParcelFileDescriptor.close();
      }

      mContext.unregisterReceiver(mBroadcastReceiver);
   }

   private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
         if (intent.getAction().equals(UsbManager.ACTION_USB_ACCESSORY_DETACHED)) {
            if (mContext instanceof Activity)
               ((Activity) mContext).finish();
            else if (mContext instanceof Service)
               ((Service) mContext).stopSelf();
         }
      }
   };
}
