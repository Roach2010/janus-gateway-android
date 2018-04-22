package computician.janusclient;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSink;

public class JanusActivity extends BaseActivity {
    public static String janusUri;
    final int testCase = 1;
    private boolean activityRunning;
    private GLSurfaceView vsv;
    private SurfaceViewRenderer localRender;
    private SurfaceViewRenderer remoteRender;
    private EchoTest echoTest;
    private VideoRoomTest videoRoomTest;
    private EglBase rootEglBase;

    private void init() {
        try {
            switch (testCase) {
                case 1:
                    echoTest = new EchoTest(localRender, remoteRender);
                    echoTest.initializeMediaContext(JanusActivity.this);
                    echoTest.Start();
                    break;
                case 2:
                    VideoSink[] renderers = new VideoSink[1];
                    renderers[0] = remoteRender;
                    videoRoomTest = new VideoRoomTest(localRender, renderers);
                    videoRoomTest.initializeMediaContext(JanusActivity.this);
                    videoRoomTest.Start();
                    break;
            }

        } catch (Exception ex) {
            Log.e("computician.janusclient", ex.getMessage());
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

        if (!checkPermissionNetwork()) return;
        if (!checkPermissionAudio()) return;
        if (!checkPermissionCamera()) return;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        rootEglBase = EglBase.create();

        localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
        localRender.init(rootEglBase.getEglBaseContext(), null);
        localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localRender.setMirror(true);
        localRender.requestLayout();

        remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);
        remoteRender.init(rootEglBase.getEglBaseContext(), null);
        remoteRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        remoteRender.setMirror(false);
        remoteRender.requestLayout();

        init();
    }

    private void  createRender(SurfaceViewRenderer renderer, boolean mirrored, int videoView) {

    }

    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
    }

    @Override
    protected void onDestroy() {
        disconnect();
        activityRunning = false;
        rootEglBase.release();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityRunning = true;
    }

    private void disconnect() {
        activityRunning = false;
        if (localRender != null) {
            localRender.release();
            localRender = null;
        }
        if (remoteRender != null) {
            remoteRender.release();
            remoteRender = null;
        }
        finish();
    }
}
