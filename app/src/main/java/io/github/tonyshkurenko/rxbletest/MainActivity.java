package io.github.tonyshkurenko.rxbletest;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  public static final String MAC = "98:D3:31:80:89:AF";

  public static final String FIRST_COMMAND = "#10c12n\r";
  public static final String SECOND_COMMAND = "#10c13n\r";

  @BindView(R.id.output) TextView mOutput;
  @BindView(R.id.status) TextView mStatus;
  @BindView(R.id.button_send_first) Button mSendFirst;
  @BindView(R.id.button_send_second) Button mSendSecond;
  @BindView(R.id.connect) Button mConnect;
  @BindView(R.id.devices) ListView mDevices;

  private final Handler mHandler = new Handler();

  private SmoothBluetooth mSmoothBluetooth;

  private ArrayAdapter<String> mAdapter;

  private boolean mConnecting = false;

  private String mMac = MAC;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    ButterKnife.bind(this);

    mSendFirst.setText(FIRST_COMMAND);
    mSendSecond.setText(SECOND_COMMAND);

    setTitle("To connect: " + MAC);

    mAdapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1) {
          @Override public View getView(int position, View convertView, ViewGroup parent) {
            final View view = super.getView(position, convertView, parent);

            if (view != null) {
              ((TextView) view.findViewById(android.R.id.text1)).setTextColor(Color.BLACK);
            }

            return view;
          }
        };

    mDevices.setAdapter(mAdapter);
    mDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final String item = mAdapter.getItem(position);

        final String mac = item.substring(item.length() - MAC.length(), item.length());

        Log.d(TAG, "onItemClick: " + mac);

        mMac = mac;
        setTitle("To connect: " + mMac);
      }
    });

    mSmoothBluetooth = new SmoothBluetooth(this);

    final SmoothBluetooth.Listener listener = new SmoothBluetooth.Listener() {
      @Override public void onBluetoothNotSupported() {
        Log.d(TAG, "onBluetoothNotSupported");
        mStatus.setText("onBluetoothNotSupported");
      }

      @Override public void onBluetoothNotEnabled() {
        Log.d(TAG, "onBluetoothNotEnabled");
        mStatus.setText("onBluetoothNotEnabled");
      }

      @Override public void onConnecting(Device device) {

        if(device == null) {
          return;
        }

        Log.d(TAG, "onConnecting: " + device.getName() + ", " + device.getAddress());
        mStatus.setText("onConnecting: " + device.getName() + ", " + device.getAddress());
      }

      @Override public void onConnected(Device device) {

        if(device == null) {
          return;
        }

        Log.d(TAG, "onConnected: " + device.getName() + ", " + device.getAddress());
        mStatus.setText("onConnected: " + device.getName() + ", " + device.getAddress());

        mConnect.setText("Disconnect");
      }

      @Override public void onDisconnected() {
        Log.d(TAG, "onDisconnected (trying to reconnect)");
        mStatus.setText("onDisconnected (trying to reconnect in 3 seconds)");

        mHandler.postDelayed(new Runnable() {
          @Override public void run() {
            connect();
          }
        }, 3000); // after three seconds

        mConnect.setText("Connect");
      }

      @Override public void onConnectionFailed(Device device) {
        if(device == null) {
          return;
        }

        Log.d(TAG, "onConnectionFailed: " + device.getName() + ", " + device.getAddress());
        mStatus.setText("onConnectionFailed (trying to connect again in 3 seconds): " + device.getName() + ", " + device.getAddress());

        mHandler.postDelayed(new Runnable() {
          @Override public void run() {
            connect();
          }
        }, 3000); // after three seconds
      }

      @Override public void onDiscoveryStarted() {
        Log.d(TAG, "onDiscoveryStarted");
        mStatus.setText("onDiscoveryStarted");
      }

      @Override public void onDiscoveryFinished() {
        Log.d(TAG, "onDiscoveryFinished");
        mStatus.setText("onDiscoveryFinished");
      }

      @Override public void onNoDevicesFound() {
        Log.d(TAG, "onNoDevicesFound");
        mStatus.setText("onNoDevicesFound");
      }

      @Override public void onDevicesFound(List<Device> deviceList,
          SmoothBluetooth.ConnectionCallback connectionCallback) {
        Log.v(TAG, "onDevicesFound() called with: " + "deviceList = [" + deviceList + "]");
        mStatus.setText("onDevicesFound");

        if (!mConnecting) {
          mAdapter.clear();
        }

        for (Device d : deviceList) {
          if (mConnecting && mMac.equals(d.getAddress())) {
            connectionCallback.connectTo(d);
            break;
          }

          if (!mConnecting) {
            mAdapter.add(d.isPaired() ? "PAIRED " : "" + d.getName() + ", " + d.getAddress());
          }
        }
      }

      @Override public void onDataReceived(int data) {
        Log.d(TAG, "onDataReceived() called with: " + "data = [" + data + "]");
        mStatus.setText("onDataReceived");

        mOutput.setText(mOutput.getText() + ", " + data);
      }
    };

    mSmoothBluetooth.setListener(listener);
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    mSmoothBluetooth.stop();
  }

  @OnClick(R.id.button_send_first) void sendFirst() {
    mSmoothBluetooth.send(FIRST_COMMAND);

    mStatus.setText("sendFirstCommand: " + FIRST_COMMAND);

    if (BuildConfig.DEBUG) {
      Log.d(TAG, "sendFirst: " + FIRST_COMMAND);
    }
  }

  @OnClick(R.id.button_send_second) void sendSecond() {
    mSmoothBluetooth.send(SECOND_COMMAND);

    mStatus.setText("sendSecondCommand: " + SECOND_COMMAND);

    if (BuildConfig.DEBUG) {
      Log.d(TAG, "sendSecond: " + SECOND_COMMAND);
    }
  }

  @OnClick(R.id.connect) void connect() {
    mConnecting = true;

    if (!mSmoothBluetooth.isConnected()) {
      mSmoothBluetooth.tryConnection();
    } else {
      mSmoothBluetooth.disconnect();
    }
  }

  @OnClick(R.id.discover) void discover() {
    mConnecting = false;

    mAdapter.clear();
    mAdapter.notifyDataSetChanged();
    mSmoothBluetooth.doDiscovery();
  }
}
