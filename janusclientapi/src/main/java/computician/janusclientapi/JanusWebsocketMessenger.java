package computician.janusclientapi;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;

import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.json.JSONObject;


/**
 * Created by ben.trent on 5/7/2015.
 * Modified by t_saki t_saki@serenegiant.com on 2018
 */
public class JanusWebsocketMessenger implements IJanusMessenger {
	private static final boolean DEBUG = true;	// set false on  production
	private static final String TAG = JanusWebsocketMessenger.class.getSimpleName();
 
    private final String uri;
    private final IJanusMessageObserver handler;
    private final JanusMessengerType type = JanusMessengerType.websocket;
    private WebSocket client = null;

    public JanusWebsocketMessenger(String uri, IJanusMessageObserver handler) {
        this.uri = uri;
        this.handler = handler;
    }

    @Override
    public JanusMessengerType getMessengerType() {
        return type;
    }

    @Override
    public void connect() {
        AsyncHttpClient.getDefaultInstance().websocket(uri, "janus-protocol", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(@Nullable final Exception ex, @Nullable final WebSocket webSocket) {
                if (ex != null) {
                    handler.onError(ex);
                    return;
                }
                if (webSocket == null) {
                    handler.onError(new RuntimeException("webSocket is null"));
                    return;
                }
                client = webSocket;
                client.setWriteableCallback(new WritableCallback() {
                    @Override
                    public void onWriteable() {
                        Log.d("JANUSCLIENT", "On writable");
                    }
                });
                client.setPongCallback(new WebSocket.PongCallback() {

                    @Override
                    public void onPongReceived(String s) {
                        Log.d("JANUSCLIENT", "Pong callback");
                    }
                });
                client.setDataCallback(new DataCallback() {

                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        Log.d("JANUSCLIENT", "New Data");
                    }
                });
                client.setEndCallback(new CompletedCallback() {

                    @Override
                    public void onCompleted(Exception ex) {
                        Log.d("JANUSCLIENT", "Client End");
                    }
                });
                client.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        onMessage(s);
                    }
                });
                client.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        Log.d("JANUSCLIENT", "Socket closed for some reason");
                        if (ex != null) {
                            Log.d("JANUSCLIENT", "SOCKET EX " + ex.getMessage());
                            StringWriter writer = new StringWriter();
                            PrintWriter printWriter = new PrintWriter( writer );
                            ex.printStackTrace( printWriter );
                            printWriter.flush();
                            Log.d("JANUSCLIENT", "StackTrace \n\t" + writer.toString());
                        }
                        if (ex != null) {
                            onError(ex);
                        } else {
                            onClose(-1, "unknown", true);
                        }
                    }
                });

                handler.onOpen();
            }
        });
    }

    @Override
    public void disconnect() {
        client.close();
    }

    @Override
    public void sendMessage(String message) {
        Log.d("JANUSCLIENT", "Sent: \n\t" + message);
        client.send(message);
    }

    @Override
    public void sendMessage(String message, BigInteger session_id) {
        sendMessage(message);
    }

    @Override
    public void sendMessage(String message, BigInteger session_id, BigInteger handle_id) {
        sendMessage(message);
    }

    @Override
    public void receivedMessage(String msg) {
        try {
            JSONObject obj = new JSONObject(msg);
            handler.receivedNewMessage(obj);
        } catch (Exception ex) {
            handler.onError(ex);
        }
    }

    private void onMessage(String message) {
        Log.d("JANUSCLIENT", "Recv: \n\t" + message);
        receivedMessage(message);
    }

    private void onClose(int code, String reason, boolean remote) {
        handler.onClose();
    }

    private void onError(Exception ex) {
        handler.onError(ex);
    }

}
