package computician.janusclient;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoSink;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;
import computician.janusclientapi.PluginHandleWebRTCCallbacks;

//TODO create message classes unique to this plugin
/**
 * Created by ben.trent on 7/24/2015.
 */
public class VideoRoomTest {
    public static final String REQUEST = "request";
    public static final String MESSAGE = "message";
    public static final String PUBLISHERS = "publishers";
    private JanusPluginHandle handle = null;
    private VideoSink localRender;
    private Deque<VideoSink> availableRemoteRenderers = new ArrayDeque<>();
    private HashMap<BigInteger, VideoSink> remoteRenderers = new HashMap<>();
    private JanusServer janusServer;
    private BigInteger myid;
    final private String user_name = "android";
    final private int roomid = 1234;

    public VideoRoomTest(VideoSink localRender, VideoSink[] remoteRenders) {
        this.localRender = localRender;
        for(int i = 0; i < remoteRenders.length; i++)
        {
            this.availableRemoteRenderers.push(remoteRenders[i]);
        }
        janusServer = new JanusServer(new JanusGlobalCallbacks());
    }

    class ListenerAttachCallbacks implements IJanusPluginCallbacks{
        final private VideoSink renderer;
        final private BigInteger feedid;
        private JanusPluginHandle listener_handle = null;

        public ListenerAttachCallbacks(BigInteger id, VideoSink renderer){
            this.renderer = renderer;
            this.feedid = id;
        }

        public void success(JanusPluginHandle handle) {
            listener_handle = handle;
            try
            {
                JSONObject body = new JSONObject();
                JSONObject msg = new JSONObject();
                body.put(REQUEST, "join");
                body.put("room", roomid);
                body.put("ptype", "listener");
                body.put("feed", feedid);
                msg.put(MESSAGE, body);
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
            catch(Exception ex)
            {

            }
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsep) {

            try {
                String event = msg.getString("videoroom");
                if (event.equals("attached") && jsep != null) {
                    final JSONObject remoteJsep = jsep;
                    listener_handle.createAnswer(new IPluginHandleWebRTCCallbacks() {
                        @Override
                        public void onSuccess(JSONObject obj) {
                            try {
                                JSONObject mymsg = new JSONObject();
                                JSONObject body = new JSONObject();
                                body.put(REQUEST, "start");
                                body.put("room", roomid);
                                mymsg.put(MESSAGE, body);
                                mymsg.put("jsep", obj);
                                listener_handle.sendMessage(new PluginHandleSendMessageCallbacks(mymsg));
                            } catch (Exception ex) {

                            }
                        }

                        @Override
                        public JSONObject getJsep() {
                            return remoteJsep;
                        }

                        @Override
                        public JanusMediaConstraints getMedia() {
                            JanusMediaConstraints cons = new JanusMediaConstraints();
                            cons.setVideo(null);
                            cons.setRecvAudio(true);
                            cons.setRecvVideo(true);
                            cons.setSendAudio(false);
                            return cons;
                        }

                        @Override
                        public Boolean getTrickle() {
                            return true;
                        }

                        @Override
                        public void onCallbackError(String error) {

                        }
                    });
                }
            }
            catch(Exception ex)
            {

            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {

        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            ProxyVideoSink remoteVideoSink = new ProxyVideoSink();
            stream.videoTracks.get(0).addSink(remoteVideoSink);
            remoteVideoSink.setTarget(renderer);
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
        public void onDetached() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPublisherPluginCallbacks implements IJanusPluginCallbacks {

        private void publishOwnFeed() {
            if(handle != null) {
                handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                    @Override
                    public void onSuccess(JSONObject obj) {
                        try
                        {
                            JSONObject msg = new JSONObject();
                            JSONObject body = new JSONObject();
                            body.put(REQUEST, "configure");
                            body.put("audio", true);
                            body.put("video", true);
                            msg.put(MESSAGE, body);
                            msg.put("jsep", obj);
                            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                        }catch (Exception ex) {

                        }
                    }

                    @Override
                    public JSONObject getJsep() {
                        return null;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        JanusMediaConstraints cons = new JanusMediaConstraints();
                        cons.setRecvAudio(false);
                        cons.setRecvVideo(false);
                        cons.setSendAudio(true);
                        return cons;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return true;
                    }

                    @Override
                    public void onCallbackError(String error) {

                    }
                });
            }
        }

        private void registerUsername() {
            if(handle != null) {
                JSONObject obj = new JSONObject();
                JSONObject msg = new JSONObject();
                try
                {
                    obj.put(REQUEST, "join");
                    obj.put("room", roomid);
                    obj.put("ptype", "publisher");
                    obj.put("display", user_name);
                    msg.put(MESSAGE, obj);
                }
                catch(Exception ex)
                {

                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }

        private void newRemoteFeed(BigInteger id) { //todo attach the plugin as a listener
            VideoSink myrenderer;
            if(!remoteRenderers.containsKey(id))
            {
                if(availableRemoteRenderers.isEmpty())
                {
                    //TODO no more space
                    return;
                }
                remoteRenderers.put(id, availableRemoteRenderers.pop());
            }
            myrenderer = remoteRenderers.get(id);
            janusServer.Attach(new ListenerAttachCallbacks(id, myrenderer));
        }

        @Override
        public void success(JanusPluginHandle pluginHandle) {
            handle = pluginHandle;
            registerUsername();
        }

        @Override
        public void onMessage(JSONObject msg, JSONObject jsepLocal) {
            try
            {
                String event = msg.getString("videoroom");
                if(event.equals("joined")) {
                    myid = new BigInteger(msg.getString("id"));
                    publishOwnFeed();
                    if(msg.has(PUBLISHERS)){
                        JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
                            BigInteger tehId = new BigInteger(pub.getString("id"));
                            newRemoteFeed(tehId);
                        }
                    }
                } else if(event.equals("destroyed")) {

                } else if(event.equals("event")) {
                    if(msg.has(PUBLISHERS)){
                        JSONArray pubs = msg.getJSONArray(PUBLISHERS);
                        for(int i = 0; i < pubs.length(); i++) {
                            JSONObject pub = pubs.getJSONObject(i);
                            newRemoteFeed(new BigInteger(pub.getString("id")));
                        }
                    } else if(msg.has("leaving")) {

                    } else if(msg.has("unpublished")) {

                    } else {
                        //todo error
                    }
                }
                if(jsepLocal != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
                }
            }
            catch (Exception ex)
            {

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
            return JanusSupportedPluginPackages.JANUS_VIDEO_ROOM;
        }

        @Override
        public void onCallbackError(String error) {

        }

        @Override
        public void onDetached() {

        }
    }

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {
        public void onSuccess() {
            janusServer.Attach(new JanusPublisherPluginCallbacks());
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

    public boolean initializeMediaContext(Context context){
        return janusServer.initializeMediaContext(context);
    }

    public void Start() {
        janusServer.Connect();
    }

}
