package com.kaamchor.ui.call

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kaamchor.R
import com.kaamchor.data.ApiClient
import com.kaamchor.data.InitiateCallRequest
import com.kaamchor.data.SocketClient
import com.kaamchor.databinding.ActivityCallBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.MediaConstraints
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

import org.webrtc.IceCandidate
import org.webrtc.MediaStream

class CallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallBinding
    private val tag = "CallActivity"

    private var groupId: Int = -1
    private var isGroup: Boolean = false
    private var isCaller: Boolean = false
    private var callId: Int? = null
    private var toUserId: Int? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var isMuted = false
    private var isSpeakerOn = true

    private val iceServers = listOf(
        org.webrtc.PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        org.webrtc.PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getIntExtra("group_id", -1)
        isGroup = intent.getBooleanExtra("is_group", false)
        isCaller = intent.getBooleanExtra("is_caller", false)
        toUserId = intent.getIntExtra("to_user_id", -1).let { if (it == -1) null else it }

        setupUI()

        if (checkPermissions()) {
            startCall()
        } else {
            requestPermissions()
        }
    }

    private fun setupUI() {
        binding.btnEndCall.setOnClickListener { endCall() }
        binding.btnMute.setOnClickListener { toggleMute() }
        binding.btnSpeaker.setOnClickListener { toggleSpeaker() }

        binding.tvStatus.text = if (isCaller) getString(R.string.calling) else getString(R.string.incoming_call)
    }

    private fun startCall() {
        initWebRTC()
        setupSocketListeners()

        if (isCaller) {
            initiateCall()
        }
    }

    private fun initWebRTC() {
        try {
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(this)
                    .createInitializationOptions()
            )

            val options = PeerConnectionFactory.Options()
            val rootEglBase = org.webrtc.EglBase.create()
            val videoEncoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
            val videoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(videoEncoderFactory)
                .setVideoDecoderFactory(videoDecoderFactory)
                .createPeerConnectionFactory()

            val audioConstraints = MediaConstraints()
            val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
            localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)

            val config = PeerConnection.RTCConfiguration(iceServers)
            config.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            config.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY

            peerConnection = peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
                override fun onIceCandidate(candidate: IceCandidate?) {
                    Log.d(tag, "ICE candidate generated")
                    if (candidate != null) {
                        val candidateData = JSONObject()
                        candidateData.put("sdp", candidate.sdp)
                        candidateData.put("sdpMid", candidate.sdpMid)
                        candidateData.put("sdpMLineIndex", candidate.sdpMLineIndex)
                        val targetUser = toUserId
                        if (targetUser != null) {
                            SocketClient.emitIceCandidate(targetUser, candidateData)
                        }
                    }
                }

                override fun onAddStream(stream: MediaStream?) {
                    Log.d(tag, "Remote stream added")
                    val remoteAudioTrack = stream?.audioTracks?.firstOrNull()
                    remoteAudioTrack?.setEnabled(true)
                }

                override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out MediaStream>?) {}
                override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
                override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                    runOnUiThread {
                        when (state) {
                            PeerConnection.IceConnectionState.CONNECTED -> {
                                binding.tvStatus.text = "Connected"
                            }
                            PeerConnection.IceConnectionState.DISCONNECTED -> {
                                binding.tvStatus.text = "Disconnected"
                                endCall()
                            }
                            PeerConnection.IceConnectionState.FAILED -> {
                                binding.tvStatus.text = "Connection failed"
                                endCall()
                            }
                            else -> {}
                        }
                    }
                }
                override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
                override fun onRemoveStream(stream: MediaStream?) {}
                override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
                override fun onTrack(transceiver: org.webrtc.RtpTransceiver?) {}
                override fun onRenegotiationNeeded() {}
            })

            localAudioTrack?.let { track ->
                peerConnection?.addTrack(track)
            }

        } catch (e: Exception) {
            Log.e(tag, "WebRTC init failed", e)
            if (!isFinishing) {
                Toast.makeText(this, "Call failed to initialize", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }

    private fun initiateCall() {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiClient.getApi().initiateCall(
                        token = ApiClient.getAuthHeader(),
                        body = InitiateCallRequest(group_id = groupId)
                    )
                }

                if (isFinishing) return@launch
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        callId = body.id
                        createOffer()
                    } else {
                        Toast.makeText(this@CallActivity, "Failed to start call", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } else {
                    Toast.makeText(this@CallActivity, "Failed to start call", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to initiate call", e)
                if (!isFinishing) {
                    Toast.makeText(this@CallActivity, "Failed to start call", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))

        peerConnection?.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) return
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val offerData = JSONObject()
                        offerData.put("type", "offer")
                        offerData.put("sdp", sdp.description)
                        SocketClient.emitCallOffer(
                            toUserId = toUserId,
                            groupId = if (isGroup) groupId else null,
                            offer = offerData,
                            callId = callId ?: 0
                        )
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(tag, "Offer creation failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun setupSocketListeners() {
        SocketClient.onIncomingCall { fromUserId, fromUsername, offer, recvCallId, callType ->
            if (!isCaller) {
                runOnUiThread {
                    binding.tvStatus.text = "Incoming call from $fromUsername"
                    binding.tvCallerName.text = fromUsername
                }
                handleOffer(fromUserId, offer)
            }
        }

        SocketClient.onCallAnswered { fromUserId, answer, recvCallId ->
            runOnUiThread { binding.tvStatus.text = "Connected" }
            handleAnswer(answer)
        }

        SocketClient.onIceCandidate { fromUserId, candidate ->
            handleRemoteIceCandidate(candidate)
        }

        SocketClient.onCallRejected { fromUserId, recvCallId ->
            runOnUiThread {
                binding.tvStatus.text = "Call rejected"
                if (!isFinishing) {
                    Toast.makeText(this, "Call rejected", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }

        SocketClient.onCallEnded { fromUserId, recvCallId ->
            runOnUiThread {
                binding.tvStatus.text = "Call ended"
                finish()
            }
        }
    }

    private fun handleOffer(fromUserId: Int, offer: Any?) {
        try {
            val offerObj = offer as? JSONObject ?: JSONObject(offer.toString())
            val sdp = offerObj.getString("sdp")
            val sessionDesc = SessionDescription(SessionDescription.Type.OFFER, sdp)

            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    createAnswer(fromUserId)
                }
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(error: String?) {
                    Log.e(tag, "Set remote desc failed: $error")
                }
            }, sessionDesc)
        } catch (e: Exception) {
            Log.e(tag, "Handle offer failed", e)
        }
    }

    private fun createAnswer(toUserId: Int) {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))

        peerConnection?.createAnswer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp == null) return
                peerConnection?.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onSetSuccess() {
                        val answerData = JSONObject()
                        answerData.put("type", "answer")
                        answerData.put("sdp", sdp.description)
                        SocketClient.emitCallAnswer(toUserId, answerData, callId)
                    }
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                }, sdp)
            }
            override fun onSetSuccess() {}
            override fun onCreateFailure(error: String?) {
                Log.e(tag, "Answer creation failed: $error")
            }
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    private fun handleAnswer(answer: Any?) {
        try {
            val answerObj = answer as? JSONObject ?: JSONObject(answer.toString())
            val sdp = answerObj.getString("sdp")
            val sessionDesc = SessionDescription(SessionDescription.Type.ANSWER, sdp)

            peerConnection?.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() {
                    Log.d(tag, "Remote description set (answer)")
                }
                override fun onCreateSuccess(p0: SessionDescription?) {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(error: String?) {
                    Log.e(tag, "Set remote answer failed: $error")
                }
            }, sessionDesc)
        } catch (e: Exception) {
            Log.e(tag, "Handle answer failed", e)
        }
    }

    private fun handleRemoteIceCandidate(candidate: Any?) {
        try {
            val candidateObj = candidate as? JSONObject ?: JSONObject(candidate.toString())
            val iceCandidate = IceCandidate(
                candidateObj.optString("sdpMid"),
                candidateObj.optInt("sdpMLineIndex"),
                candidateObj.optString("sdp")
            )
            peerConnection?.addIceCandidate(iceCandidate)
        } catch (e: Exception) {
            Log.e(tag, "Add ICE candidate failed", e)
        }
    }

    private fun toggleMute() {
        isMuted = !isMuted
        localAudioTrack?.setEnabled(!isMuted)
        binding.btnMute.setColorFilter(
            ContextCompat.getColor(this, if (isMuted) R.color.call_red else R.color.text_secondary)
        )
    }

    private fun toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn
        binding.btnSpeaker.setColorFilter(
            ContextCompat.getColor(this, if (isSpeakerOn) R.color.accent_primary else R.color.text_secondary)
        )
    }

    private fun endCall() {
        SocketClient.emitCallEnd(toUserId, if (isGroup) groupId else null, callId)

        if (callId != null) {
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        ApiClient.getApi().endCall(
                            token = ApiClient.getAuthHeader(),
                            callId = callId!!
                        )
                    }
                } catch (e: Exception) {
                    Log.e(tag, "End call API failed", e)
                }
            }
        }

        cleanupWebRTC()
        finish()
    }

    private fun cleanupWebRTC() {
        try {
            localAudioTrack?.dispose()
            peerConnection?.close()
            peerConnectionFactory?.dispose()
        } catch (e: Exception) {
            Log.e(tag, "Cleanup failed", e)
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCall()
            } else {
                if (!isFinishing) {
                    Toast.makeText(this, "Microphone permission required for calls", Toast.LENGTH_SHORT).show()
                }
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupWebRTC()
        SocketClient.removeAllListeners()
    }
}
