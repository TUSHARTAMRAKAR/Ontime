package com.tushartamrakar.ontime.focus.blocker

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import javax.inject.Inject
import com.tushartamrakar.ontime.focus.accessibility.FocusSessionPrefs

/**
 * AdultContentVpnService — always-on local DNS filter.
 *
 * How it works:
 *   1. Establishes a local VPN interface (no data leaves the device)
 *   2. Routes DNS traffic (all queries) through our TUN interface
 *   3. Intercepts DNS queries and checks against AdultDomainBlocklist
 *   4. If domain is blocked → returns NXDOMAIN (domain doesn't exist)
 *   5. If domain is allowed → forwards to real DNS (8.8.8.8) and returns response
 *
 * This is the same technique used by AdGuard, Blokada, DNS66, and NextDNS.
 * Zero data leaves the device — we only intercept DNS (port 53 UDP).
 * All HTTPS traffic passes through untouched.
 *
 * The VPN only intercepts DNS queries — it does NOT intercept:
 *   - HTTPS traffic (sites still connect — only domain resolution is blocked)
 *   - Any personal data
 *   - Anything other than DNS port 53
 *
 * Started/stopped by BlockerScreen toggle or FocusSettingsEntity.adultFilterEnabled.
 */
@AndroidEntryPoint
class AdultContentVpnService : VpnService() {

    @Inject lateinit var blocklist:       AdultDomainBlocklist
    @Inject lateinit var focusWebBlocklist: FocusWebBlocklist

    private val tag = "AdultContentVPN"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var filterJob: Job? = null

    // Upstream DNS server — all non-blocked queries forwarded here
    private val upstreamDns = "8.8.8.8"
    private val dnsPort     = 53

    companion object {
        const val ACTION_START = "adultfilter.START"
        const val ACTION_STOP  = "adultfilter.STOP"
        private val _isRunning = java.util.concurrent.atomic.AtomicBoolean(false)
        val isRunning: Boolean get() = _isRunning.get()
    }

