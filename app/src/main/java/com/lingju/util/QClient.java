package com.lingju.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * 客户端接口类
 * <p>
 * Title:
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2014
 * </p>
 * 
 * <p>
 * Company:
 * </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class QClient {
	protected final static String SERVER_NAME="sdk.lingjuai.com";//aimusic.lingjutech.com
	protected final static int CONNECT_TIMEOUT=178000;//理论上是180000，为提高精度，压缩两秒
	protected final static int IDLE=0;
	protected final static int WAITTING_CONNECTION=1;
	protected final static int SENDING=2;
	protected final static int WAITTING_FOR_RESULT=3;
	protected final static int ACCEPTING=4;
	protected static String SERVER_IP;//="192.168.1.9";//123.59.53.2

	protected SocketChannel socketChannel;
	protected Selector selector;
	protected final ExecutorService executorService;
	private final static int BUFFER_LENGTH=1024;
	private final ByteBuffer rBuffer= ByteBuffer.allocate(BUFFER_LENGTH);
	private final ByteBuffer wBuffer= ByteBuffer.allocate(BUFFER_LENGTH);
	private final byte ReadLengthBytes[]=new byte[4];
	private final byte WriteLengthBytes[]=new byte[4];
	private static int read_timeout=20000;
	private final Lock mLock=new ReentrantLock();
	private final Condition mCondition=mLock.newCondition();
	private final Lock sendBlockLock=new ReentrantLock();
	private final AtomicInteger sendState=new AtomicInteger(0);
	private final Condition sendBlockCondtion=sendBlockLock.newCondition();
	private final Lock sendLock=new ReentrantLock(true);
	private final Lock closeLock=new ReentrantLock();
	private String currentMsg;
	private String response;
	private long lastConnectTime;


	private static QClient instance=new QClient();
	public final static int NO_NETWORK=010;
	public final static int BAD_NETWORK=011;
	public final static int INTERRUPTED=020;
	public final static ThreadLocal<Integer> STATUS=new ThreadLocal<Integer>();


	public static String errorMsg(int code){
		switch(code){
			case NO_NETWORK:return "网络不可用";
			case BAD_NETWORK:return "网络不给力";
			case INTERRUPTED:return "异常打断";
		}
		return "";
	}


	private QClient(){
		System.out.println("SocketClient init.......");
		executorService= Executors.newFixedThreadPool(2);
		executorService.execute(new Runnable() {

			@Override
			public void run() {
				Thread.currentThread().setName("SocketClient-monitor-thread");
				try{
					monitoring();
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
		});
	}

	public static QClient getInstance(){
		return instance;
	}

	long openTime;


	public boolean isSessionInvalid(){
		closeLock.lock();
		try{
			return selector==null||socketChannel==null;
		}
		finally{
			closeLock.unlock();
		}
	}


	protected void open() throws Exception {
		if(SERVER_IP==null||SERVER_IP==SERVER_NAME){
			String ip= InetAddress.getByName(SERVER_NAME).getHostAddress();
			if(ip!=null)SERVER_IP=ip;
			else SERVER_IP=SERVER_NAME;
		}
		socketChannel= SocketChannel.open();
		selector= Selector.open();
		socketChannel.configureBlocking(false);
		socketChannel.register(selector, SelectionKey.OP_CONNECT);
		System.out.println("connecting..."+SERVER_IP);
		socketChannel.connect(new InetSocketAddress(SERVER_IP, 81));
		openTime= System.currentTimeMillis();
		mLock.lock();
		try{
			mCondition.signal();
		}
		finally{
			mLock.unlock();
		}
	}

	private byte readCache[];
	private int readOffset;

	private void monitoring(){
		while(true){
			mLock.lock();
			try{
				if(selector==null||socketChannel==null){
					System.out.println("monitoring>>mCondition.await........");
					mCondition.await();
					System.out.println("monitoring>>mCondition.start........");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				mLock.unlock();
			}

			try {
				System.out.println("monitoring>>selector.select wait........");
				selector.select();
				System.out.println("monitoring>>selector.select start........");
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			catch (ClosedSelectorException e) {
				// TODO: handle exception
				e.printStackTrace();
				continue;
			}
			catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				continue;
			}
			Iterator<SelectionKey> it=null;
			try {
				it = selector.selectedKeys().iterator();
			}catch (Throwable e){
				close();
			}
			if(it!=null)
			while(it.hasNext()){
				SelectionKey key=it.next();
				it.remove();
				SocketChannel sc=(SocketChannel) key.channel();
				if(key.isConnectable()){
					try{
						System.out.println("monitoring>>finishConnect.connectTime="+(System.currentTimeMillis()-openTime)+"ms");
						if(sc.isConnectionPending()){
							if(sc.finishConnect()){
								key.interestOps(SelectionKey.OP_READ);
							}
							else{
								key.cancel();
							}
						}
						sendBlockLock.lock();
						try{
							System.out.println("monitoring>>sendBlockCondtion.signal()");
							sendBlockCondtion.signal();
						}
						finally{
							sendBlockLock.unlock();
						}
					}catch(Exception e){
						e.printStackTrace();
						exceptionHandle(e);
						break;
					}
				}
				if(key.isReadable()){
					try{
						int rl=0;
						int l=0;
						if(readCache==null){//第一次接收
							rBuffer.clear();
							setAccepting();
							rl=sc.read(rBuffer);
							lastConnectTime= System.currentTimeMillis();
							System.out.println("read length=" + rl);
							rBuffer.flip();
							System.out.println("p=" + rBuffer.position() + ",l=" + rBuffer.limit());
							if(rBuffer.limit()==0){
								close();
								setIdle();
								break;
							}
							else {
								rBuffer.get(ReadLengthBytes, 0, 4);
								l = getLengthValue(ReadLengthBytes);
								System.out.println("data length=" + l);
								if (l > 0) {
									readCache = new byte[l];
									readOffset = 0;
									if (rl > 4) {
										rBuffer.get(readCache, readOffset, rl - 4);
										readOffset = rl - 4;
									}
									if (rl == BUFFER_LENGTH) {
										rBuffer.clear();
										while ((rl = sc.read(rBuffer)) > 0) {
											System.out.println("read length>>" + rl);
											rBuffer.flip();
											rBuffer.get(readCache, readOffset, rl);
											readOffset += rl;
											rBuffer.clear();
										}
									}
									System.out.println("read>>" + readOffset);
									if (readOffset == l) {//一次性全部接收
										read(readCache, null);
									}
								}
							}
						}
						else{//数据太长，分多次接收，第N次接收（N>navi）
							l=readCache.length;
							while((rl=sc.read(rBuffer))>0){
								System.out.println("read length>>"+rl);
								rBuffer.flip();
								if((readOffset+rl)<l){
									rBuffer.get(readCache, readOffset, rl);
									readOffset+=rl;
								}
								else if((readOffset+rl)==l){
									rBuffer.get(readCache, readOffset, rl);
									readOffset+=rl;
									break;
								}
								else{
									rBuffer.get(readCache, readOffset, l-readOffset);
									readOffset=l;
								}
								rBuffer.clear();
							}
							lastConnectTime= System.currentTimeMillis();
							if(readOffset==l){
								read(readCache,null);
							}
						}
					}catch(Exception e){
						e.printStackTrace();
						read(null,e);
					}
				}
			}
		}
	}

	private boolean setAccepting(){
		if(sendBlockLock.tryLock()){
			try{
				if(sendState.get()==WAITTING_FOR_RESULT) {
					sendState.set(ACCEPTING);
					return true;
				}
			}
			finally {
				sendBlockLock.unlock();
			}
		}
		return false;
	}

	private boolean setIdle(){
		if(sendBlockLock.tryLock()){
			try{
				sendState.set(IDLE);
				return true;
			}
			finally {
				sendBlockLock.unlock();
			}
		}
		return false;
	}

	public static void setReadTimeOut(int timeout){
		if(timeout>0){
			QClient.read_timeout=timeout;
		}
	}

	public String sendMessage(String text){
		return sendMessage(text, read_timeout);
	}

	public String sendMessage(final String text,final int timeout){
		sendLock.lock();
		System.out.println(Thread.currentThread().getName()+">>sendMessage>>" + text);
		try {
			STATUS.set(0);
			return executorService.submit(new ChatCallable(text)).get(timeout, TimeUnit.MILLISECONDS);
		}
		catch(CancellationException e){
			e.printStackTrace();
			//UserContext.getUserContext().getCmd().setStatus(Command.FORCE_INTERRUPT);
			STATUS.set(INTERRUPTED);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			STATUS.set(INTERRUPTED);
			// UserContext.getUserContext().getCmd().setStatus(Command.FORCE_INTERRUPT);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			STATUS.set(BAD_NETWORK);
		}
		catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			STATUS.set(NO_NETWORK);
			//UserContext.getUserContext().getCmd().setStatus(Command.NO_NETWORK);
		}
		catch(Exception e){
			e.printStackTrace();
			STATUS.set(BAD_NETWORK);
		}
		finally{
			if(sendBlockLock.tryLock()){
				try{
					sendState.set(IDLE);
					System.out.println("sendMessage>>sendBlockCondtion.signal()");
					sendBlockCondtion.signal();
				}
				finally{
					sendBlockLock.unlock();
				}
			}
			sendLock.unlock();
		}
		return null;
	}

	private void send(final String text) throws IOException {
		System.out.println("send>>");
		if(null==text||text.length()==0)return;
		sendState.set(SENDING);
		byte[] data=text.getBytes("UTF-8");
		Integer l=data.length;
		//System.out.println("user.length="+l);
		setLengthBytes(WriteLengthBytes,l);
		wBuffer.clear();
		wBuffer.put(WriteLengthBytes);
		//System.out.println("1p="+wBuffer.position()+",l="+wBuffer.limit());
		if(l<=(BUFFER_LENGTH-4)){
			wBuffer.put(data);
			wBuffer.flip();
			socketChannel.write(wBuffer);
		}
		else{
			int offset=BUFFER_LENGTH-4;
			wBuffer.put(data,0,BUFFER_LENGTH-4);
			wBuffer.flip();
			socketChannel.write(wBuffer);
			while(offset<l){
				wBuffer.clear();
				wBuffer.put(data, offset, Math.min(BUFFER_LENGTH, l - offset));
				offset+=BUFFER_LENGTH;
				wBuffer.flip();
				socketChannel.write(wBuffer);
			}
		}
	}

	private void setLengthBytes(byte LengthBytes[],int l){
		LengthBytes[3]=(byte)(l&0xFF);
		LengthBytes[2]=(byte)((l>>8)&0xFF);
		LengthBytes[1]=(byte)((l>>16)&0xFF);
		LengthBytes[0]=(byte)(l>>24&0xFF);
	}

	private int getLengthValue(byte LengthBytes[]){
		return  (LengthBytes[0]&0xFF)<<24|
				(LengthBytes[1]&0xFF)<<16|
				(LengthBytes[2]&0xFF)<<8|
				LengthBytes[3]&0xFF;
	}

	private void read(byte[] data,Exception e){
		sendBlockLock.lock();
		try{
			if(e==null){
				if(data!=null&&data.length>0){
					try {
						response=new String(data,"UTF-8");
						System.out.println("print>>>>>>>"+response);
					} catch (UnsupportedEncodingException ex) {
						// TODO Auto-generated catch block
						ex.printStackTrace();
						response="编码出错";
					}
				}
				else{
					response=null;
					System.out.println("print>>empty");
				}
			}
			else{
				response=null;
				exceptionHandle(e);
			}
			readCache=null;
			readOffset=0;
			sendState.set(IDLE);
			sendBlockCondtion.signal();
		}finally{
			sendBlockLock.unlock();
		}
	}

	protected void close(){
		if(closeLock.tryLock()) {
			try {
				System.out.println("QClient close........................");
				try {
					if (selector != null)
						selector.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					if (socketChannel != null)
						socketChannel.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				selector = null;
				socketChannel = null;
			}finally {
				closeLock.unlock();
			}
		}
	}

	protected void exceptionHandle(Exception e){
       /* if(e instanceof UnknownHostException ||e instanceof ConnectException){
            try{
               // UserContext.getUserContext().getCmd().setStatus(Command.NO_NETWORK);
            }catch(Exception e1){
                e1.printStackTrace();
            }
        }
        else if(e instanceof SocketTimeoutException){
            //强制关闭socket，防止上次没读取的数据本次被读取
            try{
               // UserContext.getUserContext().getCmd().setStatus(Command.BAD_NETWORK);
            }catch(Exception e1){
                e1.printStackTrace();
            }
        }*/
		close();
	}

	private boolean time2ReConnect(){
		return (System.currentTimeMillis()-lastConnectTime)>CONNECT_TIMEOUT;
	}

	class ChatCallable implements Callable<String> {
		private String msg;

		public ChatCallable(String msg){
			this.msg=msg;
		}
		@Override
		public String call() throws Exception{
			sendBlockLock.lock();
			try{
				while(sendState.get()!=IDLE){
					System.out.println("send.call>>sendState.get()!=IDLE");
					sendBlockCondtion.await();
				}
				if(time2ReConnect()){
					System.out.println("time to reconnect.......");
					close();
				}
				if(socketChannel==null||selector==null||!socketChannel.isOpen()||!selector.isOpen()){
					sendState.set(WAITTING_CONNECTION);
					open();
				}
				while(!socketChannel.isConnected()){
					sendState.set(WAITTING_CONNECTION);
					System.out.println("send.call>>WAITTING_CONNECTION");
					sendBlockCondtion.await();
				}
				sendState.set(SENDING);
				currentMsg=this.msg;
				//UserContext.getUserContext().getCmd().setStatus(0);
				send(currentMsg);
				lastConnectTime= System.currentTimeMillis();
				sendState.set(WAITTING_FOR_RESULT);
				System.out.println("send.call>>WAITTING_FOR_RESULT");
				sendBlockCondtion.await();
			}
			catch(Exception e){
				exceptionHandle(e);
				sendState.set(IDLE);
				e.printStackTrace();
				throw e;
			}
			finally{
				sendBlockLock.unlock();
			}
			return response;
		}
	}
}
