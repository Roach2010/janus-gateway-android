package computician.janusclient;

import android.app.Activity;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class JanusActivity extends Activity {
    public static String janusUri;
    final int testCase = 1;
    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private EchoTest echoTest;
    private VideoRoomTest videoRoomTest;

    private class MyInit implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                EGLContext con = VideoRendererGui.getEGLContext();
                switch (testCase) {
                    case 1:
                        echoTest = new EchoTest(localRender, remoteRender);
                        echoTest.initializeMediaContext(JanusActivity.this, true, true, true, con);
                        echoTest.Start();
                        break;
                    case 2:
                        VideoRenderer.Callbacks[] renderers = new VideoRenderer.Callbacks[1];
                        renderers[0] = remoteRender;
                        videoRoomTest = new VideoRoomTest(localRender, renderers);
                        videoRoomTest.initializeMediaContext(JanusActivity.this, true, true, true, con);
                        videoRoomTest.Start();
                        break;
                }

            } catch (Exception ex) {
                Log.e("computician.janusclient", ex.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        janusUri = getString(R.string.janus_uri);
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_janus);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        vsv = (GLSurfaceView) findViewById(R.id.glview);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new MyInit());

        localRender = VideoRendererGui.create(72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        remoteRender = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }
}