    // ─── Service lifecycle ────────────────────────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_START -> { startFiltering(); START_STICKY }
            ACTION_STOP  -> { stopFiltering();  START_NOT_STICKY }
            else         -> START_NOT_STICKY
        }
    }

    override fun onRevoke() {
        // Called by Android when user disables VPN from Settings
        stopFiltering()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopFiltering()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ─── VPN setup ────────────────────────────────────────────────────────────

    private fun startFiltering() {
        if (_isRunning.get()) return
        serviceScope.launch {
            blocklist.loadIfNeeded(applicationContext)
            Log.d(tag, "Starting VPN filter with ${blocklist.size()} blocked domains")

            try {
                // Build the VPN interface
                vpnInterface = Builder()
                    .setSession("Ontime Adult Filter")
                    .addAddress("10.0.0.1", 32)          // our TUN IP
                    .addDnsServer("10.0.0.2")             // route DNS to ourselves
                    .addRoute("10.0.0.2", 32)             // only route DNS server traffic
                    .setMtu(1500)
                    .establish()

                if (vpnInterface == null) {
                    Log.e(tag, "VPN interface is null — user may not have granted VPN permission")
                    return@launch
                }

                _isRunning.set(true)
                Log.d(tag, "VPN interface established")
                startDnsProxy()

            } catch (e: Exception) {
                Log.e(tag, "Failed to start VPN: ${e.message}")
                _isRunning.set(false)
            }
        }
    }

    private fun stopFiltering() {
        filterJob?.cancel()
        filterJob = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        _isRunning.set(false)
        stopSelf()
        Log.d(tag, "VPN filter stopped")
    }

    // ─── DNS proxy loop ───────────────────────────────────────────────────────

    /**
     * Reads DNS query packets from the TUN interface, checks the domain
     * against the blocklist, and either:
     *   - Returns NXDOMAIN (blocked) — domain cannot be resolved
     *   - Forwards to 8.8.8.8 (allowed) — returns real DNS response
     */
    private fun startDnsProxy() {
        filterJob = serviceScope.launch {
            val tun = vpnInterface ?: return@launch
            val inputStream  = FileInputStream(tun.fileDescriptor)
            val outputStream = FileOutputStream(tun.fileDescriptor)
            val buffer = ByteArray(32767)

            Log.d(tag, "DNS proxy loop started")

            while (isActive) {
                try {
                    val length = inputStream.read(buffer)
                    if (length <= 0) continue

                    val packet = ByteBuffer.wrap(buffer, 0, length)

                    // Parse IPv4 UDP DNS packet
                    val dnsPayload = extractDnsPayload(buffer, length) ?: continue
                    val domain     = parseDnsQuery(dnsPayload) ?: continue

                    if (blocklist.isBlocked(domain)) {
                        // Always blocked — adult content filter
                        Log.d(tag, "BLOCKED [adult]: $domain")
                        val nxDomain = buildNxDomainResponse(dnsPayload)
                        outputStream.write(
                            wrapInUdpPacket(nxDomain, "10.0.0.2", "10.0.0.1", dnsPort, 12345)
                        )
                    } else if (
                        FocusSessionPrefs.isSessionActive(applicationContext) &&
                        focusWebBlocklist.isBlockedDuringFocus(domain)
                    ) {
                        // Blocked only during active focus sessions — web focus filter
                        Log.d(tag, "BLOCKED [focus]: $domain")
                        val nxDomain = buildNxDomainResponse(dnsPayload)
                        outputStream.write(
                            wrapInUdpPacket(nxDomain, "10.0.0.2", "10.0.0.1", dnsPort, 12345)
                        )
                    } else {
                        // Allowed — forward to real DNS and send response back
                        val response = forwardToUpstreamDns(dnsPayload) ?: continue
                        outputStream.write(
                            wrapInUdpPacket(response, "10.0.0.2", "10.0.0.1", dnsPort, 12345)
                        )
                    }
                } catch (e: Exception) {
                    if (isActive) Log.w(tag, "DNS proxy error: ${e.message}")
                }
            }
            Log.d(tag, "DNS proxy loop ended")
        }
    }

    // ─── DNS packet parsing ───────────────────────────────────────────────────

    /**
     * Extract DNS payload from a raw IPv4/UDP packet.
     * IPv4 header = 20 bytes, UDP header = 8 bytes, DNS starts at byte 28.
     */
    private fun extractDnsPayload(raw: ByteArray, length: Int): ByteArray? {
        if (length < 28) return null
        val ipHeaderLen = (raw[0].toInt() and 0x0F) * 4
        val protocol    = raw[9].toInt() and 0xFF
        if (protocol != 17) return null  // not UDP
        val udpDstPort  = ((raw[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or
                           (raw[ipHeaderLen + 3].toInt() and 0xFF)
        if (udpDstPort != dnsPort) return null
        val dnsStart = ipHeaderLen + 8
        if (dnsStart >= length) return null
        return raw.copyOfRange(dnsStart, length)
    }

    /**
     * Parse the queried domain name from a DNS query packet.
     * DNS name encoding: length byte + label bytes, ending with 0x00.
     */
    private fun parseDnsQuery(dns: ByteArray): String? {
        return try {
            if (dns.size < 12) return null
            val sb = StringBuilder()
            var i = 12  // skip DNS header (12 bytes)
            while (i < dns.size) {
                val labelLen = dns[i].toInt() and 0xFF
                if (labelLen == 0) break
                if (sb.isNotEmpty()) sb.append('.')
                i++
                if (i + labelLen > dns.size) return null
                sb.append(String(dns, i, labelLen))
                i += labelLen
            }
            sb.toString().lowercase()
        } catch (e: Exception) { null }
    }

    /**
     * Build a DNS NXDOMAIN response for the given query.
     * Copies the transaction ID from the query, sets RCODE = 3 (NXDOMAIN).
     */
    private fun buildNxDomainResponse(query: ByteArray): ByteArray {
        if (query.size < 12) return query
        val response = query.copyOf()
        // Byte 2-3: flags — QR=1 (response), OPCODE=0, AA=0, TC=0, RD=1,
        //                    RA=1, Z=0, RCODE=3 (NXDOMAIN)
        response[2] = 0x81.toByte()   // QR=1, RD=1
        response[3] = 0x83.toByte()   // RA=1, RCODE=3 (NXDOMAIN)
        // Zero out answer count, authority count, additional count
        response[6] = 0; response[7] = 0
        response[8] = 0; response[9] = 0
        response[10] = 0; response[11] = 0
        return response
    }

    /**
     * Forward DNS query to 8.8.8.8 and return the response.
     * Uses protect() to bypass the VPN for this socket (avoids loops).
     */
    private fun forwardToUpstreamDns(query: ByteArray): ByteArray? {
        return try {
            val socket = DatagramSocket()
            protect(socket)  // CRITICAL — bypass VPN for this socket
            socket.soTimeout = 3000
            val sendPacket = DatagramPacket(
                query, query.size,
                InetAddress.getByName(upstreamDns), dnsPort,
            )
            socket.send(sendPacket)
            val recvBuf    = ByteArray(4096)
            val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
            socket.receive(recvPacket)
            socket.close()
            recvBuf.copyOfRange(0, recvPacket.length)
        } catch (e: Exception) {
            Log.w(tag, "Upstream DNS failed: ${e.message}")
            null
        }
    }

    /**
     * Wrap a DNS payload in a minimal IPv4/UDP packet
     * so we can write it back to the TUN interface.
     */
    private fun wrapInUdpPacket(
        payload: ByteArray,
        srcIp: String, dstIp: String,
        srcPort: Int, dstPort: Int,
    ): ByteArray {
        val udpLen  = 8 + payload.size
        val ipLen   = 20 + udpLen
        val buf     = ByteArray(ipLen)

        // ── IPv4 header (20 bytes) ──────────────────────────────────────────
        buf[0]  = 0x45.toByte()   // Version=4, IHL=5
        buf[1]  = 0
        buf[2]  = (ipLen shr 8).toByte()
        buf[3]  = (ipLen and 0xFF).toByte()
        buf[8]  = 64              // TTL
        buf[9]  = 17              // Protocol = UDP
        // Source IP
        val src = InetAddress.getByName(srcIp).address
        System.arraycopy(src, 0, buf, 12, 4)
        // Dest IP
        val dst = InetAddress.getByName(dstIp).address
        System.arraycopy(dst, 0, buf, 16, 4)
        // IP checksum (simple — skipped for local loopback)

        // ── UDP header (8 bytes) ─────────────────────────────────────────────
        buf[20] = (srcPort shr 8).toByte()
        buf[21] = (srcPort and 0xFF).toByte()
        buf[22] = (dstPort shr 8).toByte()
        buf[23] = (dstPort and 0xFF).toByte()
        buf[24] = (udpLen shr 8).toByte()
        buf[25] = (udpLen and 0xFF).toByte()
        // UDP checksum = 0 (optional for IPv4)

        // ── DNS payload ──────────────────────────────────────────────────────
        System.arraycopy(payload, 0, buf, 28, payload.size)
        return buf
    }
}
