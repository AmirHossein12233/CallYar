package com.callyar.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

public class CallActivity extends AppCompatActivity
        implements CallYarSocket.Listener {

    private static final int PERMISSION_REQUEST = 1001;

    private SurfaceViewRenderer remoteVideoView;
    private SurfaceViewRenderer localVideoView;

    private TextView txtCallStatus;

    private Button btnMute;
    private Button btnCamera;
    private Button btnSwitchCamera;
    private Button btnHangup;

    private CallYarManager callYarManager;

    private String username;
    private String targetUser;
    private String mode;

    private boolean caller;
    private boolean muted = false;
    private boolean cameraEnabled = true;

    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;

    private EglBase eglBase;

    private VideoCapturer videoCapturer;
    private SurfaceTextureHelper surfaceTextureHelper;

    private VideoSource videoSource;
    private AudioSource audioSource;

    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private final List<IceCandidate> pendingIceCandidates =
            new ArrayList<>();

    /*
     * پیام‌هایی که قبل از آماده شدن WebRTC
     * دریافت شوند، اینجا نگه داشته می‌شوند.
     */
    private final List<JSONObject> pendingMessages =
            new ArrayList<>();

    private boolean remoteDescriptionSet = false;

    private final List<PeerConnection.IceServer> iceServers =
            new ArrayList<>();

    private boolean callStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_call);

        remoteVideoView =
                findViewById(R.id.remoteVideoView);

        localVideoView =
                findViewById(R.id.localVideoView);

        txtCallStatus =
                findViewById(R.id.txtCallStatus);

        btnMute =
                findViewById(R.id.btnMute);

        btnCamera =
                findViewById(R.id.btnCamera);

        btnSwitchCamera =
                findViewById(R.id.btnSwitchCamera);

        btnHangup =
                findViewById(R.id.btnHangup);

        username =
                getIntent().getStringExtra("username");

        targetUser =
                getIntent().getStringExtra("target");

        mode =
                getIntent().getStringExtra("mode");

        caller =
                getIntent().getBooleanExtra(
                        "caller",
                        false
                );

        if (username == null) {
            username = "";
        }

        if (targetUser == null) {
            targetUser = "";
        }

        if (mode == null) {
            mode = "voice";
        }

        callYarManager =
                CallYarManager.getInstance();

        /*
         * قبل از شروع WebRTC Listener را ثبت می‌کنیم.
         */
        callYarManager.setListener(this);

        if ("voice".equals(mode)) {

            cameraEnabled = false;

            localVideoView.setVisibility(
                    View.GONE
            );

            remoteVideoView.setVisibility(
                    View.GONE
            );

            btnCamera.setVisibility(
                    View.GONE
            );

            btnSwitchCamera.setVisibility(
                    View.GONE
            );

        } else {

            cameraEnabled = true;

            localVideoView.setVisibility(
                    View.VISIBLE
            );

            remoteVideoView.setVisibility(
                    View.VISIBLE
            );

            btnCamera.setVisibility(
                    View.VISIBLE
            );

            btnSwitchCamera.setVisibility(
                    View.VISIBLE
            );
        }

        txtCallStatus.setText(
                "در حال اتصال به "
                        + targetUser
                        + "..."
        );

        btnMute.setOnClickListener(
                v -> toggleMute()
        );

        btnCamera.setOnClickListener(
                v -> toggleCamera()
        );

        btnSwitchCamera.setOnClickListener(
                v -> switchCamera()
        );

        btnHangup.setOnClickListener(
                v -> hangup()
        );

        if (hasPermissions()) {

            startCall();

        } else {

            requestPermissions();
        }
    }

    private boolean hasPermissions() {

        boolean audio =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED;

        if ("voice".equals(mode)) {
            return audio;
        }

        boolean camera =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED;

        return audio && camera;
    }

    private void requestPermissions() {

        if ("voice".equals(mode)) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    PERMISSION_REQUEST
            );

        } else {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CAMERA
                    },
                    PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == PERMISSION_REQUEST) {

            if (hasPermissions()) {

                startCall();

            } else {

                Toast.makeText(
                        this,
                        "دسترسی لازم داده نشد",
                        Toast.LENGTH_LONG
                ).show();

                finish();
            }
        }
    }

    private void startCall() {

        if (callStarted) {
            return;
        }

        callStarted = true;

        initializeWebRTC();

        if (callYarManager.isConnected()) {

            txtCallStatus.setText(
                    "🟢 اتصال سیگنالینگ برقرار است"
            );

            /*
             * فقط تماس‌گیرنده Offer می‌سازد.
             */
            if (caller) {

                createOffer();
            }

        } else {

            txtCallStatus.setText(
                    "🔴 اتصال به سرور برقرار نیست"
            );
        }
    }

    private void initializeWebRTC() {

        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions
                        .builder(
                                getApplicationContext()
                        )
                        .setEnableInternalTracer(true)
                        .createInitializationOptions()
        );

        eglBase =
                EglBase.create();

        if (!"voice".equals(mode)) {

            remoteVideoView.init(
                    eglBase.getEglBaseContext(),
                    null
            );

            localVideoView.init(
                    eglBase.getEglBaseContext(),
                    null
            );

            localVideoView.setMirror(true);
        }

        PeerConnectionFactory.Options options =
                new PeerConnectionFactory.Options();

        DefaultVideoEncoderFactory encoderFactory =
                new DefaultVideoEncoderFactory(
                        eglBase.getEglBaseContext(),
                        true,
                        true
                );

        DefaultVideoDecoderFactory decoderFactory =
                new DefaultVideoDecoderFactory(
                        eglBase.getEglBaseContext()
                );

        peerConnectionFactory =
                PeerConnectionFactory.builder()
                        .setOptions(options)
                        .setVideoEncoderFactory(
                                encoderFactory
                        )
                        .setVideoDecoderFactory(
                                decoderFactory
                        )
                        .createPeerConnectionFactory();

        if (!"voice".equals(mode)) {

            setupLocalVideo();
        }

        setupLocalAudio();

        iceServers.clear();

        iceServers.add(
                PeerConnection.IceServer
                        .builder(
                                "stun:stun.l.google.com:19302"
                        )
                        .createIceServer()
        );

        PeerConnection.RTCConfiguration configuration =
                new PeerConnection.RTCConfiguration(
                        iceServers
                );

        configuration.sdpSemantics =
                PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection =
                peerConnectionFactory.createPeerConnection(
                        configuration,
                        new PeerConnection.Observer() {

                            @Override
                            public void onSignalingChange(
                                    PeerConnection.SignalingState state) {
                            }

                            @Override
                            public void onIceConnectionChange(
                                    PeerConnection.IceConnectionState state) {

                                runOnUiThread(() -> {

                                    if (state ==
                                            PeerConnection.IceConnectionState.CONNECTED ||
                                            state ==
                                            PeerConnection.IceConnectionState.COMPLETED) {

                                        txtCallStatus.setText(
                                                "🟢 تماس برقرار شد"
                                        );
                                    }

                                    if (state ==
                                            PeerConnection.IceConnectionState.FAILED) {

                                        txtCallStatus.setText(
                                                "🔴 اتصال WebRTC ناموفق"
                                        );
                                    }
                                });
                            }

                            @Override
                            public void onIceConnectionReceivingChange(
                                    boolean receiving) {
                            }

                            @Override
                            public void onIceGatheringChange(
                                    PeerConnection.IceGatheringState state) {
                            }

                            @Override
                            public void onIceCandidate(
                                    IceCandidate candidate) {

                                sendIceCandidate(
                                        candidate
                                );
                            }

                            @Override
                            public void onIceCandidatesRemoved(
                                    IceCandidate[] candidates) {
                            }

                            @Override
                            public void onAddStream(
                                    MediaStream mediaStream) {

                                if ("voice".equals(mode)) {
                                    return;
                                }

                                if (mediaStream.videoTracks != null
                                        && !mediaStream.videoTracks.isEmpty()) {

                                    VideoTrack remoteVideo =
                                            mediaStream.videoTracks.get(0);

                                    runOnUiThread(() ->
                                            remoteVideo.addSink(
                                                    remoteVideoView
                                            )
                                    );
                                }
                            }

                            @Override
                            public void onRemoveStream(
                                    MediaStream mediaStream) {
                            }

                            @Override
                            public void onDataChannel(
                                    DataChannel dataChannel) {
                            }

                            @Override
                            public void onRenegotiationNeeded() {
                            }

                            @Override
                            public void onAddTrack(
                                    RtpReceiver receiver,
                                    MediaStream[] mediaStreams) {

                                attachRemoteTrack(
                                        receiver
                                );
                            }

                            @Override
                            public void onTrack(
                                    RtpTransceiver transceiver) {

                                if (transceiver == null) {
                                    return;
                                }

                                RtpReceiver receiver =
                                        transceiver.getReceiver();

                                attachRemoteTrack(
                                        receiver
                                );
                            }

                            @Override
                            public void onConnectionChange(
                                    PeerConnection.PeerConnectionState state) {

                                runOnUiThread(() -> {

                                    switch (state) {

                                        case NEW:

                                            txtCallStatus.setText(
                                                    "در حال آماده‌سازی..."
                                            );

                                            break;

                                        case CONNECTING:

                                            txtCallStatus.setText(
                                                    "🟡 در حال اتصال..."
                                            );

                                            break;

                                        case CONNECTED:

                                            txtCallStatus.setText(
                                                    "🟢 تماس برقرار شد"
                                            );

                                            break;

                                        case DISCONNECTED:

                                            txtCallStatus.setText(
                                                    "🔴 اتصال قطع شد"
                                            );

                                            break;

                                        case FAILED:

                                            txtCallStatus.setText(
                                                    "🔴 اتصال ناموفق"
                                            );

                                            break;

                                        case CLOSED:

                                            txtCallStatus.setText(
                                                    "تماس پایان یافت"
                                            );

                                            break;
                                    }
                                });
                            }
                        }
                );

        if (peerConnection == null) {

            Toast.makeText(
                    this,
                    "خطا در ساخت WebRTC",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        if (localAudioTrack != null) {

            peerConnection.addTrack(
                    localAudioTrack
            );
        }

        if (localVideoTrack != null) {

            peerConnection.addTrack(
                    localVideoTrack
            );
        }

        /*
         * اگر پیام‌هایی قبل از آماده شدن WebRTC رسیده‌اند،
         * حالا پردازششان می‌کنیم.
         */
        if (!pendingMessages.isEmpty()) {

            List<JSONObject> messages =
                    new ArrayList<>(
                            pendingMessages
                    );

            pendingMessages.clear();

            for (JSONObject message : messages) {

                handleSignalingMessage(
                        message
                );
            }
        }
    }

    private void attachRemoteTrack(
            RtpReceiver receiver) {

        if (receiver == null) {
            return;
        }

        if ("voice".equals(mode)) {
            return;
        }

        MediaStreamTrack track =
                receiver.track();

        if (track instanceof VideoTrack) {

            VideoTrack videoTrack =
                    (VideoTrack) track;

            runOnUiThread(() ->
                    videoTrack.addSink(
                            remoteVideoView
                    )
            );
        }
    }

    private void setupLocalAudio() {

        MediaConstraints constraints =
                new MediaConstraints();

        audioSource =
                peerConnectionFactory.createAudioSource(
                        constraints
                );

        localAudioTrack =
                peerConnectionFactory.createAudioTrack(
                        "LOCAL_AUDIO",
                        audioSource
                );
    }

    private void setupLocalVideo() {

        CameraEnumerator enumerator =
                new Camera2Enumerator(this);

        String[] deviceNames =
                enumerator.getDeviceNames();

        String selectedCamera = null;

        for (String name : deviceNames) {

            if (enumerator.isFrontFacing(name)) {

                selectedCamera = name;
                break;
            }
        }

        if (selectedCamera == null
                && deviceNames.length > 0) {

            selectedCamera =
                    deviceNames[0];
        }

        if (selectedCamera == null) {
            return;
        }

        videoCapturer =
                enumerator.createCapturer(
                        selectedCamera,
                        null
                );

        if (videoCapturer == null) {
            return;
        }

        surfaceTextureHelper =
                SurfaceTextureHelper.create(
                        "CallYarCamera",
                        eglBase.getEglBaseContext()
                );

        videoSource =
                peerConnectionFactory.createVideoSource(
                        videoCapturer.isScreencast()
                );

        videoCapturer.initialize(
                surfaceTextureHelper,
                getApplicationContext(),
                videoSource.getCapturerObserver()
        );

        try {

            videoCapturer.startCapture(
                    640,
                    480,
                    30
            );

        } catch (Exception ignored) {
        }

        localVideoTrack =
                peerConnectionFactory.createVideoTrack(
                        "LOCAL_VIDEO",
                        videoSource
                );

        localVideoTrack.addSink(
                localVideoView
        );
    }

    @Override
    public void onConnected() {

        runOnUiThread(() -> {

            txtCallStatus.setText(
                    "🟢 اتصال سیگنالینگ برقرار شد"
            );

            if (caller) {

                createOffer();
            }
        });
    }

    @Override
    public void onUsers(
            String[] users) {

        // در صفحه تماس نیازی به لیست کاربران نیست.
    }

    @Override
    public void onMessage(
            String message) {

        if (message == null ||
                message.trim().isEmpty()) {

            return;
        }

        runOnUiThread(() -> {

            try {

                JSONObject json =
                        new JSONObject(message);

                String type =
                        json.optString(
                                "type",
                                ""
                        );

                /*
                 * پیام call فقط برای باز کردن CallActivity است.
                 * HomeActivity این کار را انجام داده است.
                 */
                if ("call".equals(type)) {
                    return;
                }

                /*
                 * اگر WebRTC هنوز ساخته نشده،
                 * پیام را نگه می‌داریم.
                 */
                if (peerConnection == null) {

                    pendingMessages.add(json);

                    return;
                }

                handleSignalingMessage(
                        json
                );

            } catch (Exception e) {

                showError(
                        "خطا در دریافت پیام تماس"
                );
            }
        });
    }

    private void handleSignalingMessage(
            JSONObject json) {

        try {

            String type =
                    json.optString(
                            "type",
                            ""
                    );

            JSONObject data =
                    json.optJSONObject(
                            "data"
                    );

            if ("hangup".equals(type)) {

                Toast.makeText(
                        CallActivity.this,
                        "تماس توسط طرف مقابل پایان یافت",
                        Toast.LENGTH_SHORT
                ).show();

                finish();

                return;
            }

            if (data == null) {
                return;
            }

            if ("offer".equals(type)) {

                handleOffer(data);

            } else if ("answer".equals(type)) {

                handleAnswer(data);

            } else if ("candidate".equals(type)) {

                handleCandidate(data);
            }

        } catch (Exception e) {

            showError(
                    "خطا در پردازش پیام تماس"
            );
        }
    }

    private void createOffer() {

        if (peerConnection == null) {
            return;
        }

        MediaConstraints constraints =
                new MediaConstraints();

        peerConnection.createOffer(
                new SimpleSdpObserver() {

                    @Override
                    public void onCreateSuccess(
                            SessionDescription description) {

                        peerConnection.setLocalDescription(
                                new SimpleSdpObserver() {

                                    @Override
                                    public void onSetSuccess() {

                                        sendSdp(
                                                "offer",
                                                description
                                        );
                                    }
                                },
                                description
                        );
                    }

                    @Override
                    public void onCreateFailure(
                            String error) {

                        showError(
                                "خطا در ساخت Offer: "
                                        + error
                        );
                    }
                },
                constraints
        );
    }

    private void createAnswer() {

        if (peerConnection == null) {
            return;
        }

        MediaConstraints constraints =
                new MediaConstraints();

        peerConnection.createAnswer(
                new SimpleSdpObserver() {

                    @Override
                    public void onCreateSuccess(
                            SessionDescription description) {

                        peerConnection.setLocalDescription(
                                new SimpleSdpObserver() {

                                    @Override
                                    public void onSetSuccess() {

                                        sendSdp(
                                                "answer",
                                                description
                                        );
                                    }
                                },
                                description
                        );
                    }

                    @Override
                    public void onCreateFailure(
                            String error) {

                        showError(
                                "خطا در ساخت Answer: "
                                        + error
                        );
                    }
                },
                constraints
        );
    }

    private void sendSdp(
            String type,
            SessionDescription description) {

        if (!callYarManager.isConnected()) {

            showError(
                    "اتصال سیگنالینگ برقرار نیست"
            );

            return;
        }

        try {

            JSONObject data =
                    new JSONObject();

            data.put(
                    "sdp",
                    description.description
            );

            data.put(
                    "sdpType",
                    description.type.canonicalForm()
            );

            JSONObject message =
                    new JSONObject();

            message.put(
                    "type",
                    type
            );

            message.put(
                    "target",
                    targetUser
            );

            message.put(
                    "data",
                    data
            );

            callYarManager.sendJson(
                    message
            );

        } catch (Exception e) {

            showError(
                    "خطا در ارسال SDP"
            );
        }
    }

    private void sendIceCandidate(
            IceCandidate candidate) {

        if (!callYarManager.isConnected()) {
            return;
        }

        try {

            JSONObject data =
                    new JSONObject();

            data.put(
                    "sdpMid",
                    candidate.sdpMid
            );

            data.put(
                    "sdpMLineIndex",
                    candidate.sdpMLineIndex
            );

            data.put(
                    "candidate",
                    candidate.sdp
            );

            JSONObject message =
                    new JSONObject();

            message.put(
                    "type",
                    "candidate"
            );

            message.put(
                    "target",
                    targetUser
            );

            message.put(
                    "data",
                    data
            );

            callYarManager.sendJson(
                    message
            );

        } catch (Exception ignored) {
        }
    }

    private void handleOffer(
            JSONObject data) {

        if (peerConnection == null) {
            return;
        }

        try {

            String sdp =
                    data.optString(
                            "sdp",
                            ""
                    );

            if (sdp.isEmpty()) {
                return;
            }

            SessionDescription description =
                    new SessionDescription(
                            SessionDescription.Type.OFFER,
                            sdp
                    );

            peerConnection.setRemoteDescription(
                    new SimpleSdpObserver() {

                        @Override
                        public void onSetSuccess() {

                            remoteDescriptionSet =
                                    true;

                            addPendingIceCandidates();

                            createAnswer();
                        }

                        @Override
                        public void onSetFailure(
                                String error) {

                            showError(
                                    "خطا در دریافت Offer: "
                                            + error
                            );
                        }
                    },
                    description
            );

        } catch (Exception e) {

            showError(
                    "خطا در پردازش Offer"
            );
        }
    }

    private void handleAnswer(
            JSONObject data) {

        if (peerConnection == null) {
            return;
        }

        try {

            String sdp =
                    data.optString(
                            "sdp",
                            ""
                    );

            if (sdp.isEmpty()) {
                return;
            }

            SessionDescription description =
                    new SessionDescription(
                            SessionDescription.Type.ANSWER,
                            sdp
                    );

            peerConnection.setRemoteDescription(
                    new SimpleSdpObserver() {

                        @Override
                        public void onSetSuccess() {

                            remoteDescriptionSet =
                                    true;

                            addPendingIceCandidates();

                            txtCallStatus.setText(
                                    "🟢 پاسخ تماس دریافت شد"
                            );
                        }

                        @Override
                        public void onSetFailure(
                                String error) {

                            showError(
                                    "خطا در دریافت Answer: "
                                            + error
                            );
                        }
                    },
                    description
            );

        } catch (Exception e) {

            showError(
                    "خطا در پردازش Answer"
            );
        }
    }

    private void handleCandidate(
            JSONObject data) {

        if (peerConnection == null) {
            return;
        }

        try {

            String sdpMid =
                    data.optString(
                            "sdpMid",
                            ""
                    );

            int sdpMLineIndex =
                    data.optInt(
                            "sdpMLineIndex",
                            0
                    );

            String candidateText =
                    data.optString(
                            "candidate",
                            ""
                    );

            if (candidateText.isEmpty()) {
                return;
            }

            IceCandidate candidate =
                    new IceCandidate(
                            sdpMid,
                            sdpMLineIndex,
                            candidateText
                    );

            if (remoteDescriptionSet) {

                peerConnection.addIceCandidate(
                        candidate
                );

            } else {

                pendingIceCandidates.add(
                        candidate
                );
            }

        } catch (Exception e) {

            showError(
                    "خطا در دریافت Candidate"
            );
        }
    }

    private void addPendingIceCandidates() {

        if (peerConnection == null) {
            return;
        }

        for (IceCandidate candidate :
                pendingIceCandidates) {

            peerConnection.addIceCandidate(
                    candidate
            );
        }

        pendingIceCandidates.clear();
    }

    private void toggleMute() {

        muted = !muted;

        if (localAudioTrack != null) {

            localAudioTrack.setEnabled(
                    !muted
            );
        }

        btnMute.setText(
                muted
                        ? "🔇 میکروفون"
                        : "🎤 میکروفون"
        );
    }

    private void toggleCamera() {

        if ("voice".equals(mode)) {
            return;
        }

        cameraEnabled =
                !cameraEnabled;

        if (localVideoTrack != null) {

            localVideoTrack.setEnabled(
                    cameraEnabled
            );
        }

        btnCamera.setText(
                cameraEnabled
                        ? "📷 دوربین"
                        : "🚫 دوربین"
        );
    }

    private void switchCamera() {

        if (videoCapturer
                instanceof CameraVideoCapturer) {

            CameraVideoCapturer camera =
                    (CameraVideoCapturer)
                            videoCapturer;

            camera.switchCamera(
                    new CameraVideoCapturer.CameraSwitchHandler() {

                        @Override
                        public void onCameraSwitchDone(
                                boolean isFrontCamera) {

                            if (localVideoView != null) {

                                localVideoView.setMirror(
                                        isFrontCamera
                                );
                            }
                        }

                        @Override
                        public void onCameraSwitchError(
                                String errorDescription) {

                            Toast.makeText(
                                    CallActivity.this,
                                    "خطا در تغییر دوربین",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        }
    }

    private void hangup() {

        try {

            if (callYarManager != null &&
                    callYarManager.isConnected()) {

                JSONObject message =
                        new JSONObject();

                message.put(
                        "type",
                        "hangup"
                );

                message.put(
                        "target",
                        targetUser
                );

                message.put(
                        "data",
                        new JSONObject()
                );

                callYarManager.sendJson(
                        message
                );
            }

        } catch (Exception ignored) {
        }

        finish();
    }

    private void showError(
            String error) {

        runOnUiThread(() -> {

            if (txtCallStatus != null) {

                txtCallStatus.setText(
                        "🔴 " + error
                );
            }

            Toast.makeText(
                    CallActivity.this,
                    error,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    public void onDisconnected() {

        runOnUiThread(() ->
                txtCallStatus.setText(
                        "🔴 اتصال سیگنالینگ قطع شد"
                )
        );
    }

    @Override
    public void onError(
            String error) {

        showError(error);
    }

    @Override
    protected void onDestroy() {

        /*
         * WebSocket را قطع نمی‌کنیم.
         * اتصال توسط CallYarManager مدیریت می‌شود.
         */
        if (callYarManager != null) {

            callYarManager.clearListener(
                    this
            );
        }

        try {

            if (videoCapturer != null) {

                try {

                    videoCapturer.stopCapture();

                } catch (Exception ignored) {
                }

                videoCapturer.dispose();

                videoCapturer = null;
            }

            if (localVideoView != null) {

                localVideoView.release();
            }

            if (remoteVideoView != null) {

                remoteVideoView.release();
            }

            if (peerConnection != null) {

                peerConnection.close();

                peerConnection = null;
            }

            if (videoSource != null) {

                videoSource.dispose();

                videoSource = null;
            }

            if (audioSource != null) {

                audioSource.dispose();

                audioSource = null;
            }

            if (surfaceTextureHelper != null) {

                surfaceTextureHelper.dispose();

                surfaceTextureHelper = null;
            }

            if (peerConnectionFactory != null) {

                peerConnectionFactory.dispose();

                peerConnectionFactory = null;
            }

            if (eglBase != null) {

                eglBase.release();

                eglBase = null;
            }

        } catch (Exception ignored) {
        }

        super.onDestroy();
    }

    private static class SimpleSdpObserver
            implements SdpObserver {

        @Override
        public void onCreateSuccess(
                SessionDescription description) {
        }

        @Override
        public void onSetSuccess() {
        }

        @Override
        public void onCreateFailure(
                String error) {
        }

        @Override
        public void onSetFailure(
                String error) {
        }
    }
}