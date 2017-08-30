package com.lingju.assistant.service;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources.NotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.lingju.assistant.R;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SensorManagerHelper implements SensorEventListener {

	// 速度阈值，当摇晃速度达到这值后产生作用
	private static final int SPEED_SHRESHOLD = 5000;
	// 两次检测的时间间隔
	private static final int UPTATE_INTERVAL_TIME = 50;
	// 传感器管理器
	private SensorManager sensorManager;
	// 传感器
	private Sensor sensor;
	// 重力感应监听器
	private OnShakeListener onShakeListener;
	// 上下文对象context
	private Context context;
	// 手机上一个位置时重力感应坐标
	private float lastX;
	private float lastY;
	private float lastZ;
	// 上次检测时间
	private long lastUpdateTime;
	private Lock lock=new ReentrantLock();
	private MediaPlayer player;
	private AssetFileDescriptor fd;
	// 构造器
	public SensorManagerHelper(Context context) {
		// 获得监听对象
		this.context = context;
		player=new MediaPlayer();
		try {
			fd = context.getAssets().openFd(context.getResources().getString(R.string.audio_shaking));
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				// TODO Auto-generated method stub
				
			}
		});*/
		start();
	}

	// 开始
	public void start() {
		// 获得传感器管理器
		sensorManager = (SensorManager) context
				.getSystemService(Context.SENSOR_SERVICE);
		if (sensorManager != null) {
			// 获得重力传感器
			sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		// 注册
		if (sensor != null) {
			sensorManager.registerListener(this, sensor,
					SensorManager.SENSOR_DELAY_GAME);
		}
	}

	// 停止检测
	public void stop() {
		sensorManager.unregisterListener(this);
	}

	// 摇晃监听接口
	public interface OnShakeListener {
		/**
		 * 
		 * @return 是否有效的回调
		 */
		public boolean onShake();
	}

	// 设置重力感应监听器
	public void setOnShakeListener(OnShakeListener listener) {
		onShakeListener = listener;
	}

	/*
	 * (non-Javadoc)
	 * android.hardware.SensorEventListener#onAccuracyChanged(android.hardware
	 * .Sensor, int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	/*
	 * 重力感应器感应获得变化数据
	 * android.hardware.SensorEventListener#onSensorChanged(android.hardware
	 * .SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		// 现在检测时间
		long currentUpdateTime = System.currentTimeMillis();
		// 两次检测的时间间隔
		long timeInterval = currentUpdateTime - lastUpdateTime;
		// 判断是否达到了检测时间间隔
		if (timeInterval < UPTATE_INTERVAL_TIME) return;
		// 现在的时间变成last时间
		lastUpdateTime = currentUpdateTime;
		// 获得x,y,z坐标
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		// 获得x,y,z的变化值
		float deltaX = x - lastX;
		float deltaY = y - lastY;
		float deltaZ = z - lastZ;
		// 将现在的坐标变成last坐标
		lastX = x;
		lastY = y;
		lastZ = z;
		double speed = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
				* deltaZ)
				/ timeInterval * 10000;
		// 达到速度阀值，发出提示
		if (speed >= SPEED_SHRESHOLD){
			System.out.println("摇晃中。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。");
			if(lock.tryLock()){
				try{
					if(!player.isPlaying()){
						System.out.println("摇晃回调。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。。");
						if(onShakeListener.onShake())
						try {
							player.reset();
							player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
							player.setAudioStreamType(AudioManager.STREAM_MUSIC);
							player.setLooping(false);
							player.prepare();
							player.start();
						} catch (IllegalArgumentException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}finally{
					lock.unlock();
				}
			}
		}
	}
}
