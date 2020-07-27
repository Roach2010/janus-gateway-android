package computician.janusclient;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoSink;

import java.util.ArrayList;
import java.util.List;

import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;

/**
 * Created by ben.trent on 7/24/2015.
 */

//TODO create message classes unique to this plugin

public class EchoTest {
    private final String TAG = "EchoTest";
    private JanusPluginHandle handle = null;
    private final VideoSink localRender, remoteRender;
    private final JanusServer janusServer;

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {

        @Override
        public void onSuccess() {
            janusServer.Attach(new JanusPluginCallbacks());
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public String getServerUri() {
            return JanusActivity.janusUri;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            return new ArrayList<PeerConnection.IceServer>();
        }

        @Override
        public Boolean getIpv6Support() {
            return Boolean.FALSE;
        }

        @Override
        public Integer getMaxPollEvents() {
            return 0;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPluginCallbacks implements IJanusPluginCallbacks {

        @Override
        public void success(JanusPluginHandle pluginHandle) {
            EchoTest.this.handle = pluginHandle;

                JSONObject msg = new JSONObject();
                JSONObject obj = new JSONObject();
                try {
                    obj.put("audio", true);
                    obj.put("video", true);
                    msg.put("message", obj);
                    handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                } catch (Exception ex) {

                }

            handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                @Override
                public JSONObject getJsep() {
                    return null;
                }

                @Override
                public void onCallbackError(String error) {

                }

                @Override
                public Boolean getTrickle() {
                    return true;
                }

                @Override
                public JanusMediaConstraints getMedia() {
                    return new JanusMediaConstraints();
                }

                @Override
                public void onSuccess(JSONObject obj) {
                    Log.d("JANUSCLIENT", "OnSuccess for CreateOffer called");
                    try {
                        JSONObject body = new JSONObject();
                        JSONObject msg = new JSONObject();
                        body.put("audio", true);
                        body.put("video", true);
                        msg.put("message", body);
                        msg.put("jsep", obj);
                        handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                    } catch (Exception ex) {

                    }
                }
            });

        }

        @Override
        public void onMessage(JSONObject msg, final JSONObject jsepLocal) {
            if(jsepLocal != null)
            {
                handle.handleRemoteJsep(new IPluginHandleWebRTCCallbacks() {
                    final JSONObject myJsep = jsepLocal;
                    @Override
                    public void onSuccess(JSONObject obj) {

                    }

                    @Override
                    public JSONObject getJsep() {
                        return myJsep;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        return null;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return Boolean.FALSE;
                    }

                    @Override
                    public void onCallbackError(String error) {

                    }
                });
            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {
            ProxyVideoSink localVideoSink = new ProxyVideoSink();
            stream.videoTracks.get(0).addSink(localVideoSink);
            localVideoSink.setTarget(localRender);
        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            stream.videoTracks.get(0).setEnabled(true);
            if(stream.videoTracks.get(0).enabled()) {
                Log.d(TAG, "video tracks enabled");
            }
            ProxyVideoSink remoteVideoSink = new ProxyVideoSink();
            stream.videoTracks.get(0).addSink(remoteVideoSink);
            remoteVideoSink.setTarget(remoteRender);
        }

        @Override
        public void onDataOpen(Object data) {

        }

        @Override
        public void onData(Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_ECHO_TEST;
        }

        @Override
        public void onCallbackError(String error) {

        }

        @Override
        public void onDetached() {

        }

    }

    public EchoTest(VideoSink localRender, VideoSink remoteRender) {
        this.localRender = localRender;
        this.remoteRender = remoteRender;
        janusServer = new JanusServer(new JanusGlobalCallbacks());
    }

    public boolean initializeMediaContext(Context context){
        return janusServer.initializeMediaContext(context);
    }

    public void Start() {
        janusServer.Connect();
    }
}
